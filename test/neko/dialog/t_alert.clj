(ns neko.dialog.t-alert
  (:require [neko.dialog.alert :as dlg]
            [neko.activity :refer [defactivity]]
            [neko.listeners.dialog :as listeners]
            neko.t-activity
            [coa.droid-test :refer [deftest]]
            [clojure.test :refer :all :exclude [deftest]])
  (:import android.app.AlertDialog
           android.content.DialogInterface
           [org.robolectric Robolectric RuntimeEnvironment]))

(deftype OnClick [callback]
  android.content.DialogInterface$OnClickListener
  (onClick [this dialog which]
    (callback dialog which)))

(deftest alert-dialog-builder
  (let [;; Need to redef a listener, otherwise Cloverage freaks out.
        dialog (with-redefs [listeners/on-click-call (fn [x] (OnClick. x))]
                 (-> (dlg/alert-dialog-builder
                      RuntimeEnvironment/application
                      {:message "Dialog message"
                       :cancelable true
                       :positive-text "OK"
                       :positive-callback (fn [dialog res] (is true))
                       :negative-text "Cancel"
                       :negative-callback (fn [dialog res] (is true) (.cancel dialog))
                       :neutral-text "Maybe"
                       :neutral-callback (fn [dialog res] (is true))})
                     .create))]
    (is (instance? AlertDialog dialog))
    (.show dialog)
    (is (.isShowing dialog))
    (.performClick (.getButton dialog DialogInterface/BUTTON_POSITIVE))
    (.performClick (.getButton dialog DialogInterface/BUTTON_NEUTRAL))
    (.performClick (.getButton dialog DialogInterface/BUTTON_NEGATIVE))
    (is (not (.isShowing dialog)))))
