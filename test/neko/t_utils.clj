(ns neko.t-utils
  (:require [clojure.test :refer :all]
            [neko.internal :as u]))

(deftest int-id
  (is (= (u/int-id :foo) (u/int-id :foo)))
  (is (not= (u/int-id :foo) (u/int-id :bar)))
  (every? pos? (map u/int-id [:foo :bar :baz ::qux :foo/bar :q.w.e.r])))

(deftest simple-name
  (is (= "Context" (u/simple-name 'android.context.Context)))
  (is (= "Activity" (u/simple-name 'Activity)))
  (is (= "App" (u/simple-name 'neko.App))))

(deftest capitalize
  (is (= "Foo" (u/capitalize "foo")))
  (is (= "OnCreate" (u/capitalize "onCreate"))))

(deftest unicaseize
  (is (= "onCreate" (u/unicaseize "OnCreate"))))

(deftest keyword->static-field
  (is (= "VERTICAL" (u/keyword->static-field :vertical)))
  (is (= "SCREEN_SIZE" (u/keyword->static-field :screen-size))))

(deftest keyword->camelcase
  (is (= "onClick" (u/keyword->camelcase :on-click)))
  (is (= "getPositiveButton" (u/keyword->camelcase :get-positive-button))))

(deftest keyword->setter
  (is (= "setOnClickListener" (u/keyword->setter :on-click-listener)))
  (is (= "setPositiveButton" (u/keyword->setter :positive-button))))

(deftest reflect-setter
  (is (instance? java.lang.reflect.Method (u/reflect-setter String "indexOf" Integer/TYPE)))
  (is (thrown? NoSuchMethodException (u/reflect-setter String "nonExisting" Integer/TYPE))))

(deftest call-if-nnil
  (let [f nil]
    (is (not (u/call-if-nnil f 1 2))))
  (let [f +]
    (is (u/call-if-nnil f 1 2))))

(u/memoized
 (defn plus "Adds two numbers"
   [x y]
   (Thread/sleep 500)
   (+ x y)))

(deftest memoized
  (let [a (System/currentTimeMillis)
        _ (is (= 5 (plus 2 3)))
        b (System/currentTimeMillis)
        _ (is (= 5 (plus 2 3)))
        c (System/currentTimeMillis)]
    (is (< 300 (- b a)))
    (is (> 100 (- c b)))))
