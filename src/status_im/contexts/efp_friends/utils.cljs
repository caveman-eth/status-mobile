(ns status-im.contexts.efp-friends.utils
  (:require
    [clojure.string :as string]))

;; EFP Friends Utilities
;; Data transformation and helper functions

(defn format-address-short
  "Format ETH address to short display format (0x1234...5678)"
  [address]
  (when (and address (>= (count address) 10))
    (str (subs address 0 6) "..." (subs address -4))))

(defn create-friend-recipient
  "Create recipient object for wallet send flow"
  [{:keys [address ens-name display-name profile-photo customization-color] :as friend}]
  {:recipient-type      :efp-friend
   :label               (or ens-name display-name (format-address-short address))
   :ens-name            ens-name
   :profile-photo       profile-photo  
   :customization-color (or customization-color :blue)
   :address             address})

(defn enrich-address-with-ens
  "Enrich raw address with ENS and metadata"
  [address ens-data]
  {:address             address
   :ens-name           (:name ens-data)
   :display-name       (or (:name ens-data) (format-address-short address))
   :profile-photo      (:avatar ens-data)
   :customization-color :blue ; TODO: Derive from ENS or user preference
   :recipient-type     :efp-friend})

(defn filter-valid-addresses
  "Filter out invalid ETH addresses"
  [addresses]
  (filter (fn [addr]
            (and (string? addr)
                 (= 42 (count addr))
                 (.startsWith addr "0x")))
          addresses))

(defn dedupe-addresses
  "Remove duplicate addresses while preserving order"
  [addresses]
  (vec (distinct addresses)))

(defn sort-friends-by-name
  "Sort friends list alphabetically by display name"
  [friends]
  (sort-by (fn [friend]
             (string/lower-case (or (:ens-name friend) 
                                   (:display-name friend)
                                   (:address friend))))
           friends))

(defn cache-key-for-address
  "Generate cache key for user's EFP friends"
  [user-address]
  (str "efp-friends-" user-address))

(defn transform-efp-api-response
  "Transform raw EFP API response to internal format"
  [api-response]
  ;; Extract addresses from EFP API response structure
  ;; EFP API returns: {"following": [{"record_type": "address", "data": "0x123...", "tags": ["efp"]}]}
  (let [following-records (get api-response "following" [])]
    (->> following-records
         (filter #(= (get % "record_type") "address")) ; Only address records
         (map #(get % "data"))                         ; Extract the address
         (filter some?)                                ; Remove nil values
         filter-valid-addresses                        ; Validate addresses
         dedupe-addresses)))                           ; Remove duplicates

(defn extract-efp-tags
  "Extract tags from EFP following record"
  [efp-record]
  (get efp-record "tags" []))

(defn efp-record->friend-data
  "Convert EFP following record to friend data structure"
  [efp-record]
  (when (= (get efp-record "record_type") "address")
    (let [address (get efp-record "data")
          tags (extract-efp-tags efp-record)]
      {:address             address
       :ens-name           nil ; Will be enriched in Phase 3
       :display-name       (format-address-short address)
       :profile-photo      nil ; Will be enriched in Phase 3
       :customization-color :blue
       :recipient-type     :efp-friend
       :efp-tags          tags})))

(defn build-efp-api-url
  "Build EFP API URL with parameters"
  [base-url path params]
  (let [param-strings (for [[k v] params :when v]
                        (str (name k) "=" (if (keyword? v) (name v) v)))
        query-string (when (seq param-strings) 
                       (str "?" (string/join "&" param-strings)))]
    (str base-url path (or query-string ""))))

;; Future utility functions:
;; - ENS batch resolution helpers
;; - Profile photo caching utilities  
;; - Social graph analysis (mutual friends, etc.)
;; - Search/filter functions for large friend lists
