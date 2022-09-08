(ns tech.thomas-sojka.plolj.components
  (:require [reagent.core :as r]))

(defn drawing-canvas [{:keys [width height on-drop style]} children]
  [:div#main.relative
   {:style (merge
            {:box-shadow "0 0 #0000, 0 0 #0000, 0 25px 50px -12px rgba(0, 0, 0, 0.25)"
             :margin "auto"
             :transition "0.5s all"
             :width width
             :height height}
            style)
     :id "drop_zone"
     :onDragOver (fn [ev] (.preventDefault ev))
     :onDrop on-drop}
   children])

(defn download-button [{:keys [element]}]
  [:button.shadow-2.pointer.bn.pv2.ph3.br3.grow.outline-0.mb1
          {:style {:bottom 0 :background-color "#F3F4F6" }
           :on-click (fn []
                       (let [el (.createElement js/document "a")]
                         (.setAttribute el "href" (str "data:text/plain;charset=utf-8," (js/encodeURIComponent (.-outerHTML element))))
                         (.setAttribute el "download" "sketch.svg")
                         (set! (.-style.display el) "none")
                         (.appendChild js/document.body el)
                         (.click el)
                         (.removeChild js/document.body el)))}
          [:span {:style {:font-size 32 :color "#111827"}} "Download"]])

(defn plot-button [{:keys [element]}]
  [:button.shadow-2.pointer.bn.pv2.ph3.br3.grow.outline-0.ml1.mt5.mb1
   {:style {:bottom 0 :background-color "#F3F4F6" }
    :on-click (fn []
                (let [s (new js/XMLSerializer)]
                    (->  (js/fetch "http://localhost:8000/plot"
                                   (clj->js
                                    {:method "POST"
                                     :headers {"content-type" "application/json"}
                                     :body (.serializeToString s element)}))
                         (.catch prn))))}
   [:span {:style {:font-size 32 :color "#111827"}} "Plot"]])

(defn plot-canvas []
  (let [svg-node! (r/atom nil)]
    (fn [{:keys [width height]} children]
      (prn svg-node!)
      [:div
       [drawing-canvas {:width width :height height}
        [:svg
         {:ref (fn [el] (prn el)(when el (reset! svg-node! el)))
          :width width
          :height height}
         [:rect {:x 0 :y 0
                 :width (- width 1) :height (- height 1) :stroke "black"
                 :fill "transparent"
                 :stroke-width 1}]
         children]]
       [plot-button {:element @svg-node!}]])))
