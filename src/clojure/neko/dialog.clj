(ns neko.dialog
  "Declarative AlertDialog builder.

  Wraps android.app.AlertDialog.Builder with a simple map-based API.
  Uses listener factories from neko.listeners.dialog."
  (:require [neko.listeners.dialog :as dialog-listeners])
  (:import [android.app AlertDialog AlertDialog$Builder]
           android.content.Context))

(defn alert
  "Creates and shows an AlertDialog from a map of options.

  Options:
    :title              string — dialog title
    :message            string — dialog message body
    :positive-button    [\"label\" (fn [dialog which] ...)]
    :negative-button    [\"label\" (fn [dialog which] ...)]
    :neutral-button     [\"label\" (fn [dialog which] ...)]
    :items              [\"A\" \"B\" \"C\"] — list items (mutually exclusive with :message)
    :on-item-click      (fn [dialog position] ...) — list item click handler
    :cancelable         boolean (default true)
    :on-cancel          (fn [dialog] ...)
    :on-dismiss         (fn [dialog] ...)

  Returns the AlertDialog instance."
  ^AlertDialog [^Context context opts]
  (let [builder (AlertDialog$Builder. context)]
    (when-let [t (:title opts)]     (.setTitle builder ^CharSequence t))
    (when-let [m (:message opts)]   (.setMessage builder ^CharSequence m))
    (when-let [[label f] (:positive-button opts)]
      (.setPositiveButton builder ^CharSequence label
                          (dialog-listeners/on-click-call f)))
    (when-let [[label f] (:negative-button opts)]
      (.setNegativeButton builder ^CharSequence label
                          (dialog-listeners/on-click-call f)))
    (when-let [[label f] (:neutral-button opts)]
      (.setNeutralButton builder ^CharSequence label
                         (dialog-listeners/on-click-call f)))
    (when-let [items (:items opts)]
      (.setItems builder ^"[Ljava.lang.CharSequence;"
                 (into-array CharSequence items)
                 (when-let [f (:on-item-click opts)]
                   (dialog-listeners/on-click-call f))))
    (when (contains? opts :cancelable)
      (.setCancelable builder (boolean (:cancelable opts))))
    (let [dialog (.create builder)]
      (when-let [f (:on-cancel opts)]
        (.setOnCancelListener dialog (dialog-listeners/on-cancel-call f)))
      (when-let [f (:on-dismiss opts)]
        (.setOnDismissListener dialog (dialog-listeners/on-dismiss-call f)))
      (.show dialog)
      dialog)))
