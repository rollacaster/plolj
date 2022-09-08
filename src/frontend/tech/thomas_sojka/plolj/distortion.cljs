(ns tech.thomas-sojka.plolj.distortion
  (:require
   [tech.thomas-sojka.plolj.components :refer [plot-canvas]]
   [clojure.string :as str]))

(def width 300)
(def height 300)
(def step 14)

(defn pol->cart [{:keys [angle r]}]
  (let [x (+ (/ width 2) (* r (Math/cos angle)))
        y (+ (/ height 2) (* r (Math/sin angle)))]
    [x y]))

(defn distort [[x y] distortion-fn [center-x center-y]]
  (let [[v-x v-y] [(- x center-x) (- y center-y)]
        v-len (Math/sqrt (+ (Math/pow v-x 2) (Math/pow v-y 2)))
        distorted (+ v-len (distortion-fn v-len))
        [dist-x dist-y] [(* distorted (/ v-x v-len)) (* distorted (/ v-y v-len))]]
    [(+ dist-x center-x) (+ dist-y center-y)]))

(defn gaussian [sigma mu x]
  (* (/ 1 (* mu (Math/sqrt (* 2 Math/PI))))
    (Math/exp (- (/ (Math/pow (- x sigma) 2) (* 2 (Math/pow mu 2)))))))

(defn barrel-like-distortion [undistorted]
  (* 4000 (gaussian 30 45 undistorted)))

(def distortions
  [{:f barrel-like-distortion :center {:angle (* Math/PI 0.25), :r 120}}
   {:f barrel-like-distortion :center {:angle (* Math/PI 0.75), :r 120}}
   {:f barrel-like-distortion :center {:angle Math/PI, :r 00}}
   {:f barrel-like-distortion :center {:angle (* Math/PI 1.25), :r 120}}
   {:f barrel-like-distortion :center {:angle (* Math/PI 1.75), :r 120}}])

(defn main []
  [plot-canvas
   {:width width :height height}
   (let [lines (->> (for [y (range step height step)
                          x (range step width step)]
                      [x y])
                    (map-indexed vector)
                    (keep (fn [[idx [x y]]]
                            (cond
                              (and (odd? idx) (odd? (Math/round (/ y step))))
                              [x y]
                              (and (odd? idx) (even? (Math/round (/ y step))))
                              [x y])))
                    (group-by second)
                    (sort-by first)
                    vals)]
     (->> lines
          (keep-indexed (fn [idx line] (when (> (count lines) (inc idx)) [line (nth lines (inc idx))])))
          (map-indexed (fn [idx [line-a line-b]] (if (odd? idx) (interleave line-a line-b) (interleave line-b line-a))))
          (map (fn [line] (map (fn [pos]
                                (str/join "," (->> distortions
                                                   (reduce
                                                    (fn [pos {:keys [f center]}]
                                                      (distort pos f (pol->cart center)))
                                                    pos))))
                              line)))
          (map-indexed (fn [idx points] [:polyline {:points (str/join " " points)
                                                   :key idx
                                                   :fill "none"
                                                   :stroke "black"}]))))])
