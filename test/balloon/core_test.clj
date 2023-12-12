(ns balloon.core-test
  (:require [clojure.test :refer :all]
            [balloon.core :as b]))

(def path "test/balloon/")

(deftest deflate
  (testing "Deflating maps"
    (let [coll (read-string (slurp (str path "data_test.edn")))]
      (doall
       (for [item coll]
         (let [result (b/deflate (:inflated item))]
           (is (= result (:deflated item))))))))

  (testing "Deflating maps using :delimiter *"
    (let [coll (read-string (slurp (str path "data_test.edn")))]
      (doall
       (for [item coll]
         (let [result (b/deflate (:inflated item) :delimiter "*")]
           (is (= result (:deflated* item))))))))

  (testing "Deflating maps using :keep-coll true"
    (let [coll (read-string (slurp (str path "data_test.edn")))]
      (doall
       (for [item coll]
         (let [result (b/deflate (:inflated item) :keep-coll true)]
           (is (= result (:deflated-keep-colls item)))))))))

(deftest inflate
  (testing "Inflating maps"
    (let [coll (read-string (slurp (str path "data_test.edn")))]
      (doall
       (for [item coll]
         (let [result (b/inflate (:deflated item))]
           (is (= result (:inflated item))))))))

  (testing "Inflating maps using :delimiter *"
    (let [coll (read-string (slurp (str path "data_test.edn")))]
      (doall
       (for [item coll]
         (let [result (b/inflate (:deflated* item) :delimiter "*")]
           (is (= result (:inflated item))))))))

  (testing "Inflating maps using :pre-deflate false"
    (let [value {:value {:a.b "a.b"}
                 :other.0 "any"}
          result (b/inflate value :pre-deflate false)]
      (is (= result {:value {:a.b "a.b"}
                     :other ["any"]})))))
