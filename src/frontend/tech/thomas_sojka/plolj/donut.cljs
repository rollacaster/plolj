(ns tech.thomas-sojka.plolj.donut
  (:require [tech.thomas-sojka.plolj.components :refer [plot-canvas]]
            [tech.thomas-sojka.plolj.utils :refer [translate]]))

(def width 300)
(def height 300)

(defn donut []
  [:g
   (let [count 120]
     (map-indexed
      (fn [idx r] [:circle {:key idx
                       :cx (* (Math/sin r) 90)
                       :cy (* (Math/cos r) 90)
                       :r 50 :fill "none" :stroke "black"}])
      (range 0 (* 2 Math/PI) (/ (* 2 Math/PI) count))))])

(defn main []
  [plot-canvas {:width width :height height}
   [:g
    {:transform (translate (/ width 2)
                           (/ height 2))}
    [donut]]])
