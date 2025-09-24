(ns status-im.contexts.efp-friends.view
  (:require
    [quo.context :as quo.context]
    [quo.core :as quo]
    [react-native.core :as rn]
    [status-im.common.resources :as resources]
    [status-im.contexts.wallet.send.select-address.tabs.style :as style]
    [utils.i18n :as i18n]
    [utils.re-frame :as rf]))

;; EFP Friends Tab Component
;; Following Status UI patterns with quo components and empty states

(defn efp-friend-item
  "Individual EFP friend item using quo/saved-address component"
  [{:keys [address ens-name display-name profile-photo customization-color] :as friend}]
  [quo/saved-address
   {:user-props      {:name                (or ens-name display-name)
                      :address             address
                      :ens                 ens-name
                      :customization-color customization-color
                      :profile-picture     profile-photo}
    :container-style {:margin-horizontal 8}
    :on-press        (fn []
                       (rf/dispatch [:wallet/select-send-address
                                     {:address   address
                                      :recipient (assoc friend :recipient-type :efp-friend)
                                      :stack-id  :screen/wallet.select-address}]))}])

(defn efp-friends-list
  "List of EFP friends"
  [friends]
  [rn/flat-list
   {:data                         friends
    :render-fn                    efp-friend-item
    :key-fn                       :address
    :content-container-style      {:padding-bottom 20}
    :shows-vertical-scroll-indicator false
    :keyboard-should-persist-taps :handled}])

(defn loading-state
  "Skeleton loading state"
  []
  [quo/skeleton-list
   {:content       :messages
    :parent-height 400
    :animated?     false}])

(defn error-state
  "Error state when EFP API fails"
  [theme error]
  (let [error-title (case (:type error)
                      :network-error "Network Error"
                      "EFP Connection Error")
        error-desc (if (:message error)
                     (:message error)
                     "Unable to load EFP friends. Please check your connection and try again.")]
    [quo/empty-state
     {:title           error-title
      :description     error-desc
      :image           (resources/get-themed-image :no-network theme)
      :container-style style/empty-container-style
      :button          {:text "Retry"
                        :on-press #(rf/dispatch [:efpfriends/force-refresh])}}]))

(defn empty-state
  "Empty state when user has no EFP friends"
  [theme]
  [quo/empty-state
   {:title           (i18n/label :t/no-efp-friends)
    :description     (i18n/label :t/efp-follow-addresses-to-see-here)
    :image           (resources/get-themed-image :no-contacts theme)
    :container-style style/empty-container-style}])

(defn view
  "Main EFP friends tab view"
  [theme]
  (let [loading?       (rf/sub [:efpfriends/loading?])
        friends-list   (rf/sub [:efpfriends/enriched-list])
        error          (rf/sub [:efpfriends/error])
        has-friends?   (rf/sub [:efpfriends/has-friends?])]
    
    (cond
      loading?
      [loading-state]
      
      error  
      [error-state theme error]
                        
      (not has-friends?)
      [empty-state theme]
      
      :else
      [efp-friends-list friends-list])))
