(ns neko.listeners.rating-bar
  "Utility functions and macros for creating listeners corresponding to the
  android.widget.RatingBar class."
  (:require [neko.debug :refer [safe-for-ui]]))

(defn on-rating-bar-change-call
  "Takes a function and yields a RatingBar.OnRatingBarChangeListener object
  that will invoke the function.  This function must take the following three
  arguments:

  rating-bar  the RatingBar whose rating has changed
  rating      the current rating (float)
  from-user   true if the rating change was initiated by a user's touch gesture"
  ^android.widget.RatingBar$OnRatingBarChangeListener
  [handler-fn]
  (reify android.widget.RatingBar$OnRatingBarChangeListener
    (onRatingChanged [this rating-bar rating from-user]
      (safe-for-ui (handler-fn rating-bar rating from-user)))))

(defmacro on-rating-bar-change
  "Takes a body of expressions and yields a RatingBar.OnRatingBarChangeListener
  object that will invoke the body.  The body takes the following implicit
  arguments:

  rating-bar  the RatingBar whose rating has changed
  rating      the current rating (float)
  from-user?  true if the rating change was initiated by a user's touch gesture"
  [& body]
  `(on-rating-bar-change-call (fn [~'rating-bar ~'rating ~'from-user?] ~@body)))
