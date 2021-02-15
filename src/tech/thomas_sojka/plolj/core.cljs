(ns tech.thomas-sojka.plolj.core
  (:require [reagent.core :as r]
            [reagent.dom :as dom]
            [tech.thomas-sojka.plolj.constants :refer [width height]]
            [thi.ng.color.core :as col]
            [thi.ng.math.core :as math]
            [canvas2svg]))

(defonce svg (atom nil))
(defonce ctx (atom nil))
(def ui-state (r/atom {:y-line-space 7
                       :dark-ampl 2
                       :light-ampl 8
                       :noise-ampl 2}))

(defn get-lightness [[r g b a]]
  (:l (col/as-hsla (col/rgba (/ r 255) (/ g 255) (/ b 255) (/ a 255)))))

(defn volatize-image
  ([ctx]
   (volatize-image ctx {}))
  ([ctx {:keys [y-line-space dark-ampl light-ampl noise-ampl]
         :or {y-line-space 7 dark-ampl 2 light-ampl 8 noise-ampl 2}}]
   (assert y-line-space)
   (assert dark-ampl)
   (assert light-ampl)
   (assert noise-ampl)
   (let [ctx2 (new canvas2svg width height)
         data (->> (array-seq (.-data (.getImageData ctx 0 0 width height)))
                   (partition 4)
                   (partition width))]
     (doseq [y (range 0 height y-line-space)]
       (.beginPath ctx2)
       (loop [x 0
              i 0]
         (when (< x width)
           (let [row (nth data y)
                 [r g b a] (nth row x)
                 lightness (get-lightness [r g b a])
                 x (Math/min width (Math/floor (+ x (math/map-interval lightness
                                                                       [0 1]
                                                                       [dark-ampl light-ampl]))))]
             (try
               (.lineTo ctx2
                        x
                        (+ y y-line-space (if (= (mod i 2) 0) (- noise-ampl) noise-ampl)))
               (catch js/Object e
                 (prn x y e)))
             (recur x (inc i)))))
       (.stroke ctx2))
     (let [svg-element (.getSvg ctx2)]
       (set! (.-id svg-element) "canvas")
       (.replaceChild (js/document.getElementById "drop_zone") svg-element (js/document.getElementById "canvas"))
       svg-element))))

(comment
  (volatize-image @ctx {:y-line-space 20 :dark-ampl 5 :light-ampl 5}))

(defn draw-image-on-canvas [url]
  (new js/Promise
       (fn [resolve]
         (let [img (new js/Image)
               canvas (js/document.createElement "canvas")
               ctx (.getContext canvas "2d")]
           (set! (.-onload img)
                 (fn []
                   (set! (.-width canvas) width)
                   (set! (.-height canvas) height)
                   (.drawImage ctx img 0 0 width height)
                   (resolve ctx)))
           (set! (.-src img) url)))))

(def redraw
  (goog.functions.debounce
   (fn [params] (volatize-image @ctx params))
   1000))

(defn param-range [{:keys [label value on-change max] :or {max 20}}]
  (let [uuid (str (random-uuid))]
    [:<>
     [:div.mb2
      [:label.mr2 {:for uuid} label]
      [:span.fw7 value]]
     [:input.w5.mb4
      {:type "range" :min 1 :max max :value value
       :id uuid
       :on-change on-change}]]))

(defn app []
  [:div
   {:style {:height "100vh"
            :display "flex"
            :align-items "center"}}
   [:button {:style {:position "absolute" :top 64 :left 64 :padding "0.5rem 1rem" :border "none"
                     :background-color "#F3F4F6" :border-radius "0.5rem"
                     :box-shadow "5px 5px 5px #B91C1C" :cursor "pointer"}
             :on-click (fn []
                         (let [s (new js/XMLSerializer)]
                           (->  (js/fetch "http://localhost:8000/plot"
                                          (clj->js
                                           {:method "POST"
                                            :headers {"content-type" "application/json"}
                                            :body (.serializeToString s @svg)}))
                                (.catch prn))))}
    [:span {:style {:font-size 32 :color "#111827"}} "Plot"]]
   [:div#main.relative
    {:style {:box-shadow "0 0 #0000, 0 0 #0000, 0 25px 50px -12px rgba(0, 0, 0, 0.25)"
             :margin "auto"}
     :id "drop_zone"
     :onDragOver (fn [ev] (.preventDefault ev))
     :onDrop (fn [ev]
               (.preventDefault ev)
               (let [file (.getAsFile (first (vec ^js (.-dataTransfer.items ev))))
                     form-data (new js/FormData)]
                 (.append form-data "file" file)
                 (-> (js/fetch "http://localhost:8000/remove-bg" (clj->js {:method "POST" :body form-data}))
                     (.then #(.blob %))
                     (.then (fn [blob] (draw-image-on-canvas (.createObjectURL js/URL blob))))
                     (.then (fn [image] (reset! svg (volatize-image image))))
                     (.catch prn))))}
    [:canvas#canvas
     {:width width
      :height height}]
    [:div.sans-serif.absolute
     {:style {:right "-18rem" :top "0rem"}}
     [:div.flex.flex-column
      [param-range {:label "Line spacing"
                    :value (:y-line-space @ui-state)
                    :on-change (fn [e]
                                 (let [new-value ^js (.-target.value e)]
                                   (redraw (swap! ui-state assoc :y-line-space (js/parseInt new-value)))))}]
      [param-range {:label "Darkness amplitude"
                    :value (:dark-ampl @ui-state)
                    :max 8
                    :on-change (fn [e]
                                 (let [new-value ^js (.-target.value e)]
                                   (redraw (swap! ui-state assoc :dark-ampl (js/parseInt new-value)))))}]
      [param-range {:label "Lightness amplitude"
                    :value (:light-ampl @ui-state)
                    :max 8
                    :on-change (fn [e]
                                 (let [new-value ^js (.-target.value e)]
                                   (redraw (swap! ui-state assoc :light-ampl (js/parseInt new-value)))))}]
      [param-range {:label "Noise amplitude"
                    :value (:noise-ampl @ui-state)
                    :on-change (fn [e]
                                 (let [new-value ^js (.-target.value e)]
                                   (redraw (swap! ui-state assoc :noise-ampl (js/parseInt new-value)))))}]]]]])



(dom/render [app] (js/document.getElementById "app"))
(comment
  (when (= @svg nil)
    (-> (draw-image-on-canvas "images/person.png")
        (.then (fn [image]
                 (reset! ctx image)
                 (reset! svg (volatize-image image))))
        (.catch prn))))


