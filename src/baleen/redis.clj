(ns baleen.redis
  "Redis functions"
  
  (:require [config.core :refer [env]])
  (:require [clojure.tools.logging :as l])
  (:import [redis.clients.jedis Jedis JedisPool JedisPoolConfig]))

(defn make-jedis-pool
  []
  (let [^JedisPool pool (new JedisPool (:redis-host env) (Integer/parseInt (:redis-port env)))]
  pool))

(def jedis-pool
  (delay (make-jedis-pool)))

(defn get-connection
  "Get a Redis connection from the pool. Must be closed."
  []
  (let [resource (.getResource @jedis-pool)]
    (.select resource (Integer/parseInt (:redis-db-number env)))
    resource))
