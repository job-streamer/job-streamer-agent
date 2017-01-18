(ns job-streamer.agent.entity
  (:require [clojure.data.xml :refer [element emit-str parse] :as xml])
  (:import  [org.jsoup Jsoup]
           [org.jsoup.nodes Element Node]
           [org.jsoup.parser Tag]))

(defn has-listeners? [parent-element]
               (some-> parent-element (.getElementsByTag "Listeners") empty? not))

(defn add-listener [parent-element listener-name]
 (when (not (has-listeners? parent-element))
   (.appendChild parent-element (Element. (Tag/valueOf "Listeners") "")))
 (some-> parent-element (.getElementsByTag "Listeners") first (.appendChild (some->  (Tag/valueOf "Listener") (Element. "") (.attr "ref" listener-name)))))


(defn add-listeners [jobxml-str]
 (let [jobxml (Jsoup/parse  jobxml-str)]
   (some-> jobxml (.getElementsByTag "job") first (add-listener "net.unit8.job_streamer.agent.listener.JobProgressListener"))
   (doall (some-> jobxml (.getElementsByTag "step")
                  (as-> parent
                        (map #(add-listener % "net.unit8.job_streamer.agent.listener.StepProgressListener") parent))))
   (.toString jobxml)))


