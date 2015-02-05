(ns job-streamer.agent.connector
  (:require [clojure.tools.logging :as log])
  (:use [clojure.core.async :only [chan put! go-loop <! timeout]]
        [environ.core :only [env]])
  (:import [java.io ByteArrayOutputStream DataOutputStream File]
           [java.net InetSocketAddress InetAddress NetworkInterface]
           [java.nio ByteBuffer]
           [java.nio.file Files]
           [java.nio.channels DatagramChannel]))

(def notifier-channel (chan))
(def port-cache (atom nil))

(defn write-address [address port stream]
  (doto stream
    (.write (.getAddress address) 0 4)
    (.writeInt port)))

(defn notify [port]
  (let [baos (ByteArrayOutputStream.)
        dos  (DataOutputStream. baos)
        ch   (DatagramChannel/open)]
    (try
      (loop [i 1]
        (when-let [interface (NetworkInterface/getByIndex i)]
          (->> (.getInterfaceAddresses interface) 
               (map #(.getAddress %))
               (filter #(and (instance? java.net.Inet4Address %)
                             (not (.isLoopbackAddress %))))
               (map #(doto dos
                       (.write (.getAddress %) 0 4)
                       (.writeInt port)))
               doall)
          (recur (inc i))))
      (.. ch socket (setBroadcast true))
      (.send ch
        (ByteBuffer/wrap (.toByteArray baos))
        (InetSocketAddress. "255.255.255.255" (or (env :control-bus-port) 45100)))
      (log/info "Notify broadcast.")
      (finally (.close ch)))))

(defn start [port]
  (reset! port-cache port)
  (go-loop []
    (let [comm (<! notifier-channel)]
      (if (= comm :stop)
        (log/info "stop broadcast.")
        (do
          (notify port)
          (<! (timeout 10000))
          (put! notifier-channel :continue)
          (recur)))))
  (put! notifier-channel :start))

(defn restart []
  (if @port-cache
    (start @port-cache)
    (throw (IllegalStateException.))))

(defn stop []
  (put! notifier-channel :stop))

