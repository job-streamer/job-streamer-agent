(ns job-streamer.agent.batch-environment
  (:require [job-streamer.agent.core :as core])
  (:gen-class :name job_streamer.agent.BatchEnvironment
              :extends org.jberet.se.BatchSEEnvironment))

(defn -getClassLoader [this]
  @(core/ws-classloader))
