(ns neko.ui.t-adapters
  (:require [neko.ui.adapters :refer :all]
            [neko.ui :refer [config]]
            [neko.data.sqlite :as db]
            [clojure.test :refer :all])
  (:import [neko.ui.adapters InterchangeableListAdapter TaggedCursorAdapter]
           [android.widget ListView TextView]
           neko.App))

(deftest ref-adapter-tests
  (testing "simple adapter"
    (let [ref (atom ["one" "two" "three"])
          adapter (ref-adapter (fn [ctx]
                                 [:text-view {}])
                               (fn [pos v _ data]
                                 (config v :text data))
                               ref)]
      (instance? InterchangeableListAdapter adapter)
      (is (= 3 (.getCount adapter)))
      (let [item (.getView adapter 0 nil (ListView. App/instance))]
        (is (instance? TextView item))
        (is (= "one" (.getText item))))
      (swap! ref conj "four")
      (let [new-item (.getView adapter 3 nil (ListView. App/instance))]
        (is (instance? TextView new-item))
        (is (= "four" (.getText new-item))))))

  (testing "ref access-fn"
    (let [ref (atom {:items ["one" "two" "three"] :extra 42})
          adapter (ref-adapter (fn [ctx]
                                 [:text-view {}])
                               (fn [pos v _ data]
                                 (config v :text data))
                               ref
                               :items)]
      (is (= 3 (.getCount adapter)))
      (swap! ref update-in [:items] conj "four")
      (is (= 4 (.getCount adapter)))))

  (testing "doesn't die on exceptions"
    (let [ref (atom {:items ["one" "two" "three"] :extra 42})
          adapter (ref-adapter (fn [_] (/ 1 0)) (constantly nil)
                               ref :items)]
      (is (= 3 (.getCount adapter)))
      (let [item (.getView adapter 0 nil (ListView. App/instance))]
        ;; Should return a dummy view
        (is (instance? android.view.View item))))
    (let [ref (atom {:items ["one" "two" "three"] :extra 42})
          adapter (ref-adapter (constantly (TextView. App/instance))
                               (fn [_ _ _ _] (/ 1 0)) ref :items)]
      (is (= 3 (.getCount adapter)))
      (let [item (.getView adapter 0 nil (ListView. App/instance))]
        ;; Item test shouldn't change
        (is (= "" (.getText item))))))

  (testing "wrong inputs"
    (is (thrown? AssertionError (ref-adapter nil nil nil nil)))
    (is (thrown? AssertionError (ref-adapter (fn []) nil nil nil)))
    (is (thrown? AssertionError (ref-adapter (fn []) (fn []) [1 2 3] nil)))
    (is (thrown? AssertionError (ref-adapter (fn []) (fn []) (atom []) 42)))))

(deftest cursor-adapter-tests
  (let [schema (db/make-schema
                :name "adapters_test.db"
                :version 1
                :tables {:numbers {:columns {:_id "integer primary key"
                                             :name "text not null"}}})
        helper (db/create-helper App/instance schema)
        db (db/get-database helper :write)
        get-view (fn [adapter] (let [cursor (.getCursor adapter)
                                    v (.newView adapter App/instance cursor
                                                (ListView. App/instance))]
                                (.bindView adapter v App/instance cursor)
                                v))]
    (db/transact
      db (doall (map #(db/insert db :numbers {:name %})
                     ["one" "two" "three"])))
    (testing "explicit cursor"
      (let [cursor (db/query db :numbers {})
            adapter (cursor-adapter App/instance
                                    (fn [] [:text-view {}])
                                    (fn [v _ data] (config v :text (:name data)))
                                    cursor)]

        (instance? TaggedCursorAdapter adapter)
        (is (= 3 (.getCount adapter)))
        (.moveToFirst cursor)
        (let [item (get-view adapter)]
          (is (instance? TextView item))
          (is (= "one" (.getText item))))

        (db/insert db :numbers {:name "four"})
        (is (= 3 (.getCount adapter))) ;; Not changed because we haven't updated

        (update-cursor adapter (db/query db :numbers {}))
        (is (= 4 (.getCount adapter)))
        (.moveToFirst (.getCursor adapter))
        (dotimes [i 3] (.moveToNext (.getCursor adapter)))
        (let [new-item (get-view adapter)]
          (is (instance? TextView new-item))
          (is (= "four" (.getText new-item))))))

    (testing "cursor-fn"
      (let [adapter (cursor-adapter App/instance
                                    (fn [] [:text-view {}])
                                    (fn [v _ data] (config v :text (str data)))
                                    (fn [] (db/query db :numbers {})))]
        (is (= 4 (.getCount adapter)))
        (db/insert db :numbers {:name "four"})
        (is (= 4 (.getCount adapter))) ;; Not changed because we haven't updated
        (update-cursor adapter)
        (is (= 5 (.getCount adapter)))))

    (testing "doesn't die on exceptions"
      (let [adapter (cursor-adapter App/instance (fn [] (/ 1 0))
                                    (constantly nil)
                                    (db/query db :numbers {}))]
        (is (= 5 (.getCount adapter)))
        (.moveToFirst (.getCursor adapter))
        (is (instance? android.view.View (get-view adapter))))

      (let [adapter (cursor-adapter App/instance (fn [] (TextView. App/instance))
                                    (fn [_ _ _] (/ 1 0))
                                    (db/query db :numbers {}))]
        (is (= 5 (.getCount adapter)))
        ;; Item test shouldn't change
        (.moveToFirst (.getCursor adapter))
        (is (= "" (.getText (get-view adapter))))))))
