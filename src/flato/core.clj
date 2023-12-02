(ns flato.core
  (:require [clojure.string :as string]))

;;
;; deflate
;;

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

;;
;; inflate
;;

(defn- deflated-key->keys [k]
  (map keyword (string/split (name k) #"-")))

(defn- deflated-key? [k] (string/includes? k "-"))

(defn- keys-in-map? [ks m]
  (not (= 'not-found (get-in m ks 'not-found))))

(defn- inflate-map [ks v nm]
  (if (keys-in-map? ks nm)
    (assoc-in nm ks (merge (get-in nm ks) v))
    (assoc-in nm ks v)))

(defn inflate
  "Flats a nested map into a one level deep."
  ([m]
   {:pre [(map? m)]}
   (reduce
    (fn [accum [k v]]
      (cond

        (deflated-key? k)
        (let [ks (deflated-key->keys k)]
          (inflate-map (drop-last ks)
                       {(last ks) (if (map? v)
                                    (inflate v)
                                    v)}
                       accum))

        (map? v)
        (assoc accum k (inflate v))

        :else
        (assoc accum k v)))
    {}
    m)))
