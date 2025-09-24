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
  ;; TODO: Implement actual EFP API response transformation in F2.2
  ;; For now, assume api-response is already an array of addresses
  (-> api-response
      filter-valid-addresses
      dedupe-addresses))

;; Future utility functions:
;; - ENS batch resolution helpers
;; - Profile photo caching utilities  
;; - Social graph analysis (mutual friends, etc.)
;; - Search/filter functions for large friend lists
