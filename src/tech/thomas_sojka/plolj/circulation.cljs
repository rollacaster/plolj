(ns tech.thomas-sojka.plolj.circulation
  (:require [reagent.core :as r]
            [tech.thomas-sojka.plolj.components
             :refer
             [download-button drawing-canvas plot-button]]
            [tech.thomas-sojka.plolj.utils :refer [translate]]))

(defn mean [xs]
  (/ (apply + xs) (count xs)))
(defn rand-gaussian []
  (->> (repeatedly rand)
       (partition 10)
       (map mean)
       first))


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

(def element (r/atom nil))

(defn main []
  [drawing-canvas {:width width :height height}
   [:<>
    [:svg {:xmlns "http://www.w3.org/2000/svg"
           :width width
           :height height
           :viewBox (str "0 0 " width " " height)
           :ref (fn [el] (when el (reset! element el)))}
     [:rect {:fill "none" :stroke "black" :width (dec width ) :height (dec height)}]
     [:g
      {:transform (translate (/ width 2)
                             (/ height 2))}
      #_(map
       (fn [r]
         [:circle {:r r
                   :fill "none" :stroke "black"}])
       (range 5 (+ 5 (rand-int 15)) (+ 1 (rand-int 10))))
      
      (map-indexed
       (fn [idx big-r]
         ^{:key idx}
         [circles big-r idx])
       (range 0 140 20))]]
    [download-button {:element @element}]
    [plot-button {:element @element}]]])
