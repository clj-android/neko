(ns neko.data.t-shared-prefs
  (:require [clojure.test :refer :all]
            [neko.data.shared-prefs :as sp])
  (:import [android.content SharedPreferences SharedPreferences$Editor]
           org.robolectric.RuntimeEnvironment
           neko.App))

(set! App/instance RuntimeEnvironment/application)

(deftest get-shared-preferences
  (is (instance? SharedPreferences (sp/get-shared-preferences "test" :private)))
  (is (instance? SharedPreferences (sp/get-shared-preferences "test" :world-writeable))))

(deftest edit-shared-preferences
  (let [prefs (sp/get-shared-preferences "test2" :private)]
    (-> (.edit prefs)
        (sp/put :foo "foo")
        (sp/put :bar 42)
        .commit)
    (is (= "foo" (.getString prefs "foo" "")))
    (is (= 42 (.getLong prefs "bar" -1)))))

(deftest defpreferences
  (sp/defpreferences sp-atom "test3")
  (is (instance? clojure.lang.Atom sp-atom))
  (is (= {} @sp-atom))
  (reset! sp-atom {:foo "foo" :bar 42})
  (let [prefs (sp/get-shared-preferences "test3" :private)]
    (is (= "foo" (.getString prefs "foo" "")))
    (is (= 42 (.getLong prefs "bar" -1))))
  (swap! sp-atom assoc :foo "Foo")
  (let [prefs (sp/get-shared-preferences "test3" :private)]
    (is (= "Foo" (.getString prefs "foo" ""))))
  (swap! sp-atom dissoc :bar)
  (let [prefs (sp/get-shared-preferences "test3" :private)]
    (is (= -1 (.getLong prefs "bar" -1))))
  (reset! sp-atom {})
  (let [prefs (sp/get-shared-preferences "test3" :private)]
    (is (empty? (.getAll prefs)))))
