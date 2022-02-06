(ns tech.thomas-sojka.plolj.sierpinski
  (:require [reagent.core :as r]
            [tech.thomas-sojka.plolj.components
             :refer
             [download-button drawing-canvas plot-button]]
            [tech.thomas-sojka.plolj.utils :refer [translate tr-and]]))

(def width 300)
(def height 300)
(def element (r/atom nil))

(defn triangle [x1 y1 x2 y2 x3 y3]
  [:<>
   [:polygon {:points (str x1 "," y1 " "
                           x2 "," y2 " "
                           x3 "," y3)
              :stroke "black"
              :fill "none"}]])

(defn equi-tri [length]
  (triangle (- (/ length 2)) (/ length 2)
            (/ length 2) (/ length 2)
            0 (- (/ length 2))))
(defn rotate
  ([deg]
   (rotate deg 0 0))
  ([deg x y]
   (str "rotate(" deg "," x "," y ")")))
(defn sierpinski [length]
  (when (> length 10)
    [:<>
     (equi-tri length)
     [:g {:transform (translate (/ length 2) (- (* length 0.25)))}
      [sierpinski (/ length 2)]]
     [:g {:transform (translate (- (/ length 2)) (- (* length 0.25)))}
      [sierpinski (/ length 2)]]
     [:g {:transform (translate 0 (* length 0.75))}
      [sierpinski (/ length 2)]]]))

(defn main []
  [drawing-canvas {:width width :height height}
   [:<>
    [:svg {:xmlns "http://www.w3.org/2000/svg"
           :width width
           :height height
           :viewBox (str "0 0 " width " " height)
           :ref (fn [el] (when el (reset! element el)))}
     [:rect {:x 0 :y 0 
             :width (- width 1) :height (- height 1) :stroke "black"
             :fill "transparent"
             :stroke-width 1}]
     [:g
      {:transform (translate (/ width 2) (/ height 2))}
      (let [length 240]
        [:<>
         
         (equi-tri length)
         (let [length (/ length 2)]
           [:g {:transform (tr-and (translate 0 (/ length 2)) (rotate 180))}
            [sierpinski length]])])]]
    [download-button {:element @element}]
    [plot-button {:element @element}]]])
