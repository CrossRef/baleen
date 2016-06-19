(ns baleen.context-test
  (:require [clojure.test :refer :all]
            [baleen.context :as context]))

(deftest attributes
  (let [^Context c (context/->Context "my-app" "My Application" #{:custom-config-key})]

    (testing "Attributes can be retrieved"
      (is (= "my-app" (context/get-app-name c)))
      (is (= "My Application" (context/get-friendly-app-name c)))
      )

    (testing "Config keys contain both base and custom keys.")
      (is (:custom-config-key (context/get-config-keys c)))
      (is (:redis-host (context/get-config-keys c)))))
