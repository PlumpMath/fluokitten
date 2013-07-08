(ns uncomplicate.fluokitten.articles.learnyouahaskell-test
  "These expressions are used as examples in the
    Larn You a Haskell for Great Good
    article at the Fluokitten web site."
  (:use [uncomplicate.fluokitten jvm core test])
  (:require [uncomplicate.fluokitten.protocols :as protocols])
  (:use [midje.sweet :exclude [just]]))

(deftype Prob [xs]
  Object
  (hashCode [_]
    (hash xs))
  (equals [this that]
    (or (identical? this that)
        (and (instance? Prob that)
             (= xs (.xs that)))))
  protocols/Functor
  (fmap [_ f]
    (Prob. (fmap (fn [[x p]]
                   [(f x) p])
                 xs)))
  protocols/Applicative
  (pure [_ v]
    (Prob. [[v, 1]]))

  protocols/Monad
  (join [_]
    (let [multi-all (fn [[innerxs p]]
                      (map (fn [[x r]]
                              [x (* p r)])
                            (.xs innerxs)))]
      (Prob. (apply concat (map multi-all xs)))))
  (bind [p f]
    (join (fmap f p)))

  protocols/Foldable
  (fold [_]
    (fold (fmap first xs)))
  )

(defn prob [& xs]
  (if (= 1 (reduce (fn [sum [x p]] (+ sum p)) 0 xs))
    (Prob. xs)
    nil))

(facts
 (fmap - (Prob. [[3 1/2] [5 1/4] [9 1/4]]))
 => (Prob. [[-3 1/2] [-5 1/4] [-9 1/4]])

 (prob [3 1/2] [5 1/4] [9 1/4])
 => (Prob. [[3 1/2] [5 1/4] [9 1/4]])

 (prob [:tails 3/4] [:heads 1/2])
 => nil

 (pure (Prob. []) :something)
 => (prob [:something 1])

 (def this-situation (prob [(prob [:a 1/2] [:b 1/2]) 1/4]
                           [(prob [:c 1/2] [:d 1/2]) 3/4]))

 (join this-situation)
 => (prob [:a 1/8] [:b 1/8] [:c 3/8] [:d 3/8])

 (defn coin [] (prob [:heads 1/2] [:tails 1/2]))

 (defn loaded-coin [] (prob [:heads 1/10] [:tails 9/10]))

 (bind (coin) (fn [a]
 (bind (coin) (fn [b]
 (bind (loaded-coin) (fn [c]
 (pure (coin) (not (some #(= :heads %) [a b c])))))))))
 => (prob [false 1/40] [false 9/40] [false 1/40] [false 9/40]
          [false 1/40] [false 9/40] [false 1/40] [true 9/40]))
