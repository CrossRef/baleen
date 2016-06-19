(ns baleen.time-test
  (:require [clojure.test :refer :all]
            [baleen.time :as btime]))

(deftest a-test
  (testing "Date formats correct"
    (is (= "1998-05-02" (btime/format-ymd (clj-time.core/date-time 1998 05 02 11 22))))
    (is (= "1998-05-02T11:22:00.000Z" (btime/format-iso8601 (clj-time.core/date-time 1998 05 02 11 22))))))


