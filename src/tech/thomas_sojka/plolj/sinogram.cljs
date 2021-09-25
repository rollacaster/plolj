(ns tech.thomas-sojka.plolj.sinogram
  (:require ["d3-scale" :refer [scaleLinear]]
            [reagent.core :as r]
            [tech.thomas-sojka.plolj.components
             :refer
             [download-button drawing-canvas plot-button]]
            [tech.thomas-sojka.plolj.utils :refer [translate]]))

(defn scale [[d-from d-to] [r-from r-to]]
  (-> (scaleLinear)
      (.domain #js[d-from d-to])
      (.range #js [r-from r-to])))



(def width 300)
(def height 300)
(def element (r/atom nil))

(defn sine-wave [{:keys [amplitude period offset]}]
  [:polyline {:fill "none"
              :stroke "black"
              :points
              (apply
               str
               (map (fn [i]
                      (let [y (scale [0 width] [0 (* js/Math.PI 2)])]
                        (str i "," (* amplitude (Math/sin (+ (* period (y i)) offset))) " ")))
                    (range width)))}])

(defn cos-wave [{:keys [amplitude period offset]}]
  [:polyline {:fill "none"
              :stroke "black"
              :points
              (apply
               str
               (map (fn [i]
                      (let [y (scale [0 width] [0 (* js/Math.PI 2)])]
                        (str i "," (* amplitude (Math/cos (+ (* period (y i)) offset))) " ")))
                    (range width)))}])
(defn w [p]
  (* width p))
(defn h [p]
  (* height p))

(defn main []
  [drawing-canvas {:width width :height height}
   [:<>
    [:svg {:xmlns "http://www.w3.org/2000/svg"
           :width width
           :height height
           :viewBox (str "0 0 " width " " height)
           :ref (fn [el] (when el (reset! element el)))}
     [:g {:transform (str (translate (w 0.5) 0) " rotate(90)")}
      (for [offset (range 20 22 0.25)]
        [sine-wave {:key (str offset) :amplitude (h 0.4) :period 2 :offset offset}])
      (for [offset (range 41 43 0.15)]
        [cos-wave {:key (str offset) :amplitude (h 0.3) :period 2 :offset offset}])]]
    [download-button {:element @element}]
    [plot-button {:element @element}]]])
