(ns rohan.util)

(defn seqable? [x]
  (try
    (seq x)
    true
    (catch java.lang.IllegalArgumentException _
      false)))
