(ns job-streamer.agent.runtime
  (:require [clojure.tools.logging :as log])
  (:import [javax.batch.runtime BatchRuntime]
           [net.unit8.wscl WebSocketClassLoader])
  (:gen-class))

(defn tracer-bullet-fn [])

(defonce classloaders (atom {}))
(defonce base-url (atom nil))
(defonce job-operator (BatchRuntime/getJobOperator))

(defmacro with-classloader [loader & body]
  `(let [original-loader# (.getContextClassLoader (Thread/currentThread))]
     (try
       (.setContextClassLoader (Thread/currentThread) ~loader)
       ~@body
       (finally
         (.setContextClassLoader (Thread/currentThread) original-loader#)))))

(defn find-loader [class-loader-id]
  (log/info "find-loader " class-loader-id)
  (if-let [wscl (get @classloaders (or class-loader-id :default))]
    wscl
    (let [wscl (WebSocketClassLoader.
                (str @base-url (when class-loader-id (str "?classLoaderId=" (.toString class-loader-id))))
                (.getClassLoader (class tracer-bullet-fn)))]
      (log/info "ClassLoader URL=" (str @base-url (when class-loader-id (str "?classLoaderId=" (.toString class-loader-id))))) 
      (swap! classloaders assoc (or class-loader-id :default) wscl)
      wscl)))

(defn set-base-url! [url]
  (reset! base-url url))
