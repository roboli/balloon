(ns flato.core
  (:require [clojure.string :as string]))

(defn- index-to-str [idx]
  (keyword (str idx)))

(defn- join-keys [kys]
  (keyword (string/join "-" (map name kys))))

(defn- flat-seq [coll p]
  (let [indexed (map-indexed
                 (fn [idx item]
                   [idx item])
                 coll)]
    (reduce
     (fn [accum [idx m]]
       (merge accum (deflate m (conj p (index-to-str idx)))))
     {}
     indexed)))

(defn deflate
  "Flats a nested map into a one level deep."
  ([m] (flat m []))
  ([m p]
   (reduce
    (fn [accum [k v]]
      (cond
        (map? v)
        (merge accum (deflate v (conj p k)))
        
        (and (sequential? v)
             (map? (first v)))
        (merge accum (flat-seq v (conj p k)))
        
        :else
        (assoc accum (join-keys (conj p k)) v)))
    {}
    m)))

(defn- inflat-with-keys [ks v]
  (reduce
   (fn [accum k]
     {(keyword k) accum})
   {(keyword (last ks)) v}
   (drop-last ks)))

(defn inflate
  "Creates a nested map from a flat one with delimited keys."
  [m]
  (reduce
   (fn [accum [k v]]
     (if (string/includes? k "-")
       (merge-with into accum
                   (let [ks (string/split (name k) #"-")]
                     (inflat-with-keys ks v)))
       (assoc accum k v)))
   {}
   m))
