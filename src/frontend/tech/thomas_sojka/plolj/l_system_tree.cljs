(ns tech.thomas-sojka.plolj.l-system-tree
  (:require
   [tech.thomas-sojka.plolj.components :refer [drawing-canvas]]
   [tech.thomas-sojka.plolj.l-system :as l-system]))

(def svg (atom nil))

(defn plot-button []
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
  [:div
   (let [width 300
         height 300]
     [drawing-canvas
      {:width width
       :height height}
      [:svg
       {:ref (fn [el] (when el (reset! svg el)))
        :width width
        :height height}
       [:rect {:x 0 :y 0
               :width (- width 1) :height (- height 1) :stroke "black"
               :fill "transparent"
               :stroke-width 1}]
       [:g
        {:transform "translate(170, 295)"}
        (l-system/run
          {:name "tree"
           :rules [{:from "|", :to "||-(-|+|⬤-+|⬤)+(+|-|⬤-|⬤)"}]
           :axiom "|"
           :angle (* (/ Math/PI 180) 25)
           :length 88
           :radius 3
           :order 3})]]])
   [plot-button]])
