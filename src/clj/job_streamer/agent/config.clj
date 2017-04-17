(ns job-streamer.agent.config
  (:require [environ.core :refer [env]])
  (:import  [java.util UUID]))

(def defaults
  {:http {:port 4510}
   :connector {:port 4510}
   :beacon    {:port 4510}})

(def environ
  (let [port (some-> env :agent-port Integer.)]
    {:http {:port port}
     :connector {:port port}
     :beacon    {:port port}
     :runtime {:instance-id (some-> (env :instance-name)
                                    (.getBytes)
                                    (UUID/nameUUIDFromBytes))
               :job-xml-dir (env :job-xml-dir)}}))
