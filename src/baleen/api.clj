(ns baleen.api
  "Routines for paging API access")


(defn api-page-seq
  [url-fn starting-page date-parse-f start-date end-date]
  "Return a seq of pages items from an API as text.
   A parse-function is supplied, which parse the page into a seq of items.
   Every item must include a :date key, which is used to filter the items.
   Return a seq of [page-body items]
  The API must be ordered in order, newest to oldest. 
  
   - url-fn - function to return [url query-params] given page number
   - starting-page - starting page number
   - parse-f - function that returns a seq of item dates found on the page
   - start-date - keep dates including and after this
   - end-date - keep dates before this"

; TODO
  )