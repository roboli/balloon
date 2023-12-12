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
           (is (= result (:deflated* item)))))))))
