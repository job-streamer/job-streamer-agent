(ns job-streamer.agent.component.beacon
  (:require [clojure.tools.logging :as log]
            [clojure.core.async :refer [go-loop chan <! close! timeout put!]]
            [com.stuartsierra.component :as component]
            [environ.core :refer [env]])
  (:import [java.io ByteArrayOutputStream DataOutputStream File]
           [java.net InetSocketAddress InetAddress NetworkInterface]
           [java.nio ByteBuffer]
           [java.nio.file Files]
           [java.nio.channels DatagramChannel]))

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

(defn stop-notifications [beacon]
  (put! (:notifier-channel beacon) :stop))

(defn start-notifications [beacon]
  (put! (:notifier-channel beacon) :start))

(defn beacon-loop [notifier-channel port]
  (go-loop [status :start]
    (when-let [comm (<! notifier-channel)]
      (case comm
        :stop (do (log/info "stop broadcast.")
                  (recur :stop))
        :start (do (put! notifier-channel :continue)
                   (recur :start))
        (do
          (when-not (= status :stop)
            (notify port)
            (<! (timeout 10000))
            (put! notifier-channel :continue))
          (recur status))))))

(defrecord Beacon [port]
  component/Lifecycle

  (start [component]
    (log/info "start beacon:" component)
    (if (:main-loop component)
      component
      (let [notifier-channel (chan)
            main-loop (beacon-loop notifier-channel port)]
        (put! notifier-channel :start)
        (assoc component
               :main-loop main-loop
               :notifier-channel notifier-channel))))

  (stop [component]
    (log/info "stop beacon:" component)
    (when-let [notifier-channel (:notifier-channel component)]
      (close! notifier-channel))
    (when-let [main-loop (:main-loop component)]
      (close! main-loop))
    (dissoc component :main-loop :notifier-channel)))

(defn beacon-component [options]
  (map->Beacon options))
