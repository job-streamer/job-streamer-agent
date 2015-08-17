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

(defn send-multicast [channel msg ip-address port]
  (.. channel socket (setBroadcast false))
  (let [address (InetSocketAddress. ip-address port)]
    (.connect channel address)
    (.send channel msg address)))

(defn send-broadcast [channel msg ip-address port]
  (.. channel socket (setBroadcast true))
  (let [address (InetSocketAddress. ip-address port)]
    (.connect channel address)
    (.send channel msg address)))

(defn notify [port]
  (let [baos (ByteArrayOutputStream.)
        dos  (DataOutputStream. baos)
        ch   (DatagramChannel/open)]
    (try
      (doseq [interface (enumeration-seq (NetworkInterface/getNetworkInterfaces))]
        (->> (.getInterfaceAddresses interface) 
             (map #(.getAddress %))
             (filter #(and (instance? java.net.Inet4Address %)
                           (not (.isLoopbackAddress %))))
             (map #(doto dos
                     (.write (.getAddress %) 0 4)
                     (.writeInt port)))
             doall))
      (doto ch
        (.configureBlocking true))
      (let [port (Integer/parseInt (or (:discovery-port env) "45100"))
            msg (ByteBuffer/wrap (.toByteArray baos))]
        (if-let [multicast-address (env :discovery-address)]
          (send-multicast ch msg multicast-address port)
          (send-broadcast ch msg "255.255.255.255" port))
        (log/info "Notify broadcast."))
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

