(ns baleen.stash
  "Save things in S3."
  (:require [baleen.redis :as baleen-redis]
            [baleen.context :as bcontext])
  (:require [clojure.tools.logging :as l]
            [clojure.java.io :as io]
            [clojure.data.json :as json])
  (:import [com.amazonaws.services.s3 AmazonS3 AmazonS3Client]
           [com.amazonaws.auth BasicAWSCredentials]
           [com.amazonaws.services.s3.model GetObjectRequest ObjectMetadata PutObjectRequest])
  (:import [redis.clients.jedis Jedis])
  (:require [robert.bruce :refer [try-try-again]])
  (:gen-class))

(defn aws-client
  [context]
  (new AmazonS3Client (new BasicAWSCredentials (:s3-access-key-id (bcontext/get-config context)) 
                                               (:s3-secret-access-key (bcontext/get-config context)))))

(defn upload-file
  "Upload a file, return true if it worked."
  [context local-file remote-name content-type overwrite]
  (let [^AmazonS3 client (aws-client context)]
    (if (and (not overwrite)
             (.doesObjectExist client  (:archive-s3-bucket (bcontext/get-config context)) remote-name))
      (do
        (l/error "NOT uploading" local-file "to" remote-name ", already exists")
        false)
      (do 
        (l/info "Uploading" local-file "to" remote-name ".")
        (let [request (new PutObjectRequest (:archive-s3-bucket (bcontext/get-config context)) remote-name local-file)
              metadata (new ObjectMetadata)]
          (.setContentType metadata content-type)
          (.withMetadata request metadata)
          (.putObject client request)
      
      ; S3 isn't transactional, may take a while to propagate. Try a few times to see if it uploaded OK, return success.
       (try-try-again {:sleep 5000 :tries 10 :return? :truthy?} (fn []
          (.doesObjectExist client  (:archive-s3-bucket (bcontext/get-config context)) remote-name))))))))



(defn stash-jsonapi-list
  "Stash a list of JSON-able objects into a file in S3 using JSONAPI format and remove the key from Redis, if the key exists."
  [context list-data remote-name json-api-type overwrite]
  (let [tempfile (java.io.File/createTempFile "event-data-twitter-agent-stash" nil)
        counter (atom 0)]
    (let [decorated (map #(assoc % "type" json-api-type) list-data)
          api-object {"meta" {"status" "ok" 
                              "type" json-api-type}
                      "data" decorated}]
                      
      (with-open [writer (io/writer tempfile)]
        (json/write api-object writer)))
    
    (l/info "Saved " (count list-data) "items to" tempfile)

    ; If the upload worked OK, delete from Redis.
    (if-not (upload-file context tempfile remote-name "application/json" overwrite)
      (do
        (l/error "Failed to upload to " remote-name "!")
        (.delete tempfile)
        false)
      (do
        (l/info "Successful upload")
        (.delete tempfile)
        true))))


(defn stash-jsonapi-redis-list
  "Get a named Redis list containing JSON strings.
   Stash into a file in S3 using JSONAPI format."
  [context list-name remote-name json-api-type overwrite]
  (l/info "Attempt stash" list-name " -> " remote-name)
  (with-open [^Jedis redis-conn (baleen-redis/get-connection context)]
    (let [list-name-key (str (bcontext/get-app-name context))
          list-range (.lrange redis-conn list-name 0 -1)
          key-exists (.exists redis-conn list-name)]
      (if-not key-exists
        (l/info "Key" list-name "did not exist. This is expected for anything older than yesterday.")  
        (let [parsed-list (map json/read-str list-range)
              success (stash-jsonapi-list context parsed-list remote-name json-api-type overwrite)]
          (l/info "Result stash" list-name "->" remote-name "=" success)
          (when success
            (.del redis-conn (into-array [list-name]))))))))
