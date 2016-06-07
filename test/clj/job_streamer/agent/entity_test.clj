(ns entity-test
  (:require [clojure.edn :as edn]
            [job-streamer.agent.entity :only [make-job to-xml]])
  (:use [clojure.test]))

(deftest make-job-test
  (let [job (make-job (edn/read-string "{:job/name j, :job/components [{:step/name a, :step/properties nil, :step/transitions [{:next/on c, :next/to [{:step/name b, :step/properties nil, :step/batchlet {:batchlet/ref example.HelloBatch}}]}], :step/batchlet {:batchlet/ref example.HelloBatch}}], :job/properties nil}"))]
    ;success
    (testing "一つ目のcomponentは一番初めに実行されるstepである"
      (is (= (read-string "a") (:id (first (:components job))))))
    ;fail
    (testing "二つ目のcomponentは二番目に実行されるstepである"
      (is (= (read-string "b") (:id (second (:components job))))))
    ;fail
    (testing "一つ目のstepの中のnext/toは二番目に実行されるstepのidである"
      (is (= (read-string "b") (:next/to (first (:transitions (first (:components job))))))))))
