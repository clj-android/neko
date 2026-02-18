(ns neko.listeners.t-adapter-view
  (:require [clojure.test :refer :all :exclude [deftest]]
            [neko.listeners.adapter-view :as l]
            [coa.droid-test :refer [deftest]])
  (:import android.widget.ListView
           org.robolectric.RuntimeEnvironment))

(defmacro test-listener [& body]
  `(let [~'v (ListView. RuntimeEnvironment/application)
         ~'called (fn [] (is true))]
     ~@body))

(deftest on-item-click
  (test-listener
   (.onItemClick (l/on-item-click (is (= position 3))
                                  (is (= id 20)))
                 v v 3 20))

  (test-listener
   (.onItemClick (l/on-item-click-call (fn [_ __ pos id]
                                         (is (= pos 3))
                                         (is (= id 20))))
                 v v 3 20)))

(deftest on-item-long-click
  (test-listener
   (.onItemLongClick (l/on-item-long-click (is (= position 3))
                                           (is (= id 20)))
                     v v 3 20))

  (test-listener
   (.onItemLongClick (l/on-item-long-click-call (fn [_ __ pos id]
                                                  (is (= pos 3))
                                                  (is (= id 20))))
                     v v 3 20)))

(deftest on-item-selected
  (test-listener
   (.onItemSelected (l/on-item-selected (is (= position 3))
                                        (is (= id 20)))
                    v v 3 20))

  (test-listener
   (.onItemSelected (l/on-item-selected-call (fn [_ __ pos id]
                                               (is (= pos 3))
                                               (is (= id 20))))
                    v v 3 20)))
