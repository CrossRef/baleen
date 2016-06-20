(ns baleen.queue
  "Queueing and logging functions.
  These queues operate as normal queues but also keep a daily log of values that pass through them."
  (:require [clojure.tools.logging :as l])
  (:require [baleen.context :as bcontext]
            [baleen.time :as btime]
            [baleen.redis :as redis]))

(defn enqueue-with-time
  "Enqueue an input as a JSON blob.
   Save in the input queue and also the daily input log goverened by the time it was created."
  [context queue-name event-time json-blob]
  (with-open [redis-connection (redis/get-connection)]
    (let [queue-key-name (str (bcontext/get-app-name context) "-" queue-name)
          log-key-name (str (bcontext/get-app-name context) "-" queue-name "-" (btime/format-ymd event-time))]
      ; Push to start of queue and end of log.
      (.lpush redis-connection queue-key-name (into-array [json-blob]))
      (.rpush redis-connection log-key-name (into-array [json-blob])))))

(defn enqueue
  "Enqueue an input as a JSON blob.
   Save in the input queue and also the daily input log goverened by the time it was created."
   [context queue-name json-blob]
   (enqueue-with-time context queue-name (btime/now) json-blob))

(defn process-queue
  "Process a named queue, blocking infinitely.
  Save work-in-progress (or failed items) on a working queue.

  See http://redis.io/commands/rpoplpush for reliable queue pattern."
  [context queue-name function]
  (let [redis-connection (redis/get-connection)
        queue-key-name (str (bcontext/get-app-name context) "-" queue-name)
        working-queue-key-name (str (bcontext/get-app-name context) "-" queue-name "-working")]
    (l/debug "Processing queue:" working-queue-key-name "working queue name:" working-queue-key-name)
    (loop []
      (let [item-str (.brpoplpush redis-connection queue-key-name working-queue-key-name 0)
            success (function item-str)]
        ; Once this is done successfully remove from the working queue.
        (when success
          (.lrem redis-connection working-queue-key-name 0 item-str)))
      (recur))))

