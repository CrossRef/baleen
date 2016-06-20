(ns baleen.lagotto-test
  (:require [baleen.lagotto :as lagotto]
            [baleen.context :as bcontext])
  (:use org.httpkit.fake)
  (:require [clojure.test :refer :all]))

(deftest post-lagotto
  (testing "POST to deposits with success works correctly."
    (let [context (bcontext/->Context
                                    "test-agent"
                                    "Test Agent"
                                    #{})]

      (with-fake-http [{:url "http://test.api.eventdata.crossref.org/deposits" :method :post}
                       {:status 202 :body ""}]
        (let [post-result (lagotto/send-deposit
                              context
                              :subj-title "Fish article"
                              :subj-url "http://fish.com/on-fish"
                              :subj-author "Jim Hake"
                              :subj-container-title "Fish Journal"
                              :subj-work-type "journal-article"
                              
                              :obj-doi "10.5040/9781580818650.p01"
                              :action "add"
                              
                              :event-id "123456"
                              :date-str "2016-06-20T08:30:15+00:00"
                              :source-id "fish-net"
                              
                              :relation-type "cites")]

      (is (= true post-result))))))

  (testing "POST to deposits without success returns failure."
    (let [context (bcontext/->Context
                                    "test-agent"
                                    "Test Agent"
                                    #{})]

      (with-fake-http [{:url "http://test.api.eventdata.crossref.org/deposits" :method :post}
                       {:status 400 :body ""}]
        (let [post-result (lagotto/send-deposit
                              context
                              :subj-title "Fish article"
                              :subj-url "http://fish.com/on-fish"
                              :subj-author "Jim Hake"
                              :subj-container-title "Fish Journal"
                              :subj-work-type "journal-article"
                              
                              :obj-doi "10.5040/9781580818650.p01"
                              :action "add"
                              
                              :event-id "123456"
                              :date-str "2016-06-20T08:30:15+00:00"
                              :source-id "fish-net"
                              
                              :relation-type "cites")]

      (is (= false post-result)))))))


(deftest lagotto-payload
  (testing "Payload correctly constructed"
    (let [context (bcontext/->Context
                                    "test-agent"
                                    "Test Agent"
                                    #{})
          result (lagotto/prepare-deposit
                    context
                    :subj-title "Fish article"
                    :subj-url "http://fish.com/on-fish"
                    :subj-author "Jim Hake"
                    :subj-work-type "journal-article"
                    
                    :obj-doi "10.5040/9781580818650.p01"
                    :action "add"
                    
                    :event-id "123456"
                    :date-str "2016-06-20T08:30:15+00:00"
                    :source-id "fish-net"
                    
                    :relation-type "cites")]
    (is (= result
            "{\"deposit\":{\"uuid\":\"123456\",\"source_token\":\"SOURCE_TOKEN\",\"subj_id\":\"http:\\/\\/fish.com\\/on-fish\",\"obj_id\":\"https:\\/\\/doi.org\\/10.5040\\/9781580818650.p01\",\"relation_type_id\":\"cites\",\"source_id\":\"fish-net\",\"occurred_at\":\"2016-06-20T08:30:15+00:00\",\"subj\":{\"pid\":\"http:\\/\\/fish.com\\/on-fish\",\"author\":{\"literal\":\"Jim Hake\"},\"title\":\"Fish article\",\"issued\":\"2016-06-20T08:30:15+00:00\",\"URL\":\"http:\\/\\/fish.com\\/on-fish\",\"type\":\"journal-article\"}}}")))))



