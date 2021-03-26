(ns tech.thomas-sojka.plolj.core
  (:require [reagent.core :as r]
            [reagent.dom :as dom]
            [tech.thomas-sojka.plolj.constants :refer [width height]]
            [thi.ng.color.core :as col]
            [thi.ng.math.core :as math]
            [canvas2svg]))

(def svg (r/atom nil))
(defonce ctx (atom nil))
(def loading-state (r/atom nil))
(def ui-state (r/atom {:y-line-space 7
                       :dark-ampl 2
                       :light-ampl 8
                       :noise-ampl 2}))
(defonce worker (js/Worker. "/js/worker.js"))

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
     [:input.w5.mb4.outline-0
      {:type "range" :min 1 :max max :value value
       :id uuid
       :on-change on-change}]]))

(defn app []
  [:div.overflow-hidden
   {:style {:height "100vh"
            :display "flex"
            :align-items "center"}}
   [:div#main.relative
    {:style {:box-shadow "0 0 #0000, 0 0 #0000, 0 25px 50px -12px rgba(0, 0, 0, 0.25)"
             :margin "auto"
             :transition "0.5s all"
             :transform (str "translateX(" (if @svg -120 0) "px)")}
     :id "drop_zone"
     :onDragOver (fn [ev] (.preventDefault ev))
     :onDrop (fn [ev]
               (.preventDefault ev)
               (reset! loading-state "REMOVE_BG")
               (let [file (.getAsFile (first (vec ^js (.-dataTransfer.items ev))))
                     form-data (new js/FormData)]
                 (.append form-data "file" file)
                 (-> (js/fetch "http://localhost:8000/remove-bg" (clj->js {:method "POST" :body form-data}))
                     (.then #(.blob %))
                     (.then (fn [blob]
                              (reset! loading-state "VOLATIZE_IMG")
                              (.. worker (postMessage (.createObjectURL js/URL blob)))))
                     ;; (.then (fn [blob] (draw-image-on-canvas (.createObjectURL js/URL blob))))
                     ;; (.then (fn [image]
                     ;;          (reset! ctx image)
                     ;;          (reset! loading-state "VOLATIZE_IMG")
                     ;;          (reset! svg (volatize-image image))
                     ;;          (reset! loading-state "DONE")))
                     (.catch prn))))}
    [:canvas#canvas
     {:width width
      :height height}]
    [:div.absolute.f2.w-100.flex.justify-center
     {:style {:top "50%" :left "50%"
              :opacity (if (and @loading-state (not= @loading-state "DONE")) 1 0)
              :transition "all 0.5s"
              :transform "translate(-50%, -50%"}}
     [:span.absolute
      {:class (when (= @loading-state "VOLATIZE_IMG") "slide-out-bck-bottom")}
      "Remove background"]
     [:span
      {:class (when (= @loading-state "DONE") "slide-out-bck-bottom")
       :style {:opacity (if (or (= @loading-state "VOLATIZE_IMG") (= @loading-state "DONE")) 1 0)
               :transition "all 0.5s 1s"}}
      "Volatize image"]]
    [:div.absolute.h-100.flex.flex-column.justify-between
     {:style {:opacity (if @svg 1 0)
              :transition "all 0.5s 0.5s"
              :top 0
              :right "-22rem"}}
     [:div.sans-serif
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
                                    (redraw (swap! ui-state assoc :noise-ampl (js/parseInt new-value)))))}]]]
     [:button.shadow-2.pointer.bn.pv2.ph3.br3.grow.outline-0.mb1
      {:style {:bottom 0 :background-color "#F3F4F6" }
       :on-click (fn []
                   (let [s (new js/XMLSerializer)]
                     (->  (js/fetch "http://localhost:8000/plot"
                                    (clj->js
                                     {:method "POST"
                                      :headers {"content-type" "application/json"}
                                      :body (.serializeToString s @svg)}))
                          (.catch prn))))}
      [:span {:style {:font-size 32 :color "#111827"}} "Plot"]]]]])



(dom/render [app] (js/document.getElementById "app"))
(comment
  (when (= @svg nil)
    (-> (draw-image-on-canvas "images/person.png")
        (.then (fn [image]
                 (reset! ctx image)
                 (reset! svg (volatize-image image))))
        (.catch prn))))
(defn init []
  (.. worker (addEventListener "message" (fn [e]
                                           (.transferFromImageBitmap
                                            (.getContext (js/document.getElementById "canvas") "bitmaprenderer")
                                            (.-data e))
                                           (reset! loading-state "DONE")))))


