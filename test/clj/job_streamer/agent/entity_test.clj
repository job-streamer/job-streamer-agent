(ns job-streamer.agent.entity-test
  (:require [job-streamer.agent.entity :refer :all]
            [clojure.test :refer :all])
  (:import  [org.jsoup Jsoup]))

(deftest has-listeners-test
  (testing "has listeners"
    (is (has-listeners?
 (Jsoup/parse "<step id=\"2\">
 <listeners></listeners>
 <batchlet ref=\"example.HelloBatch\"></batchlet>
</step>")))
    (testing "has no listeners"
      (is (not (has-listeners?
 (Jsoup/parse "<step id=\"2\">
 <batchlet ref=\"example.HelloBatch\"></batchlet>
</step>")))))))

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
</job>") "<!--?xml version=\"1.0\" encoding=\"UTF-8\"?-->\n<html>\n <head></head>\n <body>\n  <job id=\"2\"> \n   <step id=\"1\"> \n    <next on=\"*\" to=\"2\"></next> \n    <batchlet ref=\"example.HelloBatch\"></batchlet> \n    <Listeners>\n     <Listener ref=\"net.unit8.job_streamer.agent.listener.StepProgressListener\"></Listener>\n    </Listeners>\n   </step> \n   <step id=\"2\"> \n    <batchlet ref=\"example.HelloBatch\"></batchlet> \n    <Listeners>\n     <Listener ref=\"net.unit8.job_streamer.agent.listener.StepProgressListener\"></Listener>\n    </Listeners>\n   </step> \n   <Listeners>\n    <Listener ref=\"net.unit8.job_streamer.agent.listener.JobProgressListener\"></Listener>\n   </Listeners>\n  </job>\n </body>\n</html>"))))


