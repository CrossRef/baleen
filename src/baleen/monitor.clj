(ns baleen.monitor
  "Monitor activity, serve up."
  (:require [liberator.core :as liberator]
            [overtone.at-at :as at-at]
            [org.httpkit.server :refer [with-channel on-close on-receive send! run-server]]
            [compojure.core :as compojure])
  (:require [clojure.tools.logging :as l]
            [clojure.data.json :as json]
            [clojure.string :as string])
  (:require [baleen.context :as bcontext]
            [baleen.redis :as redis]
            [baleen.time :as baleen-time]))

(def server (atom nil))

(def at-at-pool (delay (at-at/mk-pool)))

(defn get-heartbeats
  "Return list of heartbeats and statuses as [{:name :value}]. If value is nil, that's a problem."
  [redis-connection context]
  (let [; Remove the prefix for display, showing only the name.
        counter-name-prefix-length (.length (str (bcontext/get-app-name context) "__heartbeat__"))
        all-heartbeats-key (str (bcontext/get-app-name context) "__heartbeats")
        heartbeat-names (.smembers redis-connection all-heartbeats-key)
        heartbeats (map (fn [heartbeat-name] {:name (.substring heartbeat-name counter-name-prefix-length)
                                              :value (.get redis-connection heartbeat-name)}) heartbeat-names)]
  heartbeats))

(defn get-queues-status
  [redis-connection context]
  (let [prefix-length (inc (.length (bcontext/get-app-name context)))
        queues-list-name (str (bcontext/get-app-name context) "__queues")
        queue-names (.smembers redis-connection queues-list-name)
        queues (map (fn [queue-name] {:name (.substring queue-name prefix-length)
                                      :length (.llen redis-connection queue-name)}) queue-names)]
    queues))

(liberator/defresource status-resource []
  :available-media-types ["application/json"]
  :handle-ok (fn [ctx]
    (with-open [redis-connection (redis/get-connection @bcontext/current-context)]
      (let [context @bcontext/current-context
            counter-names (.smembers redis-connection (str (bcontext/get-app-name context) "__counters"))
            counts (into {} (map (fn [counter-name]
                             (let [; drop the app-prefix (e.g. "reddit-" from key names)
                                   prefix-length (inc (.length (str (bcontext/get-app-name context))))
                                   counter-name-unprefixed (.substring counter-name prefix-length)

                                   history-queue-name (str counter-name "-history")
                                   history-values-str (.lrange redis-connection history-queue-name 0 -1)
                                   history-values (map #(Integer/parseInt (if (string/blank? %) "0" %)) history-values-str)

                                   current-value-str (.get redis-connection counter-name)
                                   current-value (Integer/parseInt (if (string/blank? current-value-str) "0" current-value-str))]
                                [counter-name-unprefixed {:current-count current-value
                                                          :count-history history-values}])) counter-names))

            heartbeats (get-heartbeats redis-connection context)
            queues (get-queues-status redis-connection context)

            heartbeat-errors (filter #(-> % :value nil?) heartbeats)]

        (json/write-str {:app-name (bcontext/get-app-name @bcontext/current-context)
                         :app-friendly-name (bcontext/get-friendly-app-name @bcontext/current-context)
                         :counts counts
                         :queues queues
                         :heartbeats heartbeats
                         :heartbeat-errors heartbeat-errors})))))


(liberator/defresource heartbeat-resource []
  :available-media-types ["application/json"]
  :service-available? (fn [ctx]
    (with-open [redis-connection (redis/get-connection @bcontext/current-context)]
      (let [heartbeats (get-heartbeats redis-connection @bcontext/current-context)
            heartbeat-errors (filter #(-> % :value nil?) heartbeats)]
        [(empty? heartbeat-errors) {::heartbeats heartbeats ::heartbeat-errors heartbeat-errors}])))
  :handle-ok (fn [ctx]
      (json/write-str {:heartbeats (::heartbeats ctx) :heartbeat-errors (::heartbeat-errors ctx)}))
  :handle-service-not-available (fn [ctx]
      (json/write-str {:heartbeats (::heartbeats ctx) :heartbeat-errors (::heartbeat-errors ctx)})))

(liberator/defresource recent-events-resource []
  :available-media-types ["application/json"]
  :handle-ok (fn [ctx]
    (with-open [redis-connection (redis/get-connection @bcontext/current-context)]
      (let [events-queue-name (str (bcontext/get-app-name @bcontext/current-context) "__push_history")
            events (.lrange redis-connection events-queue-name 0 -1)
            events-parsed (map json/read-str events)]
        (json/write-str events-parsed)))))
        

(def max-history-len 100)

(defn shift
  "Shift all counters that match a given expression."
  [context match-re]
  (l/info "Shift" match-re)
  (with-open [redis-connection (redis/get-connection context)]
    ; Fetch the set of counter names maintained by the `queue/enqueue-with-time`.
    (let [counter-names (.smembers redis-connection (str (bcontext/get-app-name context) "__counters"))
  
          matching-counter-names (filter #(re-matches match-re %) counter-names)]
      (doseq [counter-name matching-counter-names]
        (let [event-count (or (.get redis-connection counter-name))
              history-queue-name (str counter-name "-history")]
            (.del redis-connection counter-name)
            (.lpush redis-connection history-queue-name (into-array [(str event-count)]))
            (.ltrim redis-connection history-queue-name 0 max-history-len))))))

(compojure/defroutes app
  (compojure/GET "/status" [] (status-resource))
  (compojure/GET "/heartbeat" [] (heartbeat-resource))
  (compojure/GET "/recent-events" [] (recent-events-resource)))

(defn register-heartbeat
  "Register a named heartbeat. This will start running and keep running in a background thread.
  Do this once per permanently-running component."
  [context heartbeat-name]
  (let [all-heartbeats-key-name (str (bcontext/get-app-name context) "__heartbeats")
        heartbeat-key-name (str (bcontext/get-app-name context) "__heartbeat__" heartbeat-name)]
    (with-open [redis-connection (redis/get-connection context)]
      ; Add (or re-add) this to the collection of known heartbeats.
      ; Once registered, this will stay around forever and the monitor will expect all heartbeats to continue to exist.
      (.sadd redis-connection all-heartbeats-key-name (into-array [heartbeat-key-name])))

    ; Now set and expire the key forever. If the process dies, so does this.
    (at-at/every 1000
      (fn []
        (with-open [redis-connection (redis/get-connection context)]
         (.set redis-connection heartbeat-key-name (baleen-time/iso8601-now))
         (.expire redis-connection heartbeat-key-name 2)))
      @at-at-pool)))

(defn run
  "Run the monitoring server."
  [context]
  (let [
        port (Integer/parseInt (:monitor-port (bcontext/get-config context)))]
    
    ; Shift inputs every 5 seconds.
    (at-at/every 5000 (partial shift context #"^.*-received-count$") @at-at-pool)
    (at-at/every 5000 (partial shift context #"^.*-processed-count$") @at-at-pool)

    ; Shift mathces every 5 minutes.
    (at-at/every 300000 (partial shift context #"^.*-matched-count$") @at-at-pool)

    (l/info "Start server on port" port)
    (reset! server (run-server #'app {:port port}))))
    
