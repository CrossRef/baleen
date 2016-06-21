(ns baleen.lagotto
  "Functions for interacting with Lagotto."
  (:require [baleen.context :as bcontext]
              [baleen.redis :as redis])
  (:import  [java.util.logging Logger Level])
  (:require [clojure.tools.logging :as l]
            [clojure.data.json :as json])
  (:require [org.httpkit.client :as http-client]
            [config.core :refer [env]]
            [crossref.util.doi :as cr-doi]))

(defn prepare-deposit
  "Create deposit JSON from inputs."
  [context &{:keys [subj-title subj-url subj-author subj-work-type obj-doi action event-id date-str source-id relation-type]}]
  (let [
        source-token (:lagotto-source-token (bcontext/get-config context))
        subject_metadata {"pid" subj-url
                          "author" {"literal" subj-author}
                          "title" subj-title
                          "issued" date-str
                          "URL" subj-url
                          "type" subj-work-type}

        payload (condp = action
          ; No message action when adding, only deleting.
          "add" {:deposit {:uuid event-id
                           :source_token source-token
                           :subj_id subj-url
                           :obj_id (cr-doi/normalise-doi obj-doi)
                           :relation_type_id relation-type
                           :source_id source-id
                           :occurred_at date-str
                           :subj subject_metadata}}
          "remove" {:deposit {:uuid event-id
                              :message_action "delete"
                              :source_token source-token
                              :subj_id subj-url
                              :obj_id (cr-doi/normalise-doi obj-doi)
                              :relation_type_id relation-type
                              :source_id source-id
                              :occurred_at date-str
                              :subj subject_metadata}}
          nil)]
    (when payload
      (json/write-str payload))))

(def max-push-history-len 100)

(defn send-deposit
  [context &{:keys [subj-title subj-url subj-author subj-container-title subj-work-type obj-doi action event-id date-str source-id relation-type]}]
  (let [endpoint (str (:lagotto-api-base-url (bcontext/get-config context)) "/api/deposits")
        
        auth-token (:lagotto-auth-token (bcontext/get-config context))

        payload (prepare-deposit 
          context
          :subj-title subj-title
          :subj-url subj-url
          :subj-author subj-author
          :subj-work-type subj-work-type
          :obj-doi obj-doi
          :action action
          :event-id event-id
          :date-str date-str
          :source-id source-id
          :relation-type relation-type)]

        (l/info "Sending payload" payload)

    (when payload
      (let [result @(http-client/post endpoint
                   {:headers {"Authorization" (str "Token token=" auth-token) "Content-Type" "application/json"}
                    :body payload})
            ok (= (:status result) 202)]
        
        ; Maintain last n push events.
        ; Don't run in test mode without booted context and connection to redis. TODO bit of a hack.
        (when (and ok @bcontext/current-context)
          (with-open [redis-connection (redis/get-connection context)]
            (let [queue-names-key-name (str (bcontext/get-app-name context) "__push_history")]
              (.lpush redis-connection queue-names-key-name (into-array [payload]))
              (.ltrim redis-connection queue-names-key-name 0 max-push-history-len))))

      (l/info "Result: ok?" ok ":" result)
      ok))))

