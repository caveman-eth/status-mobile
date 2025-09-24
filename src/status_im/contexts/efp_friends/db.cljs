(ns status-im.contexts.efp-friends.db)

;; EFP Friends Independent State Management
;; Following Status pattern: independent context state like [:contacts/contacts]

(def efp-friends-defaults
  {:loading?        false
   :last-updated    nil
   :cache-ttl       300000           ; 5 minutes TTL cache  
   :raw-addresses   []               ; Addresses from EFP API
   :enriched-list   []               ; ENS + metadata enriched friends
   :error          nil
   :user-address   nil})             ; Current user's ETH address

(defn efp-friends
  "Get EFP friends state from app-db"
  [db]
  (get db :efp-friends efp-friends-defaults))

(defn loading?
  "Check if EFP friends are currently loading"
  [db]
  (get-in db [:efp-friends :loading?] false))

(defn cache-expired?
  "Check if EFP friends cache has expired"
  [db]
  (let [{:keys [last-updated cache-ttl]} (efp-friends db)]
    (if (and last-updated cache-ttl)
      (> (- (js/Date.now) last-updated) cache-ttl)
      true))) ; No cache = expired

(defn enriched-friends
  "Get enriched friends list from app-db"
  [db]
  (get-in db [:efp-friends :enriched-list] []))

(defn has-friends?
  "Check if user has any EFP friends"
  [db]
  (seq (enriched-friends db)))
