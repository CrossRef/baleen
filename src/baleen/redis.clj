(ns baleen.redis
  "Redis functions"
  (:require [baleen.context :as bcontext])
  (:require [clojure.tools.logging :as l])
  (:import [redis.clients.jedis Jedis JedisPool JedisPoolConfig]))

(defn make-jedis-pool
  [context]
  ; Nice big pool in case we have multiple threads trying to interact with multiple queues.
  ; Real chance of deadlock otherwise!
  (let [pool-config (new org.apache.commons.pool2.impl.GenericObjectPoolConfig)]
    (.setMaxTotal pool-config 100)
  (new JedisPool pool-config (:redis-host (bcontext/get-config context)) (Integer/parseInt (:redis-port (bcontext/get-config context))))))


(def jedis-pool
  "Jedis pool based on the currently booted context. This must be called after context has booted."
  (delay (make-jedis-pool @bcontext/current-context)))

(defn get-connection
  "Get a Redis connection from the pool. Must be closed."
  [context]
  (let [resource (.getResource @jedis-pool)]
    (.select resource (Integer/parseInt (:redis-db-number (bcontext/get-config context))))
    resource))
