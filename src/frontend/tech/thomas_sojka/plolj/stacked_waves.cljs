(ns tech.thomas-sojka.plolj.stacked-waves
  (:require ["d3-shape" :as d3-shape]
            [cljs.pprint :refer [pprint]]
            [reagent.core :as r]
            [tech.thomas-sojka.plolj.components :refer [plot-canvas]]))

(def dpi 4)
(def width (/ 1748 dpi))
(def height  (/ 2480 dpi))
(def step 15)
(defn color [lightness]
  (str "hsl(100 50% " lightness "%)"))
(defn- data []
  (reduce (fn [lines line]
            (if (empty? lines)
              (conj lines line)
              (conj lines
                    (map
                     (fn [line]
                       (let [{x1 :x y1 :y} line]
                         (merge line
                                {:y (min y1
                                         (reduce min
                                                 (map :y
                                                      (filter
                                                       #(= (:x %) x1)
                                                       (flatten lines)))))
                                 })))
                     line))))
          []
          (for [y (range height (* 4 step) (- step))]
            (for [x (range 0 width step)]
              (let [distance-to-center (/ (Math/abs (- (/ width 2) x)) (/ width 2))
                    variance (Math/max (* (- 1 distance-to-center) (* 4 step)) 0)]
                {:x x
                 :variance variance
                 :y (- y
                       (*  (rand) variance))
                 :color (color 0 #_(* 100 (/ 100 (max 1 y))))})))))
(defonce hovered-line (r/atom nil))
(def d (data))
(defn main []
  [:div {:style {:display "flex"}}
   [plot-canvas {:width width :height height}
    [:g #_{:style {:transform (str "translateY(-" (/ height 2) "px)")}}
     (doall
      (for [line d]
        (do
          (def line line)
          ^{:key (random-uuid)}
          [:path
           {:d
            ((-> (d3-shape/line (fn [d] (:x d)) (fn [d] (:y d)))
                 (.curve (.alpha d3-shape/curveCatmullRom 0.5)))
             line)
            :fill "none"
            :stroke (if (= @hovered-line line)
                      "red"
                      (:color (first line)))}])))]]
   [:div
    [:div
     (->> d
          (map
           (fn [line]
             ^{:key line}
             [:pre
              {:on-mouse-enter (fn []
                                 (prn "enter")
                                 (reset! hovered-line line))}
              (->> line
                   (map (fn [point] (dissoc point :color)))
                   (take 5))])))]
    #_[:pre
       (with-out-str (pprint (map
                              (fn [line]

                                (->> line
                                     (map (fn [point] (dissoc point :color)))
                                     (take 5)))
                              d)))]]])
