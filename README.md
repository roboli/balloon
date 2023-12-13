# balloon

Deflate (flat) a nested map into one level deep or inflate (unflat) a one level into a nested map using delimited keys.

*Inspired by these [guys](https://github.com/hughsk/flat).*

## Quickstart

For installation, add the following dependency to your `project.clj` file:

    [org.clojars.roboli/balloon "0.1.0-SNAPSHOT"]

Import namespace, example:

```clojure
(ns balloon.core
  (:require [balloon.core :as b]))
```

Use deflate to flat a nested hash-map:

```clojure
(b/deflate {:my-map {:one "one"
                    :two "two"
                    :any {:other "other"}}
            :my-array [{:a "a"} "b" "c"]})

;;=>
;; {:my-map.one "one",
;;  :my-map.two "two",
;;  :my-map.any.other "other",
;;  :my-array.0.a "a",
;;  :my-array.1 "b",
;;  :my-array.2 "c"}
```

Use inflate to convert a deflated (flatten) hash-map into a nested one:

```clojure
(b/inflate {:my-map.one "one",
            :my-map.two "two",
            :my-map.any.other "other",
            :my-array.0.a "a",
            :my-array.1 "b",
            :my-array.2 "c"})

;;=>
;; {:my-map {:one "one",
;;           :two "two",
;;           :any {:other "other"}},
;;  :my-array [{:a "a"} "b" "c"]}
```

## Usage

There are only two functions: deflate and inflate, each one with its own options.

### Deflate

```clojure
(deflate nested-hash-map :option-1-key option-1-val :option-2-key option-2-val ...)
```

Where options:

* `:delimiter`: Use different delimiter to build delimited keys, defaults to ".".
* `:keep-coll`: Do not flat collections (lists or vectors), defaults to `false`.

Examples:

```clojure
;; Using :delimiter

(b/deflate {:id 3
            :profile {:first-name "Lucas"
                      :last-last "Arts"}
            :location {:country "USA"
                       :city "LA"}}
    :delimiter "*")

;;=>
;; {:id 3,
;;  :profile*first-name "Lucas",
;;  :profile*last-last "Arts",
;;  :location*country "USA",
;;  :location*city "LA"}


;; Using :keep-coll

(b/deflate {:destinations ["Paris" "London" "Madrid"]
            :person {:first-name "John"
                     :last-last "Walker"}}
    :keep-coll true)

;;=>
;; {:destinations ["Paris" "London" "Madrid"],
;;  :person.first-name "John",
;;  :person.last-last "Walker"}
```

### Inflate

```clojure
(inflate flat-hash-map :option-1-key option-1-val :option-2-key option-2-val ...)
```

* `:delimiter`: Use different delimiter to unflat the hash-map delimited keys, defaults to ".".
* `:pre-deflate`: Run deflate on hash-map to guarantee is fully normalized before running unflat process, defaults to `true`.
* `:hash-map`: Unflat indexes in delimited keys as hash-map, not as a collection, defaults to `false`.

Examples:

```clojure
;; Using :delimiter

(b/inflate {:id 3,
            :profile*first-name "Lucas",
            :profile*last-last "Arts",
            :location*country "USA",
            :location*city "LA"}
    :delimiter "*")

;;=>
;; {:id 3,
;;  :profile {:first-name "Lucas",
;;            :last-last "Arts"},
;;  :location {:country "USA",
;;             :city "LA"}}


;; Using :hash-map

(b/inflate {:values.0.one "one"
            :values.1.two "two"
            :values.2.three "three"}
    :hash-map true)

;;=>
;; {:values {0 {:one "one"},
;;           1 {:two "two"},
;;           2 {:three "three"}}}
```

## License

Copyright © 2023 Roberto Oliveros

This program and the accompanying materials are made available under the
terms of the Eclipse Public License 2.0 which is available at
http://www.eclipse.org/legal/epl-2.0.

This Source Code may also be made available under the following Secondary
Licenses when the conditions for such availability set forth in the Eclipse
Public License, v. 2.0 are satisfied: GNU General Public License as published by
the Free Software Foundation, either version 2 of the License, or (at your
option) any later version, with the GNU Classpath Exception which is available
at https://www.gnu.org/software/classpath/license.html.
