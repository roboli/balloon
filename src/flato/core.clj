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
       (merge accum (flat m (conj p (index-to-str idx)))))
     {}
     indexed)))

(defn flat
  "Flats a nested map into a one level deep."
  ([m] (flat m []))
  ([m p]
   (reduce
    (fn [accum [k v]]
      (cond
        (map? v)
        (merge accum (flat v (conj p k)))
        
        (and (sequential? v)
             (map? (first v)))
        (merge accum (flat-seq v (conj p k)))
        
        :else
        (assoc accum (join-keys (conj p k)) v)))
    {}
    m)))
