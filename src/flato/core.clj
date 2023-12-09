(ns flato.core
  (:require [clojure.string :as string]))

;;
;; deflate
;;

(declare deflate)

(defn- index->key [index]
  (keyword (str index)))

(defn- keys->deflated-key [ks]
  (keyword (string/join "-" (map name ks))))

(defn- deflate-seq [coll ks]
  (let [indexed (map-indexed
                 (fn [index item]
                   [index item])
                 coll)]
    (reduce
     (fn [accum [index v]]
       (let [new-k (index->key index)]
         (if (map? v)
           (merge accum (deflate v (conj ks new-k)))
           (merge accum (deflate {new-k v} ks)))))
     {}
     indexed)))

(defn deflate
  "Flats a nested map into a one level deep."
  ([m] (deflate m []))
  ([m ks]
   {:pre [(map? m)]}
   (reduce
    (fn [accum [k v]]
      (cond
        (map? v)
        (merge accum (deflate v (conj ks k)))
        
        (sequential? v)
        (merge accum (deflate-seq v (conj ks k)))
        
        :else
        (assoc accum (keys->deflated-key (conj ks k)) v)))
    {}
    m)))

;;
;; inflate
;;

(defn- deflated-key? [k] (string/includes? k "-"))

(defn- parseInt? [s]
  (try
    (Integer/parseInt s)
    true
    (catch Exception e
      false)))

(defn- deflated-key->path [k]
  (vec (map
        (fn [s]
          (if (parseInt? s)
            (Integer/parseInt s)
            (keyword s)))
        (string/split (name k) #"-"))))

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
                             (= 0 k))
                      {:end-path (conj end-path k)
                       :end-form (assoc-in end-form end-path [])}
                      (update acc :end-path conj k))))
                {:end-path []
                 :end-form form}
                path)]
    (assoc-in (:end-form result) (:end-path result) v)))

(defn- inflate-map [path v acc]
  (let [[path-found val-found] (path-in-map path acc)]
    (if (and (seq path-found)
             (not= path-found path)
             (map? val-found))
      (let [rest-path (subvec path (count path-found))]
        (assoc-in acc path-found (merge-with into val-found (assoc-inth {} rest-path v))))
      (assoc-inth acc path v))))

(defn inflate
  "Converts a one level deep flat map into a nested one."
  ([nm]
   {:pre [(map? nm)]}
   (let [m (deflate nm)]
     (reduce
      (fn [accum [k v]]
        (if (deflated-key? k)
          (let [path (deflated-key->path k)]
            (inflate-map path v accum))
          (assoc accum k v)))
      {}
      m))))
