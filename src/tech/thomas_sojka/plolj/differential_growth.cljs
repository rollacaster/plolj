(ns tech.thomas-sojka.plolj.differential-growth
  (:require [reagent.core :as r]
            [tech.thomas-sojka.plolj.components :refer [drawing-canvas]]
            [tech.thomas-sojka.plolj.mover
             :refer
             [apply-force compute-position create-mover]]))

(def width 300)
(def height 300)
(def element (r/atom nil))
(defn w [p] (* p width))
(defn h [p] (* p height))
(def movers (r/atom [(create-mover 1 [(w 0.3) (h 0.5)])
                     (create-mover 1 [(w 0.7) (h 0.5)])]))

(defn d [[{[x y] :location} & rest]]
  (str "M " x " " y " " (apply str
                         (map (fn [{[x y] :location}]
                                (str "L " x " " y " "))
                              rest))))

(defn apply-force-to-all [movers]
  (map (fn [mover]
         (-> mover
             (apply-force [1 1])
             compute-position))
       movers))
(defonce step (r/atom 0))
(defn step! []
  (swap! movers apply-force-to-all)
  (when (< @step 15)
    (swap! step inc)
    (js/window.requestAnimationFrame step!)))
(reset! step 0)
(defn main []
  (js/window.requestAnimationFrame step!)
  (fn []
    [drawing-canvas {:width width :height height}
     [:<>
      [:svg
       {:xmlns "http://www.w3.org/2000/svg"
        :width width :height height
        :style {:fontFamily "Playfair Display"}
        :ref (fn [el] (when el (reset! element el)))}
       [:path {:d (d @movers) :stroke "black"}]]]]))
