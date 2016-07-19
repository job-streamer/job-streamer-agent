(ns job-streamer.agent.main
  (:gen-class)
  (:require [com.stuartsierra.component :as component]
            [duct.middleware.errors :refer [wrap-hide-errors]]
            [duct.util.runtime :refer [add-shutdown-hook]]
            [meta-merge.core :refer [meta-merge]]
            (job-streamer.agent [config :as config]
                                [system :refer [new-system]])))

(def prod-config
  {:app {:middleware     [[wrap-hide-errors :internal-error]]
         :internal-error "Intenal Server Error"}})

(def config
  (meta-merge config/defaults
              config/environ
              prod-config))

(def banner "
   ___       _     _____ _
  |_  |     | |   /  ___| |                         Agent
    | | ___ | |__ \\ `--.| |_ _ __ ___  __ _ _ __ ___   ___ _ __
    | |/ _ \\| '_ \\ `--. \\ __| '__/ _ \\/ _` | '_ ` _ \\ /_ \\ '__|
/\\__/ / (_) | |_) /\\__/ / |_| | |  __/(_| | | | | | |  __/ |
\\____/ \\___/|_.__/\\____/ \\__|_|  \\___|\\__,_|_| |_| |_|\\___|_|
  ")

(def system
  (atom nil))

(defn -main [& args]
  (reset! system (new-system config))
  (println banner)
  (add-shutdown-hook ::stop-system #(component/stop @system))
  (swap! system component/start))

