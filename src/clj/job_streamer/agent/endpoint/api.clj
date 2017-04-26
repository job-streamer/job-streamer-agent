(ns job-streamer.agent.endpoint.api
  (:require [clojure.tools.logging :as log]
            [clojure.core.async :refer [put!]]
            [compojure.core :refer [ANY POST GET routes]]
            (job-streamer.agent.component
             [spec :as spec]
             [runtime :as runtime]
             [connector :as connector])))

(defn api-endpoint [{:keys [spec runtime connector]}]
  (routes
   (POST "/join-bus" {params :params}
     (put! (:join-request-channel connector) params)
     "Accept")

   (ANY "/jobs" []
     (runtime/jobs-resource runtime))
   (ANY ["/job-instance/:job-id" :job-id #".*"] [job-name]
     (runtime/job-instances-resource runtime job-name))
   (ANY ["/job/:job-id/executions" :job-id #".*"] [job-id]
     (runtime/job-executions-resource runtime job-id))
   (ANY ["/job-execution/:execution-id/step-execution/:step-execution-id"
         :execution-id #"\d+" :step-execution-id #"\d+"]
       [execution-id step-execution-id]
     (runtime/step-execution-resource runtime
                                      (Long/parseLong execution-id)
                                      (Long/parseLong step-execution-id)))
   (ANY ["/job-execution/:execution-id/:cmd" :execution-id #"\d+" :cmd #"[\w\-]+"]
       [execution-id cmd]
     (runtime/job-execution-resource runtime
                                     (Long/parseLong execution-id)
                                     (keyword cmd)))
   (ANY ["/job-execution/:execution-id" :execution-id #"\d+"] [execution-id]
     (runtime/job-execution-resource runtime
                                     (Long/parseLong execution-id)))
   (ANY "/spec" [] (spec/spec-resource spec))

   (GET "/healthcheck" [] (do {:status 200}))))
