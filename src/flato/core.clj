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

(defn- path-in-map? [ks m]
  (not (= 'not-found (get-in m ks 'not-found))))

(defn path-in-map [path m]
  (let [res (reduce (fn [{:keys [ksm vls] :as acc} k]
                      (let [ks (conj ksm k)]
                        (if (path-in-map? ks m)
                          {:ksm ks
                           :vls (get-in m ks)}
                          (reduced acc))))
                    {:ksm []
                     :vls {}}
                    path)]
    [(:ksm res) (:vls res)]))

(defn- assoc-inth [form path v]
  (let [result (reduce
                (fn [{:keys [cur frm] :as acc} k]
                  (if (keyword? k)
                    (update acc :cur conj k)
                    (if (and (nil? (get-in frm cur))
                             (= 0 k))
                      {:cur (conj cur k)
                       :frm (assoc-in frm cur [])}
                      (update acc :cur conj k))))
                {:cur []
                 :frm form}
                path)]
    (assoc-in (:frm result) (:cur result) v)))

(defn- inflate-map [path v nm]
  (let [[path-found val-found] (path-in-map path nm)]
    (if (and (seq path-found)
             (not= path-found path)
             (map? val-found))
      (let [rest-path (subvec path (count path-found))]
        (assoc-in nm path-found (merge-with into val-found (assoc-inth {} rest-path v))))
      (assoc-inth nm path v))))

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
