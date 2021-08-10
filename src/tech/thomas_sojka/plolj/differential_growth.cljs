(ns tech.thomas-sojka.plolj.differential-growth
  (:require [reagent.core :as r]
            [tech.thomas-sojka.plolj.components :refer [drawing-canvas]]
            [tech.thomas-sojka.plolj.mover
             :refer
             [apply-force compute-position]]
            [tech.thomas-sojka.plolj.vector :as v]
            [clojure.spec.alpha :as s]
            [clojure.test.check]
            [clojure.test.check.properties]
            [clojure.spec.test.alpha :refer [check]]))

(def width 300)
(def height 300)
(def element (r/atom nil))
(defn w [p] (* p width))
(defn h [p] (* p height))
(def movers (r/atom [{:id 0 :mass 1, :location [90 150], :velocity [0 0], :acceleration [0 0]}
                     {:id 1 :mass 1, :location [210 150], :velocity [0 0], :acceleration [0 0]}
                     {:id 2 :mass 1, :location [100 200], :velocity [0 0], :acceleration [0 0]}]))
(comment
  (check `go-to-neighbours)
  (check `apply-force-to-all)
  (check `grow)
  (grow (second @movers) @movers)

  (grow (first @movers) @movers)
  (v/mult (v/sub (:location (second @movers)) (centroid @movers)) 0.001)
  (v/mult (v/sub (:locatiom (first @movers)) (centroid @movers)) 0.001)
  (v/mult (v/sub (:location (first @movers)) (centroid @movers)) 0.001)
  )
(defn d [[{[x y] :location} & rest]]
  (str "M " x " " y " " (apply str
                         (map (fn [{[x y] :location}]
                                (str "L " x " " y " "))
                              rest))))
(s/def ::mass float?)
(s/def ::vector (s/tuple float? float?))
(s/def ::location ::vector)
(s/def ::velocity ::vector)
(s/def ::acceleration ::vector)
(s/def ::mover (s/keys :req-un [::mass ::location ::velocity ::acceleration]))

(s/fdef go-to-neighbours
  :args (s/cat :mover ::mover :neighbours (s/coll-of ::mover))
  :ret ::vector)

(s/fdef apply-force-to-all
  :args (s/cat :neighbours (s/coll-of ::mover))
  :ret (s/coll-of ::mover))

(s/fdef grow
  :args (s/cat :neighbours (s/coll-of ::mover))
  :ret ::vector)

(defn centroid [movers]
  (let [[x y] (reduce
               (fn [points {:keys [location]}]
                 (v/add points location))
               [0 0]
               movers)]
    [(/ x (count movers))
     (/ y (count movers))]))

(defn grow [mover movers]
  (v/mult (v/sub (:location mover) (centroid movers)) 0.0001))

(defn go-to-neighbours [mover neighbours]
  (v/mult
   (v/unit
    (reduce
     (fn [dir {:keys [location]}]
       (v/add dir (v/sub location (:location mover))))
     [0 0]
     neighbours))
   0.2))

(defn not-to-close [mover neighbours]
  (reduce
   (fn [dir {:keys [location]}]
     (let [distance (v/dist (:location mover) location)]
       (v/add
        dir
        (v/mult
         (v/unit (v/sub (:location mover) location))
         (* (if (< distance 20) 5 0))))))
   [0 0]
   neighbours))



(defn apply-force-to-all [movers]
  (map (fn [mover]
         (-> mover
             (apply-force (go-to-neighbours mover (remove #(= mover %) movers)))
             (apply-force (not-to-close mover (remove #(= mover %) movers)))
             (apply-force (grow mover movers))
             compute-position))
       movers))

(defonce step (r/atom 0))
(defn step! []
  (swap! movers apply-force-to-all)
  (when (< @step 300)
    (swap! step inc)
    (js/window.requestAnimationFrame step!)))
(reset! step 0)
(defn main []
  (js/window.requestAnimationFrame step!)
  (fn []
    [:div.flex.justify-center.w-100
     [:div.absolute.top-1.left-1.dn
        [:div (str "Distance: " 0)]]
     [drawing-canvas {:width width :height height}
      [:<>

       [:svg
        {:xmlns "http://www.w3.org/2000/svg"
         :width width :height height
         :style {:fontFamily "Playfair Display"}
         :ref (fn [el] (when el (reset! element el)))}
        [:path {:d (d @movers) :stroke "black" :fill "none"}]
        (map-indexed
         (fn [idx {[x y] :location}]
           ^{:key idx}
           [:circle {:cx x :cy y :r 5 :fill (if (= idx 0) "red" "blue")}])
         @movers)]]]]))
