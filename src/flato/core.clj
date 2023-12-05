(ns flato.core
  (:require [clojure.string :as string]))

;;
;; deflate
;;

(declare deflate)

(defn- index-to-key [idx]
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
       (merge accum (deflate m (conj p (index-to-key idx)))))
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

(defn- deflated-key? [k] (string/includes? k "-"))

(defn- deflated-key->keys [k]
  (vec (map keyword (string/split (name k) #"-"))))

(defn- keys-in-map? [ks m]
  (not (= 'not-found (get-in m ks 'not-found))))

(defn keys-in-map [ks m]
  (let [res (reduce (fn [{:keys [ksm vls] :as acc} k]
                      (let [ks (conj ksm k)]
                        (if (keys-in-map? ks m)
                          {:ksm ks
                           :vls (get-in m ks)}
                          (reduced acc))))
                    {:ksm []
                     :vls {}}
                    ks)]
    [(:ksm res) (:vls res)]))

(defn- inflate-map [ks v nm]
  (let [[keys-found val-found] (keys-in-map ks nm)]
    (if (seq keys-found)
      (let [rest-ks (subvec ks (count keys-found))]
        (if (and (map? val-found)
                 (seq rest-ks))
          (assoc-in nm keys-found (merge-with into val-found (assoc-in {} rest-ks v)))
          (assoc-in nm keys-found v)))
      (assoc-in nm ks v))))

(defn inflate
  "Converts a one level deep flat map into a nested one."
  ([nm]
   {:pre [(map? nm)]}
   (let [m (deflate nm)]
     (reduce
      (fn [accum [k v]]
        (if (deflated-key? k)
          (let [ks (deflated-key->keys k)]
            (inflate-map ks v accum))
          (assoc accum k v)))
      {}
      m))))
