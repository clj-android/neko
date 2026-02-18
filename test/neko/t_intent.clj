(ns neko.t-intent
  (:require [clojure.test :refer :all]
            [neko.intent :as intent]
            neko.t-activity)
  (:import android.content.Intent
           android.os.Bundle
           org.robolectric.RuntimeEnvironment))

(deftest intent-creation
  (is (instance? Intent (intent/intent "foo.bar.MAIN" {})))
  (is (instance? Intent (intent/intent RuntimeEnvironment/application
                                       neko.DefActivity {:user "Joe"})))
  ;; Can't really test with the (app-package-name)
  (is (thrown? NullPointerException (intent/intent RuntimeEnvironment/application
                                                   '.DefActivity {:user "Joe"}))))

(deftest put-extras
  (let [i (intent/intent "foo.MAIN" {:user "Joe", :age 37, :gpa 4.5, :rank (int 3)
                                     :employed true :bundle (Bundle.)})]
    (is (instance? Bundle (.getExtras i)))
    (is (= "Joe" (.getString (.getExtras i) "user")))
    (is (= 37 (.getLong (.getExtras i) "age")))
    (is (= 4.5 (.getDouble (.getExtras i) "gpa")))
    (is (= 3 (.getInt (.getExtras i) "rank")))
    (is (= true (.getBoolean (.getExtras i) "employed")))
    (is (instance? Bundle (.getBundle (.getExtras i) "bundle")))))
