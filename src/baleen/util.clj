(ns baleen.util
  "This and that"
  (:import [java.util UUID]))

(defn new-uuid []
  (.toString (UUID/randomUUID)))

