(ns ipfs.base58)
;; The MIT License (MIT)
;;
;; Copyright (c) 2016 Richard Hull
;;
;; Permission is hereby granted, free of charge, to any person obtaining a copy
;; of this software and associated documentation files (the "Software"), to deal
;; in the Software without restriction, including without limitation the rights
;; to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
;; copies of the Software, and to permit persons to whom the Software is
;; furnished to do so, subject to the following conditions:
;;
;; The above copyright notice and this permission notice shall be included in all
;; copies or substantial portions of the Software.
;;
;; THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
;; IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
;; FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
;; AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
;; LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
;; OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
;; SOFTWARE.

(def alphabet (vec "123456789ABCDEFGHJKLMNPQRSTUVWXYZabcdefghijkmnopqrstuvwxyz"))

(def inverted-alphabet
  (into {}
    (map #(vector %1 %2)
      alphabet
      (iterate inc 0))))

(defn count-leading [pred s]
  (->>
    s
    (map byte)
    (take-while pred)
    count))

(defn string->bigint [base xform s]
  (reduce +
    (map #(* (xform %1) %2)
      (reverse s)
      (iterate (partial * base) 1M))))

(def divmod (juxt quot mod))

(def first-char? (partial = (byte (first alphabet))))

(defn emitter [base value]
  (if (pos? value)
    (let [[d m] (divmod value base)]
      (cons
        (int m)
        (lazy-seq (emitter base d))))))

(defn pipeline [from to xform map-fn drop-pred replace-ch s]
  (->>
    s
    (string->bigint from xform)
    (emitter to)
    (map map-fn)
    reverse
    (concat (repeat (count-leading drop-pred s) replace-ch))
    (apply str)))

(defn encode [value]
  (pipeline 256 58 byte alphabet zero? (first alphabet) value))

(defn decode [value]
  (->>
    (drop-while first-char? value)
    (pipeline 58 256 inverted-alphabet char first-char? "\000")))
