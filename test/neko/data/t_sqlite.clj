(ns neko.data.t-sqlite
  (:require [clojure.test :refer :all]
            [neko.data.sqlite :as db])
  (:import android.app.Activity
           org.robolectric.RuntimeEnvironment
           neko.App))

(set! App/instance RuntimeEnvironment/application)

(def schema
  (db/make-schema
   :name "test.db"
   :version 1
   :tables {:employees {:columns {:_id         "integer primary key"
                                  :name        "text not null"
                                  :vacation    "boolean"
                                  :certificate "blob"
                                  :boss_id     "integer"}}
            :bosses {:columns {:_id  "integer primary key"
                               :name "text not null"}}}))

(deftest sqlite
  (def helper (db/create-helper (Activity.) schema))
  (def db (db/get-database helper :write))

  (db/transact*
   db (fn []
        (db/insert db :employees {:name "Shelley Levene"
                                  :vacation false
                                  :certificate (.getBytes "quick brown fox")})
        (db/insert db :employees {:name "Dave Moss"
                                  :vacation false})
        (db/insert db :employees {:name "Ricky Roma"
                                  :vacation false})))

  (is (= () (db/query-seq db :employees {:vacation true})))

  (db/update db :employees {:vacation true}
             {:_id [:or 1 2]})

  (is (= [{:_id 1, :name "Shelley Levene", :vacation true}
          {:_id 2, :name "Dave Moss", :vacation true}]
         (map #(select-keys % [:_id :name :vacation])
              (db/query-seq db :employees {:vacation true}))))
  (is (= [{:name "Shelley Levene", :vacation true}
          {:name "Dave Moss", :vacation true}]
         (db/query-seq db [:name :vacation] :employees {:vacation true})))

  (db/transact db
    (let [willid (db/insert db :bosses {:name "John Williamson"})
          mmid (db/insert db :bosses {:name "Mitch and Murray"})]
      (db/update db :employees {:boss_id willid} {})
      (db/insert db :employees {:name "Blake", :boss_id mmid, :vacation false})))

  ;; For all employees not on vacation get their bosses.
  (is (= [{:employees/name "Ricky Roma", :bosses/name "John Williamson"}
          {:employees/name "Blake", :bosses/name "Mitch and Murray"}]
         (db/query-seq db [:employees/name :bosses/name]
                       [:employees :bosses]
                       {:employees/vacation false
                        :employees/boss_id :bosses/_id})))

  (is (= "Shelley Levene" (db/query-scalar db :name :employees {:_id 1})))
  (is (= 4 (db/query-scalar db ["count" :_id] :employees nil))))
