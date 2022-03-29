(ns tech.thomas-sojka.plolj.donut
  (:require [reagent.core :as r]
            [tech.thomas-sojka.plolj.components
             :refer
             [download-button drawing-canvas plot-button]]
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

(def element (r/atom nil))

(defn main []
  [drawing-canvas {:width width :height height}
   [:<>
    [:svg {:xmlns "http://www.w3.org/2000/svg"
           :width width
           :height height
           :viewBox (str "0 0 " width " " height)
           :ref (fn [el] (when el (reset! element el)))}
     [:g
      {:transform (translate (/ width 2)
                             (/ height 2))}
      [donut]]]
    [download-button {:element @element}]
    [plot-button {:element @element}]]])
