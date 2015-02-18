(ns job-streamer.agent.runtime
  (:require [clojure.tools.logging :as log])
  (:import [javax.batch.runtime BatchRuntime]))

(defonce ws-classloader (atom (.getContextClassLoader (Thread/currentThread))))
(defonce job-operator (BatchRuntime/getJobOperator))

(defmacro with-classloader [loader & body]
  `(let [original-loader# (.getContextClassLoader (Thread/currentThread))]
     (try
       (.setContextClassLoader (Thread/currentThread) ~loader)
       ~@body
       (finally
         (.setContextClassLoader (Thread/currentThread) original-loader#)))))

(defn set-ws-classloader! [wscl]
  (log/debug "set-ws-classloader!" wscl)
  (reset! ws-classloader wscl))
