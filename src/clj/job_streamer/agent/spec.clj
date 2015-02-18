(ns job-streamer.agent.spec
  (:use [job-streamer.agent.runtime :only [job-operator]]))

(defn running-executions []
  (->> (.getJobNames job-operator)
       (map #(count (.getRunningExecutions job-operator %)))
       (reduce +)))

(defn agent-spec []
  (let [mx (java.lang.management.ManagementFactory/getOperatingSystemMXBean)]
    (merge
     {:os-name (.getName mx)
      :os-version (.getVersion mx)
      :cpu-arch (.getArch mx)
      :cpu-core (.getAvailableProcessors mx)
      :jobs {:running (running-executions)}}
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
