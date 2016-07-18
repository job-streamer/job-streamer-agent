(ns job-streamer.agent.component.httpkit
  (:require [com.stuartsierra.component :as component]
            [org.httpkit.server :refer [run-server]]))


(defrecord HttpKitServer [app port]
  component/Lifecycle

  (start [component]
    (if (:server component)
      component
      (let [server (run-server (:handler app) {:port port})]
        (assoc component :server server))))

  (stop [component]
    (if-let [server (:server component)]
      (server))
    (dissoc component :server)))

(defn http-kit-server [options]
  (map->HttpKitServer options))
