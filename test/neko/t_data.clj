(ns neko.t-data
  (:require [neko.data :as data]
            [neko.data.shared-prefs :as sp]
            [neko.intent :as intent]
            [clojure.test :refer :all])
  (:import org.robolectric.RuntimeEnvironment
           neko.App))

(deftest bundle-like-map
  (let [extras {:user "Joe" :age 37}
        intent (intent/intent "foo.MAIN" extras)]
    (is (= (:user extras) (:user (data/like-map intent))))
    (is (= extras (into {} (data/like-map intent))))
    (is (= extras (into {} (data/like-map (.getExtras intent)))))

    (is (= {} (into {} (data/like-map (intent/intent "bar.MAIN" {})))))
    (is (= {} (data/like-map nil)))))

(set! App/instance RuntimeEnvironment/application)

(deftest sp-like-map
  (let [sp (sp/get-shared-preferences "testprefs" :private)]
    (-> sp .edit
        (sp/put :name "Longcat")
        (sp/put :length-in-feet 10000)
        .commit)
    (is (= {:name "Longcat", :length-in-feet 10000} (into {} (data/like-map sp))))))

