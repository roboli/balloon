(ns balloon.core-test
  (:require [clojure.test :refer :all]
            [balloon.core :as b]))

(def path "test/balloon/")

(deftest deflate
  (testing "Deflating maps"
    (let [data (read-string (slurp (str path "data_test.edn")))]
      (doall
       (for [inflated (:inflated data)]
         (let [result (b/deflate (:value inflated))]
           (is (= result (:result inflated)))))))))
