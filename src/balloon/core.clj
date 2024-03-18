(ns balloon.core
  (:require [clojure.string :as string]))

;;
;; deflate
;;

(declare deflate-map)

(defn- index->key [index]
  (keyword (str index)))

(defn- keys->deflated-key [delimiter]
  (fn [ks]
    (keyword (string/join delimiter (map (fn [k]
                                           (if (number? k)
                                             (str k)
                                             (name k)))
                                         ks)))))

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

(defn- path-in-map? [path m]
  (not (= 'not-found (get-in m path 'not-found))))

(defn- path-in-map [path m]
  (let [result (reduce (fn [{:keys [end-path value] :as acc} k]
                         (let [ks (conj end-path k)]
                           (if (path-in-map? ks m)
                             {:end-path ks
                              :value (get-in m ks)}
                             (reduced acc))))
                       {:end-path []
                        :value {}}
                       path)]
    [(:end-path result) (:value result)]))

(defn- assoc-inth [form path v]
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
  :delimiter   - Use different delimiter to unflat the hash-map delimited keys, defaults to .
  :pre-deflate - Run deflate on hash-map to guarantee is fully normalized before running unflat process, defaults to true
  :hash-map    - Unflat indexes in delimited keys as hash-map, not as a collection, defaults to false"
  [m & {:keys [delimiter pre-deflate hash-map]
        :or {delimiter "."
             pre-deflate true
             hash-map false}}]
  {:pre [(map? m)]}
  (let [dm        (if pre-deflate (deflate m :delimiter delimiter) m)
        deflated? (deflated-key? delimiter)
        convert   (deflated-key->path delimiter)
        assoc-x   (if hash-map assoc-in assoc-inth)]
    (reduce
     (fn [acc [k v]]
       (if (deflated? k)
         (let [path        (convert k)
               [path-found
                val-found] (path-in-map path acc)]
           (if (and (seq path-found)
                    (not= path-found path)
                    (map? val-found))
             (let [rest-path (subvec path (count path-found))]
               (assoc-in acc path-found (merge-with into val-found (assoc-x {} rest-path v))))
             (assoc-x acc path v)))
         (assoc acc k v)))
     {}
     dm)))

(defn construct-recur [m ks v]
  (if (= (count ks) 1)
    (assoc m (first ks) v)
    (let [k  (first ks)
          mv (get m k)]
      (if (map? mv)
        (assoc m
               k
               (construct-recur mv (rest ks) v))
        (assoc m
               k
               (construct-recur {} (rest ks) v))))))

(defn inflate-recur [ks m]
  (if (empty? ks)
    m
    (let [k (first ks)
          v (get m k)]
      (if (map? v)
        (if ((deflated-key? ".") k)
          (construct-recur (dissoc m k) ((deflated-key->path ".") k) (inflate-recur (keys v) v))
          {k (inflate-recur (keys v) v)})
        (if ((deflated-key? ".") k)
          (inflate-recur (rest ks)
                         (construct-recur (dissoc m k) ((deflated-key->path ".") k) v))
          (inflate-recur (rest ks) m))))))
