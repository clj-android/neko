(ns neko.t-threading
  (:require [clojure.test :refer :all]
            [coa.droid-test :as dt]
            [neko.threading :as t])
  (:import android.view.View
           org.robolectric.RuntimeEnvironment
           neko.App))

(deftest ui-thread
  (is (t/on-ui-thread?))

  ;; Should execute immediately.
  (let [thread (Thread/currentThread)]
    (t/on-ui
      (is (= thread (Thread/currentThread)))))

  (future
    (is (not (t/on-ui-thread?)))
    (t/on-ui
      (is (t/on-ui-thread?))))

  (future
    (t/on-ui*
     (fn [] (is (t/on-ui-thread?))))))

(dt/deftest post
  (let [pr (promise)]
    (t/post (View. RuntimeEnvironment/application)
            (is (t/on-ui-thread?))
            (deliver pr :success))
    (is (= :success (deref pr 10000 :fail))))

  ;; Can't really test post-delayed from Robolectric
  (t/post-delayed (View. RuntimeEnvironment/application) 1000 nil))
