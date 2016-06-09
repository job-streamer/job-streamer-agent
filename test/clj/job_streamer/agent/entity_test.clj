(ns job-streamer.agent.entity-test
  (:require [clojure.edn :as edn]
            [clojure.test :refer [deftest testing is]]
            [job-streamer.agent.entity :refer [make-job to-xml]]))

(def job1
  {:job/name "job1",
   :job/components
   [{:step/name "step1",
     :step/properties nil,
     :step/transitions
     [{:next/on "status1",
       :next/to
       [{:step/name "step2",
         :step/properties nil,
         :step/batchlet {:batchlet/ref "example.Batchlet2"}}]}],
     :step/batchlet {:batchlet/ref "example.Batchlet1"}}], :job/properties nil})

(def job2
  {:job/name "job2",
   :job/components
   [{:step/name "step1",
     :step/properties nil,
     :step/transitions
     [{:next/on "status1",
       :next/to
       [{:step/name "step2",
         :step/properties nil,
         :step/batchlet {:batchlet/ref "example.Batchlet2"}}]}
      {:next/on "status2",
       :next/to
       [{:step/name "step3",
         :step/properties nil,
         :step/batchlet {:batchlet/ref "example.Batchlet3"}}]}],
     :step/batchlet {:batchlet/ref "example.Batchlet1"}}], :job/properties nil})
(def job3
  {:job/name "job3",
   :job/components
   [{:step/name "step1",
     :step/properties nil,
     :step/transitions
     [{:next/on "status1",
       :next/to
       [{:step/name "step2",
         :step/properties nil,
         :step/transitions
         [{:next/on "status2",
           :next/to
           [{:step/name "step3",
             :step/properties nil,
             :step/batchlet {:batchlet/ref "example.Batchlet3"}}]}],
         :step/batchlet {:batchlet/ref "example.Batchlet2"}}]}],
     :step/batchlet {:batchlet/ref "example.Batchlet1"}}], :job/properties nil})


(deftest make-job-test
  (testing "The job contains only one next phrase"
    (let [job (make-job job1)]
      (testing "First component is a first step"
        (is (= "step1" (:id (first (:components job))))))
      (testing "Second component is a second step"
        (is (= "step2" (:id (second (:components job))))))
      (testing "The 'next/to' of the first step is the identity of the second component."
        (is (= "step2" (:next/to (first (:transitions (first (:components job))))))))
      (testing "The 'next/on' of the first step is as same as argument job."
        (is (= "status1" (:next/on (first (:transitions (first (:components job))))))))))
  (testing "The job contains more than one next phrase"
    (let [job (make-job job2)]
      (testing "First component is a first step"
        (is (= "step1" (:id (first (:components job))))))
      (testing "Second component is a second step"
        (is (= "step2" (:id (second (:components job))))))
      (testing "Third component is a third step"
        (is (= "step3" (:id (nth (:components job) 2)))))
      (testing "The first 'next/to' of the first step is the identity of the second component."
        (is (= "step2" (:next/to (first (:transitions (first (:components job))))))))
      (testing "The first 'next/on' of the first step is as same as argument job."
        (is (= "status1" (:next/on (first (:transitions (first (:components job))))))))
      (testing "The second 'next/to' of the first step is the identity of the second component."
        (is (= "step3" (:next/to (second (:transitions (first (:components job))))))))
      (testing "The second 'next/on' of the first step is as same as argument job."
        (is (= "status2" (:next/on (second (:transitions (first (:components job))))))))))
  (testing "The job contains nested next phrase"
    (let [job (make-job job3)]
      (testing "First component is a first step"
        (is (= "step1" (:id (first (:components job))))))
      (testing "Second component is a second step"
        (is (= "step2" (:id (second (:components job))))))
      ;error
      (testing "Third component is a third step"
        (is (= "step3" (:id (nth (:components job) 2)))))
      (testing "The first 'next/to' of the first step is the identity of the second component."
        (is (= "step2" (:next/to (first (:transitions (first (:components job))))))))
      (testing "The first 'next/on' of the first step is as same as argument job."
        (is (= "status1" (:next/on (first (:transitions (first (:components job))))))))
      ;fail
      (testing "The first 'next/to' of the second step is the identity of the second component."
        (is (= "step3" (:next/to (first (:transitions (second (:components job))))))))
      (testing "The first 'next/on' of the second step is as same as argument job."
        (is (= "status2" (:next/on (first (:transitions (second (:components job)))))))))))

