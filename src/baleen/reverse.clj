(ns baleen.reverse
  "Utils for reversing landing pages into DOIs. Calls out to the 'DOI Destinations' service."
  (:require [org.httpkit.client :as http-client]
            [clojure.data.json :as json])

    )


(defn query-reverse-api
  "Query the reverse API with a URL or a snippet of text."
  ; TODO refactor into baleen.
  [context query]
  (let [url (str (:doi-destinations-base-url (baleen.context/get-config context)) "/guess-doi")
        response @(http-client/get url {:query-params {"q" query}})]
    (when (= 200 (:status response))
      (:body response))))

(defn fetch-domains
  "Fetch a new set of rules from the DOI Destinations service. Return JSON blob."
  [context]
  (let [url (str (:doi-destinations-base-url (baleen.context/get-config context)) "/data/domain-names.json")]
    ; TODO temp hack to exclude things like ".".
    (->> url http-client/get deref :body json/read-str (filter #(> (.length %) 5)))

  ; Single domain for prototyping.
  ; ["scitation.aip.org"]
  ; ["aac.asm.org" "journals.plos.org" "advances.sciencemag.org"  "onlinelibrary.wiley.com" "scitation.aip.org"]
  ))
