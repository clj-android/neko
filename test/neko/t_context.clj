(ns neko.t-context
  (:require [clojure.test :refer :all]
            [neko.context :as context])
  (:import [android.app AlarmManager NotificationManager Activity]
           org.robolectric.RuntimeEnvironment
           neko.App))

(set! App/instance RuntimeEnvironment/application)

(deftest sanity-check
  (is (= (.getApplication (Activity.))
         RuntimeEnvironment/application))
  (is (= (.getApplicationContext (.getApplication (Activity.)))
         RuntimeEnvironment/application)))

(deftest get-service
  (is (instance? NotificationManager (context/get-service :notification)))
  (is (instance? AlarmManager
                 (context/get-service RuntimeEnvironment/application :alarm))))

