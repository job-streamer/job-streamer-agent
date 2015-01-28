(ns job-streamer.agent.entity
  (:use [clojure.walk :only [keywordize-keys stringify-keys]])
  (:require [clojure.data.xml :refer [element emit-str] :as xml])
  (:import [javax.xml.stream XMLStreamWriter XMLOutputFactory]))

(defn properties->xml [properties]
  (element :properties {}
           (map #(element :property {:name (-> % first name)
                                     :value (second %)}) properties)))

(defprotocol XMLSerializable (to-xml [this]))

(defrecord Batchlet [ref]
  XMLSerializable
  (to-xml [this]
    (element :batchlet (select-keys this [:ref]))))

(defrecord Chunk [reader processor writer checkpoint-policy commit-interval buffer-reades chunk-size skip-limit retry-limit]
  XMLSerializable
  (to-xml [this]
    (element :chunk (->> (select-keys this [:reader :processor :writer])
                         (filter #(second %))
                         (into {})))))

(defrecord Step [id start-limit allow-start-if-complete next transition-elements
                 chunk batchlet]
  XMLSerializable
  (to-xml [this]
    (element :step (->> (select-keys this [:id :start-limit :allow-start-if-complete :next])
                        (filter #(second %))
                        (into {}))
             (when-let [chunk (some-> (:chunk this) (map->Chunk))]
               (to-xml chunk))
             (when-let [batchlet (some-> (:batchlet this) (map->Batchlet))]
               (to-xml batchlet))
             (element :listeners {}
                      (element :listener {:ref "net.unit8.job_streamer.agent.listener.StepProgressListener"})))))

(defn make-step [step]
  (-> (map->Step step)
      (assoc :step/batchlet (map->Batchlet {:ref (get-in step [:batchlet :ref])}))))

(defrecord Job [id restartable steps properties]
  XMLSerializable
  (to-xml [this]
    (element :job (select-keys this [:id :restartable])
             (properties->xml properties)
             (element :listeners {}
                      (element :listener {:ref "net.unit8.job_streamer.agent.listener.JobProgressListener"}))
             (map to-xml steps))))

(defn make-job [job]
  (let [job (keywordize-keys (stringify-keys job))
        {:keys [id restartable steps properties]
         :or {restartable true, steps [], properties {}}} job]
    (->Job id restartable (map make-step steps) properties)))

