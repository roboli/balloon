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

  (testing "Inflating maps using :hash-map true"
    (let [value {:value ["a" "b" "c"]
                 :other.0 "any"}
          result (b/inflate value :hash-map true)]
      (is (= result {:value ["a" "b" "c"]
                     :other {0 "any"}}))))

  (testing "Inflating maps using :hash-map true (again)"
    (let [value {:my-map.one "one",
                 :my-map.two "two",
                 :my-map.any.other "other",
                 :my-map.any.arr [{:a.b "c"}]
                 :my-array.0.a "a",
                 :my-array.1 "b",
                 :my-array.2 "c"}
          result (b/inflate value :hash-map true)]
      (is (= result {:my-map
                     {:one "one",
                      :two "two",
                      :any {:other "other",
                            :arr [{:a {:b "c"}}]}},
                     :my-array {0 {:a "a"}, 1 "b", 2 "c"}})))))
