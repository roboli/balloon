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
     (fn [accum [idx v]]
       (let [new-k (index-to-key idx)]
         (if (map? v)
           (merge accum (deflate v (conj p new-k)))
           (merge accum (deflate {new-k v} p)))))
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
        
        (sequential? v)
        (merge accum (deflate-seq v (conj p k)))
        
        :else
        (assoc accum (join-keys (conj p k)) v)))
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
    (if (seq path-found)
      (let [rest-path (subvec path (count path-found))]
        (if (and (map? val-found)
                 (seq rest-path))
          (assoc-in nm path-found (merge-with into val-found (assoc-inth {} rest-path v)))
          (assoc-inth nm path v)))
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
