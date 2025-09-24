(ns status-im.contexts.efp-friends.effects
  (:require
    [clojure.string :as string]
    [re-frame.core :as rf]
    [taoensso.timbre :as log]
    [utils.transforms :as transforms]))

;; EFP Friends Effects
;; Handles external EFP API integration using the EFP API
;; API Documentation: https://ethidentitykit.com/docs/api/users/following

(def ^:const efp-api-base-url "https://api.ethfollow.xyz/api/v1")

(defn- build-efp-following-url
  "Build EFP API URL for getting user's following list"
  [user-address {:keys [limit offset sort cache] :or {limit 50 sort "latest"}}]
  (let [base-url (str efp-api-base-url "/users/" user-address "/following")
        params (cond-> []
                 limit (conj (str "limit=" limit))
                 offset (conj (str "offset=" offset))
                 sort (conj (str "sort=" sort))
                 cache (conj (str "cache=" cache)))]
    (if (seq params)
      (str base-url "?" (string/join "&" params))
      base-url)))

(defn- extract-addresses-from-efp-response
  "Extract addresses from EFP API response"
  [efp-response]
  (try
    (let [following-records (get efp-response "following" [])]
      (->> following-records
           (filter #(= (get % "record_type") "address")) ; Only address records
           (map #(get % "data"))                         ; Extract the address
           (filter some?)                                ; Remove nil values
           vec))
    (catch js/Error e
      (log/error "Failed to extract addresses from EFP response:" e)
      [])))

(defn- make-efp-api-call
  "Make HTTP call to EFP API and handle response"
  [api-url]
  (-> (js/fetch api-url)
      (.then (fn [response]
               (if (.-ok response)
                 (.json response)
                 (js/Promise.reject 
                  (js/Error. (str "HTTP " (.-status response) ": " (.-statusText response)))))))
      (.then (fn [json-response]
               (let [js-response (transforms/js->clj json-response)
                     addresses (extract-addresses-from-efp-response js-response)]
                 (log/info "EFP API: Successfully fetched" (count addresses) "following addresses")
                 (rf/dispatch [:efpfriends/following-success addresses]))))
      (.catch (fn [error]
                (log/error "EFP API: Failed to fetch following:" (.-message error))
                (rf/dispatch [:efpfriends/following-error 
                             {:message (.-message error)
                              :type :network-error}])))))

(rf/reg-fx
 :efp-api/get-following-with-options
 (fn [[user-address options]]
   (log/info "EFP API: Fetching following with options:" user-address options)
   (let [api-url (build-efp-following-url user-address options)]
     (log/debug "EFP API URL:" api-url)
     (make-efp-api-call api-url))))

(rf/reg-fx
 :efp-api/resolve-ens
 (fn [addresses]
   (log/info "EFP API: Resolving ENS for addresses:" (count addresses))
   
   ;; TODO: Implement actual ENS resolution in Phase 3
   ;; For now, just return addresses without ENS resolution
   (js/setTimeout
    (fn []
      (rf/dispatch [:efpfriends/ens-resolution-complete addresses]))
    800))) ; Simulate ENS resolution delay

;; Helper function to check if address is valid ETH address
(defn valid-eth-address?
  [address]
  (and (string? address)
       (= 42 (count address))
       (.startsWith address "0x")))

;; Future: Real EFP API integration will replace mock implementation
;; - HTTP calls to EFP endpoints
;; - Authentication with user's wallet
;; - Error handling for network failures
;; - Rate limiting and caching strategies
