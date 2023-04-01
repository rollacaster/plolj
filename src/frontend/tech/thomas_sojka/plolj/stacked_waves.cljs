(ns tech.thomas-sojka.plolj.stacked-waves
  (:require ["d3-shape" :as d3-shape]
            [tech.thomas-sojka.plolj.components :refer [plot-canvas]]))

(def width 300)
(def height 300)
(def step 15)

(defn- data []
  (let [[line & lines] (for [y (range height (* 4 step) (- step))]
                         (for [x (range 0 (+ width step) step)]
                           (let [distance-to-center (/ (Math/abs (- (/ width 2) x)) (/ width 2))
                                 variance (Math/max (* (- 1 distance-to-center)
                                                       (- 1 distance-to-center)
                                                       (* 4 step)) 0)]
                             {:x x
                              :y (- y (*  (rand) variance))})))]
    (reduce (fn [lines line]
              (conj lines
                    (map
                     (fn [line]
                       (let [{x :x y :y} line
                             smallest-y (reduce min (map :y (filter #(= (:x %) x) (flatten lines))))]
                         (assoc line :y (min y smallest-y))))
                     line)))
            [line]
            lines)))

(def draw-curve
  (-> (d3-shape/line (fn [d] (:x d)) (fn [d] (:y d)))
      (.curve (.alpha d3-shape/curveCatmullRom 0.5))))

(defn main []
  [plot-canvas {:width width :height height}
   [:g
    (for [line (data)]
      ^{:key line}
      [:path
       {:d
        (draw-curve line)
        :fill "none"
        :stroke "black"}])]])
