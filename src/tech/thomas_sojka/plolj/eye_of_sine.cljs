(ns tech.thomas-sojka.plolj.eye-of-sine
  (:require [thi.ng.geom.svg.core :as svg :refer [ellipse]]))

(def scene
  (svg/svg
   {:width 300 :height 300}
   (vec
    (map-indexed
     (fn [idx g] ^{:key idx} g)
     (svg/group
      {:transform "rotate(90) translate(0 -300)" :key "group1"}
      (let [rings 50]
        (map
         (fn [i]
           (let [progress-in-percent (/ i rings)
                 cur-pi (* progress-in-percent Math/PI)
                 min-width 0
                 max-additional-width 80]
             (ellipse [150 (+ 20 (* 5 i))] (+ min-width (* max-additional-width (Math/sin cur-pi))) 10
                      {:fill "none" :stroke "black" :key i })))
         (range rings))))))
   (map-indexed
    (fn [idx g] ^{:key idx} g)
    (svg/group
     {}
     (let [rings 50]
         (map
          (fn [i]
            (let [progress-in-percent (/ i rings)
                  cur-pi (* progress-in-percent Math/PI)
                  min-width 0
                  max-additional-width 80]
              (ellipse [150 (+ 20 (* 5 i))] (+ min-width (* max-additional-width (Math/sin cur-pi))) 10
                       {:fill "none" :stroke "black" :key i})))
          (range rings)))))))

(defn main []
  [:div
   (update scene 1 (fn [attribs] (dissoc attribs "xmlns:xlink")))])
