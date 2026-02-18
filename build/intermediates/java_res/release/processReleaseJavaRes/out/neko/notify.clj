(ns neko.notify
  "Provides convenient wrappers for Toast and Notification APIs."
  (:require [neko.internal :refer [int-id]])
  (:import [android.app Notification Notification$Builder
            NotificationChannel NotificationManager PendingIntent]
           [android.content Context Intent]
           android.widget.Toast
           neko.App))

;; ### Toasts

(defn toast
  "Creates a Toast object using a text message and a keyword representing how
  long a toast should be visible (`:short` or `:long`). If length is not
  provided, it defaults to :long."
  ([message]
   (toast App/instance message :long))
  ([message length]
   (toast App/instance message length))
  ([^Context context, ^String message, length]
   {:pre [(or (= length :short) (= length :long))]}
   (.show
    ^Toast (Toast/makeText context message ^int (case length
                                                  :short Toast/LENGTH_SHORT
                                                  :long Toast/LENGTH_LONG)))))

;; ### Notifications

(def ^:private default-channel-id "neko_default")

(defn- ^NotificationManager notification-manager
  "Returns the notification manager instance."
  ([^Context context]
   (.getSystemService context Context/NOTIFICATION_SERVICE)))

(defn- ensure-channel
  "Ensures the default notification channel exists (required since API 26)."
  [^Context context ^String channel-id]
  (let [^NotificationManager nm (notification-manager context)
        channel (NotificationChannel.
                  channel-id "Default" NotificationManager/IMPORTANCE_DEFAULT)]
    (.createNotificationChannel nm channel)))

(defn construct-pending-intent
  "Creates a PendingIntent instance from a vector where the first
  element is a keyword representing the action type, and the second
  element is a action string to create an Intent from."
  ([context [action-type, ^String action]]
     (let [^Intent intent (Intent. action)
           flags PendingIntent/FLAG_IMMUTABLE]
       (case action-type
         :activity (PendingIntent/getActivity context 0 intent flags)
         :broadcast (PendingIntent/getBroadcast context 0 intent flags)
         :service (PendingIntent/getService context 0 intent flags)))))

(defn notification
  "Creates a Notification instance using Notification.Builder."
  ([options]
   (notification App/instance options))
  ([context {:keys [icon ticker-text content-title content-text action channel-id]
             :or {icon android.R$drawable/ic_dialog_info
                  channel-id default-channel-id}}]
   (ensure-channel context channel-id)
   (let [builder (doto (Notification$Builder. context channel-id)
                   (.setSmallIcon (int icon))
                   (.setContentTitle content-title)
                   (.setContentText content-text))]
     (when ticker-text
       (.setTicker builder ticker-text))
     (when action
       (.setContentIntent builder (construct-pending-intent context action)))
     (.build builder))))

(defn fire
  "Sends the notification to the status bar. ID can be an integer or a keyword."
  ([id notification]
   (fire App/instance id notification))
  ([context id notification]
   (let [id (if (keyword? id)
              (int-id id)
              id)]
     (.notify (notification-manager context) id notification))))

(defn cancel
  "Removes a notification by the given ID from the status bar."
  ([id]
   (cancel App/instance id))
  ([context id]
   (.cancel (notification-manager context) (if (keyword? id)
                                             (int-id id)
                                             id))))
