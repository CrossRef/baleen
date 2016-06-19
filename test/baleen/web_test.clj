(ns baleen.web-test
  (:require [baleen.web :as web])
  (:use org.httpkit.fake)
  (:require [clojure.test :refer :all]))

(deftest canonical
  (testing "Can get canonical"
    (with-fake-http [{:url "https://en.m.wikipedia.org/wiki/Fish"
                      :method :get}
                     {:status 201
                      :body (slurp "resources/test/wikipedia.html")}]
                      
  (is (= "https://en.wikipedia.org/wiki/Fish"
         (web/fetch-canonical-url "https://en.m.wikipedia.org/wiki/Fish"))))))

(deftest extraction
  (testing "Can extract DOIs and URLs from body"
    (.contains
      (web/extract-a-hrefs-from-html (slurp "resources/test/wikipedia.html"))
      "/wiki/Zooplankton")
    (= 869 (count (web/extract-a-hrefs-from-html (slurp "resources/test/wikipedia.html")))))

    (is (= #{"10.1016/0300-9629(73)90490-8" "10.1007/s003600050092" "10.1007/BF00002518" "10.1590/S0102-09352003000500018" "10.2307/1442742" "10.1590/S0101-81752005000300005" "10.2307/1447796"}
           (web/extract-dois-from-body (slurp "resources/test/wikipedia.html")))))

