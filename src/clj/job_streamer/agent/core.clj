(ns job-streamer.agent.core
  (:require [clojure.java.io :as io]
            [clojure.edn :as edn]
            [clojure.data.xml :as xml]
            [clojure.tools.logging :as log]
            [liberator.core :refer [defresource]]
            [compojure.core :refer [defroutes ANY POST]]
            [ring.middleware.defaults :refer [wrap-defaults api-defaults]]
            [liberator.dev :refer [wrap-trace]]
            [job-streamer.agent.entity :refer [make-job to-xml]]
            [job-streamer.agent.connector :as connector])
  (:use [clojure.core.async :only [go-loop <! put! timeout chan]]
        [org.httpkit.server :only [run-server]]
        [environ.core :only [env]]
        [ring.middleware.reload :only [wrap-reload]])
  (:import [javax.batch.runtime BatchRuntime]
           [java.util Properties UUID]
           [java.io File]
           [java.io ByteArrayOutputStream DataOutputStream File]
           [java.net InetSocketAddress InetAddress URI]
           [java.nio ByteBuffer]
           [java.nio.file Files]
           [java.nio.channels DatagramChannel]
           [org.slf4j LoggerFactory]
           [net.unit8.wscl WebSocketClassLoader]
           [net.unit8.logback WebSocketAppender]
           [net.unit8.job_streamer.agent StringMessageHandler]
           [javax.websocket ContainerProvider Endpoint MessageHandler$Whole]))

(defonce job-operator (BatchRuntime/getJobOperator))
(defonce agent-port (atom nil))
(defonce ws-classloader (atom (.getContextClassLoader (Thread/currentThread))))
(defonce websocket-container (ContainerProvider/getWebSocketContainer))
(defonce instance-id (UUID/randomUUID))
(def ws-channel (chan))
(def join-request-channel (chan))


(defn tracer-bullet-fn [])

(defmacro with-classloader [loader & body]
  `(let [original-loader# (.getContextClassLoader (Thread/currentThread))]
     (try
       (.setContextClassLoader (Thread/currentThread) ~loader)
       ~@body
       (finally
         (.setContextClassLoader (Thread/currentThread) original-loader#)))))

(defn body-as-string [ctx]
  (if-let [body (get-in ctx [:request :body])]
    (condp instance? body
      java.lang.String body
      (slurp (io/reader body)))))

(defn parse-edn [context key]
  (when (#{:put :post} (get-in context [:request :request-method]))
    (try
      (if-let [body (body-as-string context)]
        (let [data (edn/read-string body)]
          [false {key data}])
        {:message "No doby"})
      (catch Exception e
        (.printStackTrace e)
        {:message (format "IOException: %s" (.getMessage e))}))))

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

(defresource jobs-resource
  :available-media-types ["application/edn"]
  :allowed-methods [:get :post]
  :malformed? #(parse-edn % ::data)
  :post! (fn [ctx]
           (let [job-file (File/createTempFile "job" ".xml")
                 job (-> (make-job (get-in ctx [::data :job]))
                         (assoc-in [:properties :request-id] (get-in ctx [::data :request-id])))
                 parameters (Properties.)]
             (spit job-file (xml/emit-str (to-xml job)))
             (doseq [[k v] (get-in ctx [::data :parameters])]
               (.setProperty (str k) (str v)))
             (let [execution-id (with-classloader @ws-classloader
                                  (.start job-operator
                                          (.getAbsolutePath job-file)
                                          parameters)) 
                   execution (with-classloader @ws-classloader
                               (.getJobExecution job-operator execution-id))]
               {:execution-id execution-id})))
  :post-redirect? false
  :handle-created (fn [ctx]
                    (select-keys ctx [:execution-id]))
  :handle-ok (fn [ctx]
               (vec (. job-operator getJobNames))))

(defn- agent-spec []
  (let [mx (java.lang.management.ManagementFactory/getOperatingSystemMXBean)]
    (merge
     {:os-name (.getName mx)
      :os-version (.getVersion mx)
      :cpu-arch (.getArch mx)
      :cpu-core (.getAvailableProcessors mx)}
     (try
      (when (instance? (Class/forName "com.sun.management.OperatingSystemMXBean") mx)
        {:memory
         {:physical {:free  (.getFreePhysicalMemorySize mx)
                     :total (.getTotalPhysicalMemorySize mx)}
          :swap     {:free  (.getFreeSwapSpaceSize mx)
                     :total (.getTotalSwapSpaceSize mx)}}
         :cpu
         {:process {:load (.getProcessCpuLoad mx)
                    :time (.getProcessCpuTime mx)}
          :system  {:load (.getSystemCpuLoad mx)
                    :load-average (.getSystemLoadAverage mx)}}})
      (catch ClassNotFoundException e)))))

(defmulti handle-command (fn [msg client] (:command msg)))

(defmethod handle-command :class-provider-port
  ([msg client]
     (log/debug "handle :class-provider-port" msg)
     
     (.sendMessage client (pr-str ))))

(defmethod handle-command :default
  ([msg client]
     (throw (UnsupportedOperationException. (str "Unknown message: " msg)))))

(defresource job-instances-resource [job-id])

(defresource job-executions-resource [job-name]
  :available-media-types ["application/edn"]
  :handle-ok (fn [ctx]
               (if-let [job-instance (some-> job-operator
                                             (.getJobInstances job-name 0 1)
                                             first)]
                 (map bean (. job-operator getJobExecutions job-instance)))))

(defresource job-execution-resource [execution-id]
  :available-media-types ["application/edn"]
  :exists? (fn [_]
             (when-let [execution (.getJobExecution job-operator execution-id)]
               {:execution execution
                :step-executions (.getStepExecutions job-operator (.getExecutionId execution))}))
  :handle-ok (fn [{execution :execution step-executions :step-executions}]
               {:execution-id (.getExecutionId execution)
                :start-time (.getStartTime execution)
                :end-time   (.getEndTime execution)
                :batch-status (keyword "batch-status"
                                       (.. execution getBatchStatus name toLowerCase))
                :exit-status (.getExitStatus execution)
                :step-executions (->> step-executions
                                      (map (fn [se]
                                           {:start-time (.getStartTime se)
                                            :end-time (.getEndTime se)
                                            :step-execution-id (.getStepExecutionId se)
                                            :batch-status (keyword "batch-status"
                                                                   (.. se getBatchStatus name toLowerCase))}))
                                      (vec))}))

(defresource step-execution-resource [execution-id step-execution-id]
  :available-media-types ["application/edn"]
  :exists? (fn [_]
             (when-let [execution (.getJobExecution job-operator execution-id)]
               (when-let [step-execution (->> (.getStepExecutions job-operator (.getExecutionId execution))
                                              (filter #(= (.getStepExecutionId %) step-execution-id))
                                              first)]
                 {:step-execution step-execution})))
  :handle-ok (fn [{step-execution :step-execution}]
               {:step-execution-id (.getStepExecutionId step-execution)
                  :start-time (.getStartTime step-execution)
                  :end-time   (.getEndTime step-execution)
                  :batch-status (keyword "batch-status"
                                         (.. step-execution getBatchStatus name toLowerCase))
                  :exit-status (.getExitStatus step-execution)
                  :step-name   (.getStepName step-execution)}))

(defn agent-endpoint [connecting?]
  (proxy [Endpoint] []
    (onOpen [session config]
      (connector/stop)
      (.addMessageHandler
       session
       (proxy [StringMessageHandler] []
         (onMessage [msg]
           (handle-command (clojure.edn/read-string msg) session))))
      (.. session
          getAsyncRemote
          (sendText (pr-str
                     (merge {:command :ready
                             :instance-id instance-id
                             :name (.getHostName (InetAddress/getLocalHost))
                             :port @agent-port}
                            (agent-spec)))
                    (reify javax.websocket.SendHandler
                      (onResult [_ result]
                        (when-not (.isOK result)
                          (log/error (.getException result))))))))
    (onClose [session close-reason]
      (log/info "Closed by control bus. restart multicast.")
      (reset! connecting? false)
      (connector/restart))))

(defn connect-to-bus [url-str connecting?]
  (let [uri (URI/create url-str)
        endpoint (agent-endpoint connecting?)
        session (.connectToServer
                 websocket-container
                 ^Endpoint endpoint nil uri)]
    (try
      (reset! ws-classloader
              (WebSocketClassLoader.
               (str "ws://" (.getHost uri) ":" (.getPort uri) "/wscl")
               (.getClassLoader (class tracer-bullet-fn))))
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
      (let [url (<! join-request-channel)]
        (log/info "Join request proccessing... connecting? " @connecting?)
        (when-not @connecting?
          (reset! connecting? true)
          (connect-to-bus url connecting?))
        (recur)))))

(defroutes app-routes
  (POST "/join-bus" {{url :control-bus-url} :params}
    (put! join-request-channel url)
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
    (job-execution-resource (Long/parseLong execution-id))))

(def app
  (-> app-routes
      wrap-reload
      (wrap-defaults api-defaults)))

(defn -main [& {:keys [port] :or {port 4510}}]
  (reset! agent-port port)
  (join-bus-routine)
  (connector/start port)
  (run-server app {:port @agent-port}))
