(ns neko.t-log
  (:require [clojure.test :refer :all]
            [coa.droid-test :refer [unstub]]
            [neko.log :as log])
  (:import org.robolectric.shadows.ShadowLog
           android.util.Log))

(ShadowLog/setupLogging)

(defmacro capture-log [& body]
  `(let [stream# (java.io.ByteArrayOutputStream.)]
     (ShadowLog/reset)
     (set! ShadowLog/stream (java.io.PrintStream. stream#))
     ~@body
     (.close stream#)
     (str stream#)))

(deftest simple-logging-test
  (is (= (capture-log (log/d "message"))
         "D/neko.t-log: message\n"))
  (is (= (capture-log (log/e "message"))
         "E/neko.t-log: message\n"))
  (is (= (capture-log (log/i "message"))
         "I/neko.t-log: message\n"))
  (is (= (capture-log (log/v "message"))
         "V/neko.t-log: message\n"))
  (is (= (capture-log (log/w "message"))
         "W/neko.t-log: message\n")))

(deftest extra-options-test
  (is (= (capture-log (log/d "message" :tag "tag"))
         "D/tag: message\n"))

  (let [e (Exception.)]
    (is (.startsWith (capture-log (log/e "message" :tag "tag" :exception e))
                     "E/tag: message\n"))
    (is (= (.throwable (.get (ShadowLog/getLogs) 0)) e))))

(deftest string-concatenation-and-pprint
  (is (= (capture-log (log/i "quick" "brown" "fox" :tag "tag"))
         "I/tag: quick brown fox\n"))

  (is (= (capture-log (log/v "Lazy list is expanded:" (take 5 (range 100)) :tag "tag"))
         "V/tag: Lazy list is expanded: (0 1 2 3 4)\n")))
