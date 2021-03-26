(ns tech.thomas-sojka.plolj.worker
  (:require [thi.ng.color.core :as col]
            [thi.ng.math.core :as math]
            [canvas2svg]))

(def width (Math/floor (/ 1748 3)))
(def height (Math/floor (/ 1240 3)))

(defn resize [img-width img-height]
  (if (< img-width img-height)
    [width (* (/ img-width img-height) width)]
    [(* (/ img-width img-height) height) height]))

(defn draw-image-on-canvas [url]
  (let [canvas (new js/OffscreenCanvas width height)
        ctx (.getContext canvas "2d")]
  (-> (js/fetch url)
      (.then (fn [res] (.blob res)))
      (.then (fn [blob] (js/createImageBitmap blob)))
      (.then (fn [img]
               (let [[r-width r-height] (resize (.-width img) (.-height img))]
                 (.drawImage ctx img
                             (/ (- width r-width) 2)
                             (/ (- height r-height) 2)
                             r-width r-height))
               {:ctx ctx :canvas canvas})))))

(defn get-lightness [[r g b a]]
  (:l (col/as-hsla (col/rgba (/ r 255) (/ g 255) (/ b 255) (/ a 255)))))

(defn volatize-image
  ([ctx]
   (volatize-image ctx {}))
  ([{:keys [ctx canvas]}
    {:keys [y-line-space dark-ampl light-ampl noise-ampl]
     :or {y-line-space 7 dark-ampl 2 light-ampl 8 noise-ampl 2}}]
   (assert y-line-space)
   (assert dark-ampl)
   (assert light-ampl)
   (assert noise-ampl)
   (let [data (->> (array-seq (.-data (.getImageData ctx 0 0 width height)))
                   (partition 4)
                   (partition width))]
     (.clearRect ctx 0 0 width height)
     (doseq [y (range 0 height y-line-space)]
       (.beginPath ctx)
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
               (.lineTo ctx
                        x
                        (+ y y-line-space (if (= (mod i 2) 0) (- noise-ampl) noise-ampl)))
               (catch js/Object e
                 (prn x y e)))
             (recur x (inc i)))))
       (.stroke ctx))
     ^js (.transferToImageBitmap canvas)
     #_(let [svg-element (.getSvg ctx2)]
       (set! (.-id svg-element) "canvas")
       svg-element))))

(defn init []
   (js/self.addEventListener "message"
    (fn [^js e]
      (-> (draw-image-on-canvas (.. e -data))
          (.then (fn [ctx] (volatize-image ctx)))
          (.then (fn [bitmap] (js/postMessage bitmap)))))))
