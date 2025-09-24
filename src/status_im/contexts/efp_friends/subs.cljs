(ns status-im.contexts.efp-friends.subs
  (:require
    [re-frame.core :as rf]
    [status-im.contexts.efp-friends.db :as efp-db]))

;; EFP Friends Subscriptions  
;; Following Status Re-frame patterns for reactive data access

(rf/reg-sub
 :efpfriends/state
 (fn [db]
   (efp-db/efp-friends db)))

(rf/reg-sub
 :efpfriends/loading?
 :<- [:efpfriends/state]
 (fn [efp-friends-state]
   (:loading? efp-friends-state)))

(rf/reg-sub
 :efpfriends/enriched-list
 :<- [:efpfriends/state]
 (fn [efp-friends-state]
   (:enriched-list efp-friends-state)))

(rf/reg-sub
 :efpfriends/error
 :<- [:efpfriends/state]
 (fn [efp-friends-state]
   (:error efp-friends-state)))

(rf/reg-sub
 :efpfriends/has-friends?
 :<- [:efpfriends/enriched-list]
 (fn [enriched-list]
   (seq enriched-list)))

(rf/reg-sub
 :efpfriends/cache-expired?
 (fn [db]
   (efp-db/cache-expired? db)))

(rf/reg-sub
 :efpfriends/user-address
 :<- [:efpfriends/state]
 (fn [efp-friends-state]
   (:user-address efp-friends-state)))

(rf/reg-sub
 :efpfriends/last-updated
 :<- [:efpfriends/state]
 (fn [efp-friends-state]
   (:last-updated efp-friends-state)))
