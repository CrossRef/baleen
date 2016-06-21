(ns baleen.queue
  "Queueing and logging functions.
  These queues operate as normal queues but also keep a daily log of values that pass through them."
  (:require [clojure.tools.logging :as l])
  (:require [baleen.context :as bcontext]
            [baleen.time :as btime]
            [baleen.redis :as redis]))

(defn enqueue-with-time
  "Enqueue an input as a JSON blob.
   By default save in the input queue and also the daily input log goverened by the time it was created.
   Also increment named counter. Every queue has one for monitoring purposes."
  [context queue-name event-time json-blob daily-log]
  (with-open [redis-connection (redis/get-connection context)]
    (let [queue-key-name (str (bcontext/get-app-name context) "-" queue-name)
          log-key-name (str (bcontext/get-app-name context) "-" queue-name "-" (btime/format-ymd event-time))
          counter-key-name (str (bcontext/get-app-name context) "-" queue-name "-count")
          counters-names-key-name (str (bcontext/get-app-name context) "__counters")]
      
      ; Increment counter.
      (.incr redis-connection counter-key-name)

      ; Also maintain a set of known counter keys.
      (.sadd redis-connection counters-names-key-name (into-array [counter-key-name]))

      ; Push to start of queue.
      (.lpush redis-connection queue-key-name (into-array [json-blob]))

      ; Optionally push to end of log.
      (when daily-log
        (.rpush redis-connection log-key-name (into-array [json-blob]))))))

(defn enqueue
  "Enqueue an input as a JSON blob.
   Save in the input queue and also the daily input log goverened by the time it was created."
   [context queue-name json-blob daily-log]
   (enqueue-with-time context queue-name (btime/now) json-blob daily-log))

(defn process-queue
  "Process a named queue, blocking infinitely.
  Save work-in-progress (or failed items) on a working queue.
  If :keep-done, save everything processed in another list.

  See http://redis.io/commands/rpoplpush for reliable queue pattern."
  [context queue-name function & {keep-done :keep-done}]
  (with-open [redis-connection (redis/get-connection context)]
    (let [queue-key-name (str (bcontext/get-app-name context) "-" queue-name)
          working-queue-key-name (str (bcontext/get-app-name context) "-" queue-name "-working")
          done-queue-key-name (str (bcontext/get-app-name context) "-" queue-name "-done")]
      (loop []
        (let [item-str (.brpoplpush redis-connection queue-key-name working-queue-key-name 0)
              success (function item-str)]
          ; Once this is done successfully remove from the working queue.
          (when success
            (.lrem redis-connection working-queue-key-name 0 item-str)
            (when keep-done
              (.rpush redis-connection done-queue-key-name (into-array [item-str])))))
        (recur)))))

