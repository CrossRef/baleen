(ns baleen.context
  "A Baleen context object."
  (:require [clojure.set :as set]
            [clojure.tools.logging :as l])
  (:require [config.core :refer [env]]))

(def current-context
  "The currently operating context.
  A global variable to hold a context object, so use sparingly if ever."
  (atom nil))

(defprotocol BaleenContext
  "A context object for a Baleen application."
  
  (get-app-name [this] "Name of the app for filenames etc.")
  (get-friendly-app-name [this] "User-friendly name of the app for storage use.")
  (get-config-keys [this] "Collection of required config keys.")

  (get-config [this] "Return the config object.")

  (config-ok? [this] "Is the config OK? If not, logs and returns false.")

  (boot! [this] "Start the application context, start database connections etc. Return success."))

(def base-required-config-keys #{
    :doi-destinations-base-url ; base URL for DOI Destinations service.
    :archive-s3-bucket ; S3 Bucket name that holds archive information.
    :s3-access-key-id ; AWS access key for putting logs.
    :s3-secret-access-key ; AWS access key for putting logs.
    :redis-host ; host running redis server
    :redis-port ; port of redis server
    :redis-db-number ; redis database number to use
    :monitor-port ; port to run monitoring server on

    :lagotto-api-base-url
    :lagotto-source-token
    :lagotto-auth-token})

(deftype Context
  [app-name
   friendly-app-name
   config-keys]
  BaleenContext

  (get-app-name [this]
    app-name)

  (get-friendly-app-name [this]
    friendly-app-name)

  (get-config-keys [this]
    (set/union base-required-config-keys config-keys))

  (config-ok? [this]
    (l/info "Looking for " (get-config-keys this))
    (l/info "Base keys:" base-required-config-keys)
    (l/info "App keys:" config-keys)
    (let [missing (set/difference (get-config-keys this) (set (keys env)))]
      (if (empty? missing)
        true
        (do
          (l/fatal "Missing keys" missing)
          false))))

  (get-config [this]
    env)

  (boot! [this]
    (l/info "Booting" friendly-app-name)
    (let [config-ok (config-ok? this)]
      (l/info "Config" config-ok)
      (reset! current-context this)
      (l/info "Current context now" @current-context)
      
      ; Mean but fair.
      (when-not config-ok (System/exit 1)))))
