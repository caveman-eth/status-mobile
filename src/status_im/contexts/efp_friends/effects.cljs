(ns status-im.contexts.efp-friends.effects
  (:require
    [re-frame.core :as rf]
    [taoensso.timbre :as log]))

;; EFP Friends Effects
;; Routes EFP API calls through status-go backend following Status patterns
;; API Documentation: https://ethidentitykit.com/docs/api/users/following

(rf/reg-fx
 :efp-api/get-following-with-options
 (fn [[user-address options]]
   (log/info "EFP API: Fetching following via status-go for address:" user-address "options:" options)
   ;; Route through status-go backend using json-rpc/call pattern
   ;; This follows Status pattern for external API calls
   {:fx [[:json-rpc/call
          [{:method     "efp_getFollowing"  ; status-go method (to be implemented)
            :params     [user-address options]
            :on-success [:efpfriends/following-success]
            :on-error   [:efpfriends/following-error]}]]]}))

(rf/reg-fx
 :efp-api/resolve-ens
 (fn [addresses]
   (log/info "EFP API: Resolving ENS via status-go for addresses:" (count addresses))
   ;; Use status-go ENS resolution following Status patterns
   ;; This will be implemented in Phase 3 using existing ens_addressOf method
   {:fx [[:json-rpc/call
          [{:method     "ens_batchResolve"  ; status-go method (to be implemented) 
            :params     [addresses]
            :on-success [:efpfriends/ens-resolution-complete]
            :on-error   [:efpfriends/ens-resolution-error]}]]]}))

;; Note: status-go Backend Implementation Required
;; The following methods need to be implemented in status-go:
;; 1. efp_getFollowing(address, options) -> calls EFP API
;; 2. ens_batchResolve(addresses) -> batch ENS resolution
;; 
;; This follows Status pattern where external APIs are proxied through status-go
;; for better error handling, caching, and security
