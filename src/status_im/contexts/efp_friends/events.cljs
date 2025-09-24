(ns status-im.contexts.efp-friends.events
  (:require
    [status-im.contexts.efp-friends.db :as efp-db]
    [taoensso.timbre :as log]
    [utils.re-frame :as rf]))

;; EFP Friends Events
;; Following Status Re-frame patterns for event-driven state management

(rf/reg-event-fx
 :efpfriends/initialize
 (fn [{:keys [db]}]
   {:db (assoc db :efp-friends efp-db/efp-friends-defaults)}))

(rf/reg-event-fx
 :efpfriends/fetch-following
 (fn [{:keys [db]} [user-address & [{:keys [limit offset fresh?] :as options}]]]
   (log/info "Fetching EFP following for address:" user-address "options:" options)
   (let [api-options (cond-> {:limit (or limit 50) :sort "followers"}
                       offset (assoc :offset offset)
                       fresh? (assoc :cache "fresh"))]
     {:db (-> db
              (assoc-in [:efp-friends :loading?] true)
              (assoc-in [:efp-friends :user-address] user-address)
              (assoc-in [:efp-friends :error] nil))
      :fx [[:efp-api/get-following-with-options [user-address api-options]]]})))

(rf/reg-event-fx
 :efpfriends/following-success
 (fn [{:keys [db]} [addresses]]
   (log/info "EFP following fetch successful, count:" (count addresses))
   {:db (-> db
            (assoc-in [:efp-friends :loading?] false)
            (assoc-in [:efp-friends :raw-addresses] addresses)
            (assoc-in [:efp-friends :last-updated] (js/Date.now)))
    :fx [[:dispatch [:efpfriends/enrich-addresses addresses]]]}))

(rf/reg-event-fx
 :efpfriends/following-error
 (fn [{:keys [db]} [error]]
   (log/error "EFP following fetch failed:" error)
   {:db (-> db
            (assoc-in [:efp-friends :loading?] false)
            (assoc-in [:efp-friends :error] error))}))

(rf/reg-event-fx
 :efpfriends/enrich-addresses
 (fn [{:keys [db]} [addresses]]
   (log/info "Enriching EFP addresses with ENS data, count:" (count addresses))
   {:fx [[:dispatch [:efpfriends/batch-resolve-ens addresses]]]}))

(rf/reg-event-fx
 :efpfriends/batch-resolve-ens
 (fn [{:keys [db]} [addresses]]
   ;; TODO: Implement ENS batch resolution in Phase 3
   ;; For now, create basic friend objects without ENS
   (let [basic-friends (map (fn [address]
                             {:address             address
                              :ens-name           nil
                              :display-name       (str (subs address 0 6) "..." (subs address -4))
                              :profile-photo      nil
                              :customization-color :blue
                              :recipient-type     :efp-friend}) 
                           addresses)]
     {:db (assoc-in db [:efp-friends :enriched-list] basic-friends)})))

(rf/reg-event-fx
 :efpfriends/clear-cache
 (fn [{:keys [db]}]
   {:db (-> db
            (assoc-in [:efp-friends :raw-addresses] [])
            (assoc-in [:efp-friends :enriched-list] [])
            (assoc-in [:efp-friends :last-updated] nil)
            (assoc-in [:efp-friends :error] nil))}))

(rf/reg-event-fx
 :efpfriends/refresh-if-needed
 (fn [{:keys [db]}]
   (when (efp-db/cache-expired? db)
     (let [user-address (get-in db [:efp-friends :user-address])]
       (when user-address
         {:fx [[:dispatch [:efpfriends/fetch-following user-address]]]})))))

(rf/reg-event-fx
 :efpfriends/force-refresh
 (fn [{:keys [db]}]
   (let [user-address (get-in db [:efp-friends :user-address])]
     (when user-address
       {:fx [[:dispatch [:efpfriends/fetch-following user-address {:fresh? true}]]]}))))

(rf/reg-event-fx
 :efpfriends/ens-resolution-complete
 (fn [{:keys [db]} [addresses]]
   (log/info "ENS resolution complete for addresses:" (count addresses))
   ;; TODO: Implement ENS resolution completion in Phase 3
   ;; For now, just log completion
   {:db db}))
