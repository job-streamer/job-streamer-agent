(ns job-streamer.agent.component.spec
  (:require [com.stuartsierra.component :as component]
            [liberator.core :as liberator])
  (:import [java.lang.management ManagementFactory]))

(defn running-executions [job-operator]
  (->> (.getJobNames job-operator)
       (map #(count (.getRunningExecutions job-operator %)))
       (reduce +)))

(defn agent-spec [{:keys [runtime]}]
  (let [mx (ManagementFactory/getOperatingSystemMXBean)]
    (merge
     {:agent/os-name (.getName mx)
      :agent/os-version (.getVersion mx)
      :agent/cpu-arch (.getArch mx)
      :agent/cpu-core (.getAvailableProcessors mx)
      :agent/jobs {:running (running-executions (:job-operator runtime))}
      :agent/agent-version (slurp "VERSION")}
     (try
       (when (instance? (Class/forName "com.sun.management.OperatingSystemMXBean") mx)
         {:agent/stats
          {:memory
           {:physical {:free  (.getFreePhysicalMemorySize mx)
                       :total (.getTotalPhysicalMemorySize mx)}
            :swap     {:free  (.getFreeSwapSpaceSize mx)
                       :total (.getTotalSwapSpaceSize mx)}}
           :cpu
           {:process {:load (.getProcessCpuLoad mx)
                      :time (.getProcessCpuTime mx)}
            :system  {:load (.getSystemCpuLoad mx)
                      :load-average (.getSystemLoadAverage mx)}}}})
       (catch ClassNotFoundException e)))))

(defn spec-resource [spec]
  (liberator/resource
   :available-media-types ["application/edn"]
   :handle-ok (fn [ctx]
               (agent-spec spec))))


(defrecord AgentSpec []
  component/Lifecycle

  (start [component]
    component)

  (stop [component]
    component))

(defn spec-component [options]
  (map->AgentSpec options))
