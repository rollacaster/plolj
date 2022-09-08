(ns tech.thomas-sojka.plolj.tricles
  (:require [tech.thomas-sojka.plolj.components :refer [plot-canvas]]
            [thi.ng.geom.svg.core :as svg]))

(def width 300)
(def height 300)
(defn row [r step y]
  (let [[row1 row2 row3]
        [(map (fn [i] [(- (+ (* i step)) r)
                      (- (+ y) r)])
              (range 0 (+ (/ width step) 2)))
         (map (fn [i] [(- (+ (- (* (inc i) step) (/ step 2)) (rand-int step)) r)
                      (- (+ y step (rand-int step)) r)])
              (range 0 (+ (/ width step) 1)))
         (map (fn [i] [(- (+ (* i step)) r)
                      (- (+ y (* step 2)) r)])
              (range 0 (+ (/ width step) 1)))]]
    [(interleave row1 row2)
     (interleave row3 row2)]))

(defn data [r step]
  (->> (range 0 width (* 2 step))
       (map (fn [y] (row r step y)))
       (mapcat concat)
       (map #(partition 2 %))))

(defn point-in-circle? [[x y] r]
  (<= (Math/sqrt (+ (* x x) (* y y))) r))
(defn line-outside-circle? [line]
  (every? (fn [point] ((complement point-in-circle?) point 120)) line))
(defn move-line-in-circle [line]
  (map (fn [[x y]]
         (if (point-in-circle? [x y] 120)
           [x y]
           [(* 120 (Math/cos (Math/atan2 y x)))
            (* 120 (Math/sin (Math/atan2 y x)))]))
       line))
(defn line-in-circle? [line] (every? (fn [point] (point-in-circle? point 120)) line))
(defn move-point-out-of-circle [[x y]]
  (if (point-in-circle? [x y] 120)
     [(* 121 (Math/cos (Math/atan2 y x)))
      (* 121 (Math/sin (Math/atan2 y x)))]
     [x y]))
(defn move-line-out-of-circle [[p1 p2]]
  [(move-point-out-of-circle p1)
   (move-point-out-of-circle p2)])

(defn left-of-circle? [[x y]]
  (or (< y -120) (> y 120) (< x 0)))
(defn cut-out-circle [lines]
  [(take-while left-of-circle? lines)
   (drop-while left-of-circle? lines)])

(def scene
  (svg/svg
   {:width 300 :height 300}
   (map-indexed
    (fn [idx g] ^{:key idx} g)
    (svg/group
     {}
     (let [r 120]
       (vec
        (svg/group
         {:transform "translate(150, 150)" :key "group"}
         (svg/ellipse [0 0] r r {:stroke "black" :stroke-width 3
                                   :fill "transparent"
                                   :key "circle"})
         (svg/group
            {:stroke "black"}
            (map-indexed
             (fn [idx args] (svg/line-strip args {:key idx}))
                 (->> (data 150 20)
                      (map (fn [points] (->> points
                                            (remove line-outside-circle?)
                                            (mapcat move-line-in-circle)))))))
         (svg/group
          {:stroke "black"}
          (map-indexed
           (fn [idx args] (svg/line-strip args {:key idx}))
           (->> (data 150 10)
                (map (fn [points] (->> points
                                      (remove line-in-circle?)
                                      (mapcat move-line-out-of-circle))))
                (mapcat cut-out-circle)))))))))))

(defn main []
  [plot-canvas {:width width :height height}
   (update scene 1 (fn [attribs] (dissoc attribs "xmlns:xlink")))])
