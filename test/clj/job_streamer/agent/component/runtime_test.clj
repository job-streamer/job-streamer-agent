(ns job-streamer.agent.component.runtime-test
  (:require [job-streamer.agent.component.runtime :refer :all]
            [com.stuartsierra.component :as component]
            [clojure.pprint :refer :all]
            [clojure.test :refer :all]))

; This test requires control-bus.
(deftest runtime-test
  (testing "Multiple request"
    (let [runtime (component/start (runtime-component {:job-xml-dir "." :instance-id 12345}))
          _ (reset! (:base-url runtime)
              (str "ws://localhost:45102/wscl"))
          loaders (->> (take 50 (range))
                        (pmap (fn [_] (find-loader runtime nil))))]
      (println (str "loader : ") (first loaders))
      (is (every? #(= (first loaders) %) loaders)))))
