(ns neko.t-notify
  (:require [clojure.test :refer :all]
            [neko.context :as ctx]
            [neko.notify :as notify]
            [neko.internal :as u])
  (:import android.app.Activity
           org.robolectric.Shadows
           org.robolectric.shadows.ShadowToast
           neko.App))

(deftest disguised-toast
  (ShadowToast/reset)
  (is (= 0 (ShadowToast/shownToastCount)))
  (notify/toast "Disguised toast" :short)
  (notify/toast App/instance "Disguised toast" :long)
  (notify/toast "Disguised toast" :long)
  (notify/toast (Activity.) "Disguised toast" :long)
  (is (= 4 (ShadowToast/shownToastCount))))

(deftest notifications
  (let [nm (ctx/get-service :notification)
        n (notify/notification {:content-title "Title"
                                :content-text "Text"
                                :action [:activity "foo.bar.MAIN"]})]
    (notify/fire ::test n)
    (is (= n (.getNotification (Shadows/shadowOf nm) nil (u/int-id ::test))))
    (notify/cancel ::test)
    (is (= nil (.getNotification (Shadows/shadowOf nm) nil (u/int-id ::test))))))
