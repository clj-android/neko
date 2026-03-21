(ns neko.util
  "General-purpose utilities used across Neko.")

(defn edit-distance
  "Levenshtein distance between two strings."
  [^String a ^String b]
  (let [la (count a) lb (count b)
        prev (int-array (inc lb))
        curr (int-array (inc lb))]
    (dotimes [j (inc lb)] (aset prev j j))
    (dotimes [i la]
      (aset curr 0 (inc i))
      (dotimes [j lb]
        (let [cost (if (= (.charAt a i) (.charAt b j)) 0 1)]
          (aset curr (inc j)
                (min (inc (aget curr j))
                     (inc (aget prev (inc j)))
                     (+ (aget prev j) cost)))))
      (System/arraycopy curr 0 prev 0 (inc lb)))
    (aget prev lb)))
