(ns job-streamer.agent.component.connector
  (:require [clojure.tools.logging :as log]
            [clojure.core.async :refer [chan put! go-loop <! timeout close!]]
            [clojure.edn :as edn]
            [com.stuartsierra.component :as component]
            [environ.core :refer [env]]
            (job-streamer.agent.component [beacon :as beacon]
                                          [spec :as spec]))
  (:import [java.io ByteArrayOutputStream DataOutputStream]
           [java.net InetAddress InetSocketAddress URI]
           [java.nio ByteBuffer]
           [java.nio.channels DatagramChannel]
           [org.slf4j LoggerFactory]
           [net.unit8.logback WebSocketAppender]
           [net.unit8.job_streamer.agent StringMessageHandler]
           [javax.websocket ContainerProvider Endpoint MessageHandler$Whole]))

(defn join-request [{:keys [agent-port]}]
  (let [baos (ByteArrayOutputStream.)
        dos  (DataOutputStream. baos)
        ch   (DatagramChannel/open)]
    (try 
      (doto dos
        (.write (.getAddress (InetAddress/getLocalHost)) 0 4)
        (.writeInt (int agent-port)))
      (.. ch socket (setBroadcast true))
      (.send ch
             (ByteBuffer/wrap (.toByteArray baos))
             (InetSocketAddress. "255.255.255.255" (or (env :control-bus-port) 4510)))
      (finally (.close ch)))))

(defmulti handle-command (fn [msg client] (:command msg)))

(defmethod handle-command :class-provider-port
  ([msg client]
     (log/debug "handle :class-provider-port" msg)
     (.sendMessage client (pr-str ))))

(defmethod handle-command :default
  ([msg client]
     (throw (UnsupportedOperationException. (str "Unknown message: " msg)))))

(defn agent-endpoint [{:keys [beacon runtime spec]} host port connecting?]
  (proxy [Endpoint] []
    (onOpen [session config]
      (component/stop beacon)
      (.addMessageHandler
       session
       (proxy [StringMessageHandler] []
         (onMessage [msg]
           (handle-command (edn/read-string msg) session))))
      (log/info "endpoint! runtime=" runtime)
      (.. session
          getAsyncRemote
          (sendText (pr-str
                     (merge {:command :ready
                             :agent/instance-id (:instance-id runtime)
                             :agent/name (.getHostName (InetAddress/getLocalHost))
                             :agent/port port
                             :agent/host host}
                            (spec/agent-spec spec)))
                    (reify javax.websocket.SendHandler
                      (onResult [_ result]
                        (when-not (.isOK result)
                          (log/error (.getException result))))))))
    (onClose [session close-reason]
      (log/info "Closed by control bus. restart multicast.")
      (reset! connecting? false)
      (component/start beacon))))

(defn connect-to-bus [{:keys [runtime ws-handler ws-channel
                              ws-container ws-session] :as connector}
                      url-str my-host port connecting?]
  (log/info "Connect to control bus " url-str)
  (let [uri (URI/create url-str)
        endpoint (agent-endpoint connector my-host port connecting?)
        session (.connectToServer
                 ws-container
                 ^Endpoint endpoint nil uri)]
    (reset! ws-session session)
    (try
      (reset! (:base-url runtime)
              (str "ws://" (.getHost uri) ":" (.getPort uri) "/wscl"))
      (catch Exception e
        (log/error e)
        (reset! connecting? false)))

    ;; Set up logger
    (let [ws-appender (WebSocketAppender.)]
      (doto ws-appender
        (.setContext (LoggerFactory/getILoggerFactory))
        (.setServerUri (str "ws://" (.getHost uri) ":" (.getPort uri) "/wslog"))
        (.start))
      (.. (LoggerFactory/getLogger "root") (addAppender ws-appender)))
    
    (reset!
     ws-handler
     (go-loop []
       (when-let [msg (<! ws-channel)]
         (if (= (:command msg) :close)
           (do
             (.close session)
             (reset! connecting? false))
           (when (.isOpen session)
             (log/debug "send message to control-hub: " msg)
             (.. session
                   getAsyncRemote
                   (sendText (pr-str msg)))
             (recur))))))))

(defn join-bus-routine [{:keys [join-request-channel port] :as connector}]
  (let [connecting? (atom false)]
    (go-loop []
      (when-let [{url :control-bus-url my-host :agent-host} (<! join-request-channel)]
        (when-not @connecting?
          (reset! connecting? true)
          (try
            (connect-to-bus connector url my-host port connecting?)
            (catch Exception e
              (log/error e "Can't connect to bus."))))
        (recur)))))

(defn send-message [{:keys [ws-channel]} msg]
  (put! ws-channel msg))

(defrecord Connector [port]
  component/Lifecycle

  (start [component]
    (let [join-request-channel (chan)
          component (assoc component
                           :join-request-channel join-request-channel
                           :ws-container (ContainerProvider/getWebSocketContainer)
                           :ws-channel   (chan)
                           :ws-handler   (atom nil)
                           :ws-session   (atom nil))
          connector-loop (join-bus-routine component)]
      (assoc component
             :connector-loop connector-loop)))

  (stop [component]
    (when-let [join-request-channel (:join-request-channel component)]
      (close! join-request-channel))
    (when-let [connector-loop (:connector-loop component)]
      (close! connector-loop))
    (when-let [ws-session (:ws-session component)]
      (.close @ws-session))
    (when-let [ws-channel (:ws-channel component)]
      (close! ws-channel))
    (when-let [ws-handler (:ws-handler component)]
      (close! @ws-handler))
    (dissoc component :connector-loop :ws-hannel :ws-handler :ws-container :ws-session)))

(defn connector-component [options]
  (map->Connector options))
