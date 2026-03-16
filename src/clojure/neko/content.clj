(ns neko.content
  "Utilities for working with Android's ContentResolver."
  (:import android.content.Context
           android.database.Cursor
           android.net.Uri
           android.provider.OpenableColumns))

(defn query-uri
  "Queries a content URI and returns a vector of maps, one per cursor row.

  Options:
    :columns — a string array of projection columns, or nil for all (default).

  Each map key is the column name as a keyword."
  [^Context context ^Uri uri & {:keys [columns]}]
  (let [resolver (.getContentResolver context)
        ^"[Ljava.lang.String;" proj (when columns (into-array String columns))
        ^Cursor cursor (.query resolver uri proj nil nil nil)]
    (try
      (when cursor
        (let [col-count (.getColumnCount cursor)
              col-names (mapv #(.getColumnName cursor %) (range col-count))]
          (loop [results []]
            (if (.moveToNext cursor)
              (recur (conj results
                          (into {}
                                (for [i (range col-count)]
                                  [(keyword (col-names i))
                                   (case (.getType cursor i)
                                     (0)   nil                    ; FIELD_TYPE_NULL
                                     (1)   (.getLong cursor i)    ; FIELD_TYPE_INTEGER
                                     (2)   (.getFloat cursor i)   ; FIELD_TYPE_FLOAT
                                     (3)   (.getString cursor i)  ; FIELD_TYPE_STRING
                                     (4)   (.getBlob cursor i)    ; FIELD_TYPE_BLOB
                                     (.getString cursor i))]))))
              results))))
      (finally
        (when cursor (.close cursor))))))

(defn query-uri-single
  "Like query-uri but returns only the first row (or nil)."
  [^Context context ^Uri uri & {:as opts}]
  (first (apply query-uri context uri (mapcat identity opts))))

(defn get-mime-type
  "Returns the MIME type string for a content URI."
  [^Context context ^Uri uri]
  (.getType (.getContentResolver context) uri))

(defn get-openable-metadata
  "Returns a map with :name, :size, and :mime for a content URI.

  Uses OpenableColumns (DISPLAY_NAME, SIZE) and ContentResolver.getType."
  [^Context context ^Uri uri]
  (let [mime (get-mime-type context uri)
        resolver (.getContentResolver context)
        ^Cursor cursor (.query resolver uri nil nil nil nil)]
    (try
      (when (and cursor (.moveToFirst cursor))
        (let [name-idx (.getColumnIndex cursor OpenableColumns/DISPLAY_NAME)
              size-idx (.getColumnIndex cursor OpenableColumns/SIZE)]
          {:name (when (>= name-idx 0) (.getString cursor name-idx))
           :size (when (>= size-idx 0) (.getLong cursor size-idx))
           :mime mime}))
      (finally
        (when cursor (.close cursor))))))
