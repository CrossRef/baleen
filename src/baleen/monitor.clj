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
            [baleen.redis :as redis]))

(def server (atom nil))

(liberator/defresource status-resource []
  :available-media-types ["application/json"]
  :handle-ok (fn [ctx]
    (with-open [redis-connection (redis/get-connection @bcontext/current-context)]
      (let [counter-names (.smembers redis-connection (str (bcontext/get-app-name @bcontext/current-context) "__counters"))
            counts (into {} (map (fn [counter-name]
                             (let [; drop the app-prefix (e.g. "reddit-" from key names)
                                   prefix-length (inc (.length (str (bcontext/get-app-name @bcontext/current-context))))
                                   counter-name-unprefixed (.substring counter-name prefix-length)

                                   history-queue-name (str counter-name "-history")
                                   history-values-str (.lrange redis-connection history-queue-name 0 -1)
                                   history-values (map #(Integer/parseInt (if (string/blank? %) "0" %)) history-values-str)

                                   current-value-str (.get redis-connection counter-name)
                                   current-value (Integer/parseInt (if (string/blank? current-value-str) "0" current-value-str))]

                                [counter-name-unprefixed {:current-count current-value
                                               :count-history history-values}])) counter-names))]
        (json/write-str {:app-name (bcontext/get-app-name @bcontext/current-context)
                         :app-friendly-name (bcontext/get-friendly-app-name @bcontext/current-context)
                         :counts counts})))))

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
  (compojure/GET "/status" [] (status-resource)))

(defn run
  "Run the monitoring server."
  [context]
  (let [pool (at-at/mk-pool)
        port (Integer/parseInt (:monitor-port (bcontext/get-config context)))]
    
    ; Shift inputs every 5 seconds.
    (at-at/every 5000 (partial shift context #"^.*-input-count$") pool)
    (at-at/every 5000 (partial shift context #"^.*-processed-count$") pool)

    ; Shift mathces every 5 minutes.
    (at-at/every 300000 (partial shift context #"^.*-matched-count$") pool)

    (l/info "Start server on port" port)
    (reset! server (run-server #'app {:port port}))))
    
