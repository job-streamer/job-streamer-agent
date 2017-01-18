(ns job-streamer.agent.entity
  (:require [clojure.data.xml :refer [element emit-str parse] :as xml])
  (:import  [org.jsoup Jsoup]
           [org.jsoup.nodes Element Node]
           [org.jsoup.parser Tag Parser]))

(defn has-listeners? [parent-element]
               (some-> parent-element (.getElementsByTag "listeners") empty? not))

(defn add-listener [parent-element listener-name]
 (when (not (has-listeners? parent-element))
   (.appendChild parent-element (Element. (Tag/valueOf "listeners") "")))
 (some-> parent-element (.getElementsByTag "listeners") first (.appendChild (some->  (Tag/valueOf "listener") (Element. "") (.attr "ref" listener-name)))))


(defn add-listeners [jobxml-str]
 (let [jobxml (Jsoup/parse  jobxml-str "" (Parser/xmlParser))]
   (some-> jobxml (.getElementsByTag "job") first (add-listener "net.unit8.job_streamer.agent.listener.JobProgressListener"))
   (doall (some-> jobxml (.getElementsByTag "step")
                  (as-> parent
                        (map #(add-listener % "net.unit8.job_streamer.agent.listener.StepProgressListener") parent))))
   (.toString jobxml)))

(defn has-properties? [parent-element]
  (some-> parent-element (.getElementsByTag "properties") empty? not))

(defn add-property [parent-element id]
  (when (not (has-properties? parent-element))
    (.appendChild parent-element (Element. (Tag/valueOf "properties") "")))
  (some-> parent-element (.getElementsByTag "properties") first (.appendChild (some->  (Tag/valueOf "property") (Element. "") (.attr "name" "request-id") (.attr "value" id)))))

(defn add-request-id [jobxml-str id]
  (let [jobxml (Jsoup/parse  jobxml-str "" (Parser/xmlParser))]
    (some-> jobxml (.getElementsByTag "job") first (add-property id))
    (.toString jobxml)))


