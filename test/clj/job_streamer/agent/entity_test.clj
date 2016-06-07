(ns job-streamer.agent.entity-test
  (:require [clojure.edn :as edn]
            [job-streamer.agent.entity :refer [make-job to-xml]])
  (:use [clojure.test]))

(def job1
  {:job/name "j",
   :job/components
   [{:step/name "a",
     :step/properties nil,
     :step/transitions
     [{:next/on "c",
       :next/to
       [{:step/name "b",
         :step/properties nil,
         :step/batchlet {:batchlet/ref "example.Batchlet1"}}]}],
     :step/batchlet {:batchlet/ref "example.Batchlet2"}}], :job/properties nil})

(deftest make-job-test
  (let [job (make-job job1)]
    ;success
    (testing "First component is a first step"
      (is (= "a" (:id (first (:components job))))))
    ;fail
    (testing "Second component is a second step"
      (is (= "b" (:id (second (:components job))))))
    ;fail
    (testing "The 'next/to' of the first step is the identity of the second component."
      (is (= "b" (:next/to (first (:transitions (first (:components job))))))))))
