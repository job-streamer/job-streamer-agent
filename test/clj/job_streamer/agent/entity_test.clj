(ns job-streamer.agent.entity-test
  (:require [job-streamer.agent.entity :refer :all]
            [clojure.test :refer :all])
  (:import  [org.jsoup Jsoup]
            [org.jsoup.parser Tag Parser]))

(deftest has-listeners-test
  (testing "has listeners"
    (is (has-listeners?
          (some-> (Jsoup/parse "<job id=\"2\">
                               <step id=\"1\">
                               <next on=\"*\" to=\"2\"></next>
                               <listeners></listeners>
                               <batchlet ref=\"example.HelloBatch\"></batchlet>
                               </step>
                               <step id=\"2\">
                               <batchlet ref=\"example.HelloBatch\"></batchlet>
                               </step>
                               <listeners></listeners>
                               </job>") (.getElementsByTag "job") first))))
  (testing "has no listeners"
    (is (not (has-listeners?
               (some-> (Jsoup/parse "<job id=\"2\">
                                    <step id=\"1\">
                                    <next on=\"*\" to=\"2\"></next>
                                    <listeners></listeners>
                                    <batchlet ref=\"example.HelloBatch\"></batchlet>
                                    </step>
                                    <step id=\"2\">
                                    <batchlet ref=\"example.HelloBatch\"></batchlet>
                                    </step>
                                    </job>") (.getElementsByTag "job") first))))))

(deftest add-listeners-test
  (testing "add-listeners"
    (is (= (add-listeners
 "<?xml version=\"1.0\" encoding=\"UTF-8\"?>
<job id=\"2\">
<step id=\"1\">
 <next on=\"*\" to=\"2\"></next>
 <batchlet ref=\"example.HelloBatch\"></batchlet>
</step>
<step id=\"2\">
 <batchlet ref=\"example.HelloBatch\"></batchlet>
</step>
</job>") "<?xml version=\"1.0\" encoding=\"UTF-8\"?> \n<job id=\"2\"> \n <step id=\"1\"> \n  <next on=\"*\" to=\"2\"></next> \n  <batchlet ref=\"example.HelloBatch\"></batchlet> \n  <listeners>\n   <listener ref=\"net.unit8.job_streamer.agent.listener.StepProgressListener\"></listener>\n  </listeners>\n </step> \n <step id=\"2\"> \n  <batchlet ref=\"example.HelloBatch\"></batchlet> \n  <listeners>\n   <listener ref=\"net.unit8.job_streamer.agent.listener.StepProgressListener\"></listener>\n  </listeners>\n </step> \n <listeners>\n  <listener ref=\"net.unit8.job_streamer.agent.listener.JobProgressListener\"></listener>\n </listeners>\n</job>"))))


