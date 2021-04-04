(ns tech.thomas-sojka.plolj.components)

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
