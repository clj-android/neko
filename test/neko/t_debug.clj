(ns neko.t-debug
  (:require [neko.debug :as dbg]
            [neko.activity :refer [defactivity]]
            [clojure.test :refer :all])
  (:import [org.robolectric Robolectric RuntimeEnvironment]
           org.robolectric.shadows.ShadowToast
           android.view.WindowManager$LayoutParams
           neko.App))

(deftest *a-and-keep-screen
  (defactivity neko.DebugActivity
    :key :test-debug
    (onCreate [this bundle]
      (.superOnCreate this bundle)
      (dbg/keep-screen-on this)))

  (let [activity (Robolectric/setupActivity neko.DebugActivity)]
    (is (= activity (dbg/*a)))
    (is (= activity (dbg/*a :test-debug)))
    (is (not= 0 (bit-and WindowManager$LayoutParams/FLAG_KEEP_SCREEN_ON
                         (.getForcedWindowFlags (.getWindow activity)))))))

(set! App/instance RuntimeEnvironment/application)

(deftest safe-for-ui
  (ShadowToast/reset)
  (is (thrown? ArithmeticException (/ 1 0)))
  (is (= 1 (dbg/safe-for-ui (/ 2 2))))
  (is (not (dbg/safe-for-ui (/ 1 0))))
  (is (= 1 (ShadowToast/shownToastCount)))
  (is (instance? ArithmeticException (dbg/ui-e))))

(deftest safe-for-ui*
  (ShadowToast/reset)
  (let [wrapped (dbg/safe-for-ui* (fn [] (/ 1 0)))]
    (is (function? wrapped))
    (is (= 0 (ShadowToast/shownToastCount))) ;; Not called yet
    (wrapped)
    (is (= 1 (ShadowToast/shownToastCount))) ;; Failed
    (is (instance? ArithmeticException (dbg/ui-e)))))
