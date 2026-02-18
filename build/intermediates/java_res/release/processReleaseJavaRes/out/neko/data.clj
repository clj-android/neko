(ns neko.data
  "Contains utilities to manipulate data that is passed between
  Android entities via Bundles and Intents."
  (:import android.os.Bundle android.content.Intent
           android.content.SharedPreferences
           neko.App))

;; This type acts as a wrapper around Bundle instance to be able to
;; access it like an ordinary map.
;;
(deftype MapLikeBundle [^Bundle bundle]
  clojure.lang.Associative
  (containsKey [this k]
    (.containsKey bundle (name k)))
  (entryAt [this k]
    (clojure.lang.MapEntry. k (.get bundle (name k))))
  (valAt [this k]
    (.get bundle (name k)))
  (valAt [this k default]
    (let [key (name k)]
      (if (.containsKey bundle key)
        (.get bundle (name key))
        default)))
  (seq [this]
    (map (fn [k] [(keyword k) (.get bundle k)])
         (.keySet bundle))))

;; This type wraps a HashMap just redirecting the calls to the
;; respective HashMap methods. The only useful thing it does is
;; allowing to use keyword keys instead of string ones.
;;
(deftype MapLikeHashMap [^java.util.HashMap hmap]
  clojure.lang.Associative
  (containsKey [this k]
    (.containsKey hmap (name k)))
  (entryAt [this k]
    (clojure.lang.MapEntry. k (.get hmap (name k))))
  (valAt [this k]
    (.get hmap (name k)))
  (valAt [this k default]
    (let [key (name k)]
      (if (.containsKey hmap key)
        (.get hmap (name key))
        default)))
  (seq [this]
    (map (fn [k] [(keyword k) (.get hmap k)])
         (.keySet hmap))))

(defprotocol MapLike
  "A protocol that helps to wrap objects of different types into
  MapLikeBundle."
  (like-map [this]))

(extend-protocol MapLike
  Bundle
  (like-map [b]
    (MapLikeBundle. b))

  Intent
  (like-map [i]
    (if-let [bundle (.getExtras i)]
      (MapLikeBundle. bundle)
      {}))

  SharedPreferences
  (like-map [sp]
    (MapLikeHashMap. (.getAll sp)))

  nil
  (like-map [_] {}))
