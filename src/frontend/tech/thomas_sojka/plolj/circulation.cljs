(ns tech.thomas-sojka.plolj.circulation
  (:require [tech.thomas-sojka.plolj.components :refer [plot-canvas]]
            [tech.thomas-sojka.plolj.utils :refer [translate]]))

(def width 300)
(def height 300)

(defn circle [{:keys [x y color]}]
  [:<>
   (map-indexed
    (fn [idx r]
      ^{:key idx}
      [:circle {:r r
                :cx x
                :cy y
                :fill "none" :stroke color}])
    (range 5 12 (+ (rand-int 3) 2)))])

(defn map-range [in_min, in_max, out_min, out_max]
  (fn [x] (+ (/ (* (- x in_min) (- out_max out_min)) (- in_max in_min)) out_min)))

(defn circles [big-r idx]
  [:g
   (let [scale (map-range 0 130 1 25)
         count (scale big-r)]
     (map-indexed
      (fn [key phi]
        [circle {:key key
                 :color "black"
                 :x (* (Math/sin (+ (when (even? idx) 0.03) phi)) big-r)
                 :y (* (Math/cos (+ (when (even? idx) 0.03) phi)) big-r)}])
      (range 0 (* 2 Math/PI) (/ (* 2 Math/PI) count))))])

(defn main []
  [plot-canvas {:width width :height height}
   [:g
    {:transform (translate (/ width 2)
                           (/ height 2))}
    (map-indexed
     (fn [idx big-r]
       ^{:key idx}
       [circles big-r idx])
     (range 0 140 20))]])
