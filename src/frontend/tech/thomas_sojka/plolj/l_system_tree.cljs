(ns tech.thomas-sojka.plolj.l-system-tree
  (:require
   [tech.thomas-sojka.plolj.components :refer [drawing-canvas]]
   [tech.thomas-sojka.plolj.constants :refer [cm->pixel]]
   [tech.thomas-sojka.plolj.l-system :as l-system]))

(defn plot-button [svg]
  [:button.shadow-2.pointer.bn.pv2.ph3.br3.grow.outline-0.ml1.mt5.mb1
   {:style {:bottom 0 :background-color "#F3F4F6" }
    :on-click (fn []
                (->  (js/fetch "http://localhost:8000/plot"
                               (clj->js
                                {:method "POST"
                                 :headers {"content-type" "application/json"}
                                 :body (.-outerHTML @svg)}))
                     (.catch prn)))}
   [:span {:style {:font-size 32 :color "#111827"}} "Plot"]])

(defn main []
  (let [svg (atom nil)]
    (fn []
      [:div
       (let [width (cm->pixel 29.7)
             height (cm->pixel 21)]
         [drawing-canvas
          {:width width
           :height height}
          [:svg.ba
           {:ref (fn [el] (when el (reset! svg el)))
            :width width
            :height height}
           [:g
            {:transform "translate(900, 690)"}
            (l-system/run
              {:name "tree"
               :rules [{:from "|", :to "||-(-|+|⬤+|⬤)+(+|-|⬤-|⬤)"}]
               :axiom "|"
               :angle (* (/ Math/PI 180) 25)
               :length 200
               :radius 3
               :order 3})]
           [:g
            {:transform "translate(500, 690)"}
            (l-system/run
              {:name "tree"
               :rules [{:from "|", :to "||-(-|+|⬤+|⬤)+(+|-|⬤-|⬤)"}]
               :axiom "|"
               :angle (* (/ Math/PI 180) 25)
               :length 200
               :radius 3
               :order 2})]
           [:g
            {:transform "translate(200, 690)"}
            (l-system/run
              {:name "tree"
               :rules [{:from "|", :to "||-(-|+|⬤+|⬤)+(+|-|⬤-|⬤)"}]
               :axiom "|"
               :angle (* (/ Math/PI 180) 25)
               :length 200
               :radius 3
               :order 1})]]])
       [plot-button {:svg svg}]])))
