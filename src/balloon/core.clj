(ns balloon.core
  (:require [clojure.string :as string]))

;;
;; deflate
;;

(declare deflate-map)

(defn- index->key [index]
  (keyword (str index)))

(defn- keys->deflated-key
  [delimiter]
  (fn [ks]
    (->> (for [k ks] (if (number? k) (str k) (name k)))
         (string/join delimiter)
         (keyword))))

(defn- deflate-map [convert inflated-map opts]
  (let [dfmap (fn dfmap [m ks]
                (reduce
                 (fn [acc [k v]]
                   (cond
                     (and (map? v)
                          (not-empty v))
                     (merge acc (dfmap v (conj ks k)))

                     (and (sequential? v)
                          (not-empty v)
                          (not (:keep-coll opts)))
                     (merge acc (let [new-ks  (conj ks k)
                                        indexed (map-indexed
                                                 (fn [index item] [index item])
                                                 v)]
                                    (reduce
                                     (fn [acc [index v]]
                                       (let [new-k (index->key index)]
                                         (if (map? v)
                                           (merge acc (dfmap v (conj new-ks new-k)))
                                           (merge acc (dfmap {new-k v} new-ks)))))
                                     {}
                                     indexed)))

                     :else
                     (assoc acc (convert (conj ks k)) v)))
                 {}
                 m))]
    (dfmap inflated-map [])))

(defn deflate
  "Flats a nested map to one level deep.

  Options are key-value pairs and may be one or many of:
  :delimiter - Use different delimiter to build delimited keys, defaults to .
  :keep-coll - Do not flat collections (lists or vectors), defaults to false"
  [m & {:keys [delimiter keep-coll]
        :or {delimiter "."
             keep-coll false}}]
  {:pre [(map? m)]}
  (deflate-map (keys->deflated-key delimiter) m {:keep-coll keep-coll}))

;;
;; inflate
;;

(defn- deflated-key? [delimiter]
  (fn [k]
    (string/includes? k delimiter)))

(defn- parseInt? [s]
  (try
    (Integer/parseInt s)
    true
    (catch Exception e
      false)))

(defn- deflated-key->path [delimiter]
  (fn [k]
    (vec (map
          (fn [s]
            (if (parseInt? s)
              (Integer/parseInt s)
              (keyword s)))
          (string/split (name k)
                        (re-pattern (java.util.regex.Pattern/quote delimiter)))))))

(defn- assoc-inth
  "Like assoc-in but conjoins value to a vector if key in path is a number.
  Eg:
  (assoc-in {} [:a 0] 1) => {:a {0 1}}
  (assoc-inth {} [:a 0] 1) => {:a [1]}"
  [form path v]
  (let [result (reduce
                (fn [{:keys [end-path end-form] :as acc} k]
                  (if (keyword? k)
                    (update acc :end-path conj k)
                    (if (and (nil? (get-in end-form end-path))
                             (number? k))
                      {:end-path (conj end-path k)
                       :end-form (assoc-in end-form end-path (if (> k 0)
                                                               (vec (repeat k nil))
                                                               []))}
                      (update acc :end-path conj k))))
                {:end-path []
                 :end-form form}
                path)]
    (assoc-in (:end-form result) (:end-path result) v)))

(defn inflate
  "Unflats a one level deep flat map to a nested one.

  Options are key-value pairs and may be one or many of:
  :delimiter   - Use different delimiter to unflat the hash-map delimited keys, defaults to '.'
  :hash-map    - Unflat indexes/numbers in delimited keys as hash-maps, not as collections, defaults to false"
  [m & {:keys [delimiter hash-map]
        :or {delimiter "."
             hash-map false}}]
  {:pre [(map? m)]}
  (let [deflated? (deflated-key? delimiter)
        convert   (deflated-key->path delimiter)
        assoc-x   (if hash-map assoc-in assoc-inth)
        inf-recur (fn inf-recur [ks m]
                    (if (empty? ks)
                      m
                      (let [k (first ks)
                            v (get m k)]
                        (cond
                          (map? v)
                          (inf-recur (rest ks)
                                     (if (deflated? k)
                                       (assoc-x (dissoc m k)
                                                (convert k)
                                                (inf-recur (keys v) v))
                                       (assoc m k (inf-recur (keys v) v))))

                          (sequential? v)
                          (let [map-fn (fn map-fn [sv]
                                         (cond
                                           (map? sv)
                                           (inf-recur (keys sv) sv)

                                           (sequential? sv)
                                           (vec (map map-fn sv))

                                           :else sv))]
                            (inf-recur (rest ks)
                                       (if (deflated? k)
                                         (assoc-x (dissoc m k)
                                                  (convert k)
                                                  (vec (map map-fn v)))
                                         (assoc m k (vec (map map-fn v))))))

                          :else
                          (inf-recur (rest ks)
                                     (if (deflated? k)
                                       (assoc-x (dissoc m k)
                                                (convert k)
                                                v)
                                       m))))))]
    (inf-recur (keys m) m)))
