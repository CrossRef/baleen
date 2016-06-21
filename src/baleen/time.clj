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

(defn iso8601-now
  []
  (format-iso8601 (now)))

(defn last-n-days
  "Return a seq of the last n days starting :yesterday or :day-before-yesterday as midnight dates."
  [num-days from]
  (let [from-days ({:yesterday 1 :day-before-yesterday 2} from 0)]
    (map #(clj-time/minus (clj-time/now) (clj-time/days %)) (range from-days num-days))))
  

(defn last-n-days-ymd
  "Return a seq of the last n days starting :yesterday or :day-before-yesterday in YYYY-MM-DD format."
  [num-days from]
  (map format-ymd (last-n-days num-days from)))

