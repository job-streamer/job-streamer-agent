(ns job-streamer.agent.core
  (:require [clojure.edn :as edn]
            [clojure.tools.logging :as log]

            [compojure.core :refer [defroutes ANY POST]]
            [ring.middleware.defaults :refer [wrap-defaults api-defaults]]
            [liberator.dev :refer [wrap-trace]]
            (job-streamer.agent [connector :as connector]
                                [runtime :as runtime]))
  (:use [clojure.core.async :only [go-loop <! put! timeout chan]]
        [org.httpkit.server :only [run-server]]
        [environ.core :only [env]]
        [ring.middleware.reload :only [wrap-reload]]
        [job-streamer.agent.spec :only [agent-spec]]
        [job-streamer.agent.resources :only [jobs-resource job-instances-resource
                                             job-executions-resource job-execution-resource
                                             step-execution-resource spec-resource]])
  (:import [java.util UUID]
           [java.io ByteArrayOutputStream DataOutputStream File]
           [java.net InetSocketAddress InetAddress URI]
           [java.nio ByteBuffer]
           [java.nio.channels DatagramChannel]
           [org.slf4j LoggerFactory]
           [net.unit8.logback WebSocketAppender]
           [net.unit8.job_streamer.agent StringMessageHandler]
           [javax.websocket ContainerProvider Endpoint MessageHandler$Whole]))

(defonce agent-port (atom nil))
(defonce websocket-container (ContainerProvider/getWebSocketContainer))
(defonce instance-id (UUID/randomUUID))
(def ws-channel (chan))
(def join-request-channel (chan))

(defn join-request []
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

(defn agent-endpoint [host connecting?]
  (proxy [Endpoint] []
    (onOpen [session config]
      (connector/stop)
      (.addMessageHandler
       session
       (proxy [StringMessageHandler] []
         (onMessage [msg]
           (handle-command (edn/read-string msg) session))))
      (.. session
          getAsyncRemote
          (sendText (pr-str
                     (merge {:command :ready
                             :agent/instance-id instance-id
                             :agent/name (.getHostName (InetAddress/getLocalHost))
                             :agent/port @agent-port
                             :agent/host host}
                            (agent-spec)))
                    (reify javax.websocket.SendHandler
                      (onResult [_ result]
                        (when-not (.isOK result)
                          (log/error (.getException result))))))))
    (onClose [session close-reason]
      (log/info "Closed by control bus. restart multicast.")
      (reset! connecting? false)
      (connector/restart))))

(defn connect-to-bus [url-str my-host connecting?]
  (let [uri (URI/create url-str)
        endpoint (agent-endpoint my-host connecting?)
        session (.connectToServer
                 websocket-container
                 ^Endpoint endpoint nil uri)]
    (try
      (runtime/set-base-url! (str "ws://" (.getHost uri) ":" (.getPort uri) "/wscl"))
      (catch Exception e
        (log/error e)
        (reset! connecting? false)))

    ;; Set up logger
    (let [ws-appender (WebSocketAppender.)]
      (doto ws-appender
        (.setContext (LoggerFactory/getILoggerFactory))
        (.setServerUri (str "ws://" (.getHost uri) ":" (.getPort uri) "/wslog"))
        (.start))
      (.. (LoggerFactory/getLogger "job-streamer") (addAppender ws-appender)))
    

    (go-loop []
      (let [msg (<! ws-channel)]
        (if (= (:command msg) :close)
          (do
            (.close session)
            (reset! connecting? false))
          (if (.isOpen session)
            (do
              (log/debug "send message to control-hub: " msg)
              (.. session
                  getAsyncRemote
                  (sendText (pr-str msg)))
              (recur))
            (put! ws-channel msg)))))))

(defn join-bus-routine []
  (let [connecting? (atom false)]
    (go-loop []
      (let [{url :control-bus-url my-host :agent-host} (<! join-request-channel)]
        (log/debug "Join request proccessing... connecting? " @connecting?)
        (when-not @connecting?
          (reset! connecting? true)
          (try
            (connect-to-bus url my-host connecting?)
            (catch Exception e
              (log/error e "Can't connect to bus."))))
        (recur)))))

(defroutes app-routes
  (POST "/join-bus" {params :params}
    (put! join-request-channel params)
    "Accept")

  (ANY "/jobs" [] jobs-resource)
  (ANY ["/job-instance/:job-id" :job-id #".*"] [job-name]
    (job-instances-resource job-name))
  (ANY ["/job/:job-id/executions" :job-id #".*"] [job-id]
    (job-executions-resource job-id))
  (ANY ["/job-execution/:execution-id/step-execution/:step-execution-id" :execution-id #"\d+" :step-execution-id #"\d+"]
      [execution-id step-execution-id]
    (step-execution-resource (Long/parseLong execution-id) (Long/parseLong step-execution-id)))
  (ANY ["/job-execution/:execution-id" :execution-id #"\d+"] [execution-id]
    (job-execution-resource (Long/parseLong execution-id)))
  (ANY "/spec" [] spec-resource))

(def app
  (-> app-routes
      wrap-reload
      (wrap-defaults api-defaults)))

(defn -main [& {:keys [port] :or {port 4510}}]
  (reset! agent-port port)
  (join-bus-routine)
  (connector/start port)
  (run-server app {:port @agent-port}))
