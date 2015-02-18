(ns job-streamer.agent.batch-environment
  (:require [job-streamer.agent.runtime :as runtime])
  (:gen-class :name job_streamer.agent.BatchEnvironment
              :extends org.jberet.se.BatchSEEnvironment))

(defn -getClassLoader [this]
  @(runtime/ws-classloader))
