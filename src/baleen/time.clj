(ns baleen.time
  "Time-related functions. All dates should be clj-time types."
  (:require [clj-time.core :as clj-time]
            [clj-time.format :as clj-time-format]))

(def ymd (clj-time-format/formatter "yyyy-MM-dd"))

(defn format-ymd
  "Format a date to YYYY-MM-DD string."
  [date]
  (clj-time-format/unparse ymd date))


(defn now
  []
  (clj-time/now))

(defn ymd-now
  "Return today, as YYYY-MM-DD string."
  []
  (format-ymd (clj-time/now)))

(defn format-iso8601
  "Format a date to ISO8601 UTC Zulu string."
  [date]
  (str date))

(defn last-n-days
  "Return a seq of the last n days starting yesterday as midnight dates."
  [num-days]
  (map #(clj-time/minus (clj-time/now) (clj-time/days %)) (range 1 num-days)))
  

(defn last-n-days-ymd
  "Return a seq of the last n days starting yesterday in YYYY-MM-DD format."
  [num-days]
  (map format-ymd (last-n-days num-days)))

