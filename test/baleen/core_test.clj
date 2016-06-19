(ns baleen.core-test
  (:require [baleen.api :as api]
            [baleen.context :as context]
            [baleen.core :as core]
            [baleen.queue :as queue]
            [baleen.redis :as redis]
            [baleen.time :as time]
            [baleen.web :as web])
  (:require [clojure.test :refer :all]))

; Just check that everythign can be loaded.