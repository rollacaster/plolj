(ns tech.thomas-sojka.plolj.differential-growth
  (:require [clojure.spec.alpha :as s]
            [clojure.spec.test.alpha :refer [check]]
            [reagent.core :as r]
            [tech.thomas-sojka.plolj.components :refer [drawing-canvas]]
            [tech.thomas-sojka.plolj.mover
             :refer
             [apply-force compute-position create-mover]]
            [tech.thomas-sojka.plolj.vector :as v]
            [clojure.test.check]
            [clojure.test.check.properties]))

(def width 300)
(def height 300)
(def element (r/atom nil))
(defn w [p] (* p width))
(defn h [p] (* p height))
(def movers (r/atom [{:id 0 :mass 1, :location [90 150], :velocity [0 0], :acceleration [0 0]}
                     {:id 1 :mass 1, :location [210 150], :velocity [0 0], :acceleration [0 0]}
                     {:id 2 :mass 1, :location [100 200], :velocity [0 0], :acceleration [0 0]}
                     #_{:id 3 :mass 1, :location [170 50], :velocity [0 0], :acceleration [0 0]}]))

(defn d [[{[x y] :location} & rest]]
  (str "M " x " " y " " (apply str
                         (map (fn [{[x y] :location}]
                                (str "L " x " " y " "))
                              rest))))

(s/def ::valid-float (s/and float? (complement js/Number.isNaN)))
(s/def ::mass ::valid-float)
(s/def ::vector (s/tuple ::valid-float ::valid-float))
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

(s/fdef stay-inside
  :args (s/cat :mover ::mover)
  :ret ::mover)

(defn add-neighbours [movers]
  (let [with-neighbours
        (vec
         (apply mapcat
                (fn [l r]
                  (let [{[lx ly] :location} l
                        {[rx ry] :location} r]
                    [l
                     (create-mover 1 [(/ (+ lx rx) 2) (/ (+ ly ry) 2)])
                     r]))
                (->> movers
                     (map-indexed #(vector %1 %2))
                     (group-by (fn [[idx _]] (even? idx)))
                     vals
                     (map (fn [movers] (map second movers))))))]
    (if (odd? (count movers))
      (conj with-neighbours (last movers))
      with-neighbours)))

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
         (* (if (< distance 40) 0.5 0))))))
   [0 0]
   neighbours))

(defn stay-inside [mover]
  (-> mover
      (update-in [:location 0] #(Math/max 0 (Math/min % (- width 20))))
      (update-in [:location 1] #(Math/max 0 (Math/min % (- height 20))))))

(stay-inside {:id 0, :mass 1, :location [500 150], :velocity [0 0], :acceleration [0 0]})

(defn neighbours [idx movers]
  (remove
   nil?
   [(nth movers (dec idx) nil)
    (nth movers (inc idx) nil)]))

(defn apply-force-to-all [movers]
  (map-indexed (fn [idx mover]
         (-> mover
             (apply-force (go-to-neighbours mover (neighbours idx movers)))
             (apply-force (not-to-close mover (remove #(= mover %) movers)))
             #_(apply-force (grow mover movers))

             compute-position
             stay-inside))
       movers))

(defonce step (r/atom 0))
(defonce interval (r/atom nil))

(defn init []
  (reset! step 0)
  (js/window.clearInterval @interval)
  #_(reset! interval
          (js/window.setInterval
           (fn [] (when (< @step 300)
                   (swap! movers add-neighbours)))
           1000)))
(defn step! [time]
  (swap! movers apply-force-to-all)
  (when (< @step 300)
    (swap! step inc)
    (js/setTimeout
     (fn []
       (js/window.requestAnimationFrame step!))
     80)))

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
         @movers)
        (map-indexed
         (fn [idx {[vx vy] :velocity [lx ly] :location}]
           (let [[vx vy] (v/mult [vx vy] 10)]
             ^{:key idx}
             [:line {:x1 lx :y1 ly :x2 (+ vx lx) :y2 (+ vy ly) :stroke "black" :stroke-width 2}]))
         @movers)]]]]))
(init)

(comment
  (check `go-to-neighbours)
  (check `apply-force-to-all)
  (check `grow)
  (check `stay-inside)

  (grow (second @movers) @movers)

  (grow (first @movers) @movers)
  (v/mult (v/sub (:location (second @movers)) (centroid @movers)) 0.001)
  (v/mult (v/sub (:locatiom (first @movers)) (centroid @movers)) 0.001)
  (v/mult (v/sub (:location (first @movers)) (centroid @movers)) 0.001)
  )
