(ns job-streamer.agent.system
  (:require [com.stuartsierra.component :as component]
            [duct.component.endpoint :refer [endpoint-component]]
            [duct.component.handler :refer [handler-component]]
            [duct.middleware.not-found :refer [wrap-not-found]]
            [meta-merge.core :refer [meta-merge]]
            [ring.middleware.defaults :refer [wrap-defaults api-defaults]]
            [ring.util.response :refer [header]]
            (job-streamer.agent.component
             [httpkit   :refer [http-kit-server]]
             [connector :refer [connector-component]]
             [beacon    :refer [beacon-component]]
             [runtime   :refer [runtime-component]]
             [spec      :refer [spec-component]])
            (job-streamer.agent.endpoint
             [api :refer [api-endpoint]])))

(def base-config
  {:app {:middleware [[wrap-not-found :not-found]
                      [wrap-defaults :defaults]]
         :not-found  "Resource Not Found"
         :defaults  (meta-merge api-defaults {})}})


(defn new-system [config]
  (let [config (meta-merge base-config config)]
    (-> (component/system-map
         :app        (handler-component    (:app config))
         :api        (endpoint-component   api-endpoint)
         :http       (http-kit-server      (:http config))
         :beacon     (beacon-component     (:beacon config))
         :runtime    (runtime-component    (:runtime config))
         :spec       (spec-component       (:spec config))
         :connector  (connector-component  (:connector config)))
        (component/system-using
         {:http      [:app]
          :app       [:api]
          :api       [:connector :runtime :spec]
          :connector [:beacon :spec :runtime]
          :spec      [:runtime]}))))
