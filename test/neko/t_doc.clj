(ns neko.t-doc
  (:require [neko.doc :as doc]
            [clojure.test :refer :all])
  (:import java.io.StringWriter))

(defmacro capture-out [& body]
  `(let [out# (StringWriter.)]
     (binding [*out* out#]
       ~@body)
     (str out#)))

(deftest describe
  (is (instance? String (capture-out (doc/describe))))
  (is (re-matches #"(?s)^\nTraits found:\n :on-click.+" (capture-out (doc/describe :on-click))))
  (is (re-matches #"(?s)^Elements found:\n :edit-text.+" (capture-out (doc/describe :edit-text))))
  (is (re-matches #"^No elements or traits.+" (capture-out (doc/describe :nothing))))
  (is (< 2000 (count (capture-out (doc/describe :text-view :verbose))))))
