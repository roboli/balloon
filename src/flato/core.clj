(ns flato.core
  (:require [clojure.string :as string]))

(defn- index-to-str [idx]
  (keyword (str idx)))

(defn- join-keys [kys]
  (keyword (string/join "-" (map name kys))))

(defn- deflate-seq [coll p]
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
  ([m] (deflate m []))
  ([m p]
   {:pre [(map? m)]}
   (reduce
    (fn [accum [k v]]
      (cond
        (map? v)
        (merge accum (deflate v (conj p k)))
        
        (and (sequential? v)
             (map? (first v)))
        (merge accum (deflate-seq v (conj p k)))
        
        :else
        (assoc accum (join-keys (conj p k)) v)))
    {}
    m)))

(defn- inflate-w-keys [ks v]
  (let [rev-ks (reverse ks)]
    (reduce
     (fn [accum k]
       {(keyword k) accum})
     {(keyword (first rev-ks)) v}
     (drop 1 rev-ks))))

(defn inflate
  "Creates a nested map from a flat one with delimited keys."
  [m]
  {:pre [(map? m)]}
  (reduce
   (fn [accum [k v]]
     (if (string/includes? k "-")
       (merge-with into accum
                   (let [ks (string/split (name k) #"-")]
                     (inflate-w-keys ks v)))
       (assoc accum k v)))
   {}
   m))
