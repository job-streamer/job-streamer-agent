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
    (element :chunk (->> (select-keys this [:item-count])
                         (filter #(second %))
                         (into {}))
             (when reader
               (element :reader    {:ref (:ref reader)}))
             (when processor
               (element :processor {:ref (:ref processor)}))
             (when writer
               (element :writer    {:ref (:ref writer)})))))

(defrecord Step [id start-limit allow-start-if-complete next transition-elements
                 chunk batchlet properties]
  XMLSerializable
  (to-xml [this]
    (element :step (merge
                    {:id (:name this)
                     :allow-start-if-complete (:allow-start-if-complete? this)}
                    (->> (select-keys this [:start-limit :next])
                         (filter #(second %))
                         (into {}))) 
             (when-let [properties (some-> (:properties this))]
               (properties->xml properties))
             (when-let [chunk (some-> (:chunk this) (map->Chunk))]
               (to-xml chunk))
             (when-let [batchlet (some-> (:batchlet this) (map->Batchlet))]
               (to-xml batchlet))
             (element :listeners {}
                      (element :listener {:ref "net.unit8.job_streamer.agent.listener.StepProgressListener"})))))

(defn make-step [step]
  (merge (map->Step step)
         (when-let [batchlet (:batchlet step)]
           {:step/batchlet (map->Batchlet batchlet)})
         (when-let [chunk (:chunk step)]
           {:step/chunk (map->Chunk chunk)})))

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
        {:keys [name restartable? steps properties]
         :or {restartable? true, steps [], properties {}}} job]
    (->Job name restartable? (map make-step steps) properties)))

