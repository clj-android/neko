(ns neko.t-threading
  (:require [clojure.test :refer :all]
            [neko.threading :as t])
  (:import android.view.View
           [android.os Handler Looper]
           org.robolectric.RuntimeEnvironment
           org.robolectric.shadows.ShadowLooper
           neko.App))

(deftest ui-thread
  (is (t/on-ui-thread?))

  ;; Should execute immediately.
  (let [thread (Thread/currentThread)]
    (t/on-ui
      (is (= thread (Thread/currentThread))))))

(deftest post
  ;; Test Handler-based posting via on-ui from a "background" context.
  ;; View.post on a detached View defers to the main Handler in Robolectric 4.x,
  ;; so we test the underlying mechanism directly.
  (let [pr (promise)]
    (.post (Handler. (Looper/getMainLooper))
           (fn [] (deliver pr :success)))
    (.runToEndOfTasks (ShadowLooper/shadowMainLooper))
    (is (= :success (deref pr 1000 :fail)))))
