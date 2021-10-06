(ns tech.thomas-sojka.plolj.differential-growth
  (:require [cljs.pprint :refer [pprint]]
            [cljs.spec.gen.alpha :as gen]
            [clojure.spec.alpha :as s]
            [reagent.core :as r]
            [tech.thomas-sojka.plolj.components :refer [drawing-canvas]]
            [tech.thomas-sojka.plolj.mover
             :refer
             [apply-force compute-position create-mover]]
            [tech.thomas-sojka.plolj.vector :as v]
            [clojure.math.combinatorics :as combo]))

(def width 300)
(def height 300)
(def element (r/atom nil))
(defn w [p] (* p width))
(defn h [p] (* p height))

(defn d [[{[x y] :location} & rest]]
  (str "M " x " " y " " (apply str
                               (map (fn [{[x y] :location}]
                                      (str "L " x " " y " "))
                                    rest))))

(s/def ::valid-float (s/double-in :NaN? false :infinite? false))
(s/def ::mass ::valid-float)
(s/def ::vector (s/tuple ::valid-float ::valid-float))
(s/def ::centered
  (s/double-in
   :min 80 :max 200 :NaN? false :infinite? false))

(s/def ::location (s/tuple ::centered ::centered))
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

(defn add-random-neighbour [movers]
  (let [neighbours-idx (inc (rand-int (dec (count movers))))
        {[lx ly] :location} (nth movers (dec neighbours-idx))
        {[rx ry] :location} (nth movers neighbours-idx)]
    (into (conj (vec (take neighbours-idx movers))
                (create-mover 1 [(/ (+ lx rx) 2) (/ (+ ly ry) 2)]))
          (vec (drop neighbours-idx movers)))))

(defn centroid [movers]
  (let [[x y] (reduce
               (fn [points {:keys [location]}]
                 (v/add points location))
               [0 0]
               movers)]
    [(/ x (count movers))
     (/ y (count movers))]))

#_(def options )

(defn grow [mover movers options]
  (v/mult (v/sub (:location mover) (centroid movers)) (:grow options)))

(defn go-to-neighbours [mover neighbours options]
  (v/mult
   (v/unit
    (reduce
     (fn [dir {:keys [location]}]
       (v/add dir (v/sub location (:location mover))))
     [0 0]
     neighbours))
   (:go-to-neighbours options)))

(defn nearby? [m1 m2]
  (< (v/dist (:location  m1) (:location m2)) 40))

(defn not-to-close [mover movers options]
    (v/mult
   (v/unit
    (->> movers
         (filter (partial nearby? mover))
         (reduce
          (fn [dir {:keys [location]}]
            (v/add
             dir
             (v/sub (:location mover) location)))
          [0 0])))
   (:not-to-close options)))

(defn stay-inside [mover]
  (-> mover
      (update-in [:location 0] #(Math/max 0 (Math/min % (- width 20))))
      (update-in [:location 1] #(Math/max 0 (Math/min % (- height 20))))))

(defn middle-point [neighbours]
  [(/ (->> neighbours
           (map (comp first :location))
           (reduce +))
      2)
   (/ (->> neighbours
           (map (comp second :location))
           (reduce +)) 2)])

(defn stay-between-neighbours [mover neighbours options]
  (if (> (count neighbours) 1)
    (v/mult (v/unit (v/sub (middle-point neighbours) (:location mover)))
            (:stay-between-neighbours options))
    [0 0]))

(defn neighbours [idx movers]
  (remove
   nil?
   [(nth movers (dec idx) nil)
    (nth movers (inc idx) nil)]))

(defn apply-force-to-all [options movers]
  (map-indexed (fn [idx mover]
                 (-> mover
                     (apply-force (go-to-neighbours mover (neighbours idx movers) options))
                     (apply-force (not-to-close mover (remove #(= mover %) movers) options))
                     (apply-force (stay-between-neighbours mover (neighbours idx movers) options))
                     (apply-force (grow mover movers options))
                     compute-position
                     stay-inside))
               movers))

(def timer (r/atom 0))
(defonce start (r/atom nil))
(def max-step 150)

(defn step! [{:keys [movers step options stop]} curr-time]
  (when-not @start
    (reset! start curr-time))
  (reset! timer curr-time)
  (swap! movers (partial apply-force-to-all options))
  (if (< @step max-step)
    (do
      (swap! step inc)
      (js/window.requestAnimationFrame (partial step! {:movers movers :step step :options options :stop stop})))
    (do
      (reset! stop (js/Date.))
      (reset! start nil))))

(defn init [{:keys [movers step interval options stop]}]
  (reset! step 0)
  (reset! timer 0)
  (js/window.clearInterval @interval)
  (prn "init"
       (reset! interval
               (js/window.setInterval
                (fn [] (when (< @step max-step)
                        (swap! movers add-random-neighbour)))
                (:grow-interval options))))
  (js/window.requestAnimationFrame (partial step! {:movers movers :step step :options options :stop stop})))


(defn sketch [options]
  (let [movers (r/atom [{:id 0 :mass 1, :location [90 151], :velocity [0 0], :acceleration [0 0]}
                        {:id 1 :mass 1, :location [150 150], :velocity [0 0], :acceleration [0 0]}
                        {:id 2 :mass 1, :location [200 150], :velocity [0 0], :acceleration [0 0]}])
        step (r/atom 0)
        interval (r/atom nil)
        stop (r/atom nil)]
    (r/create-class
     {:component-did-mount (fn [] (init {:movers movers :step step :interval interval :options options :stop stop}))
      :component-will-unmount (fn []
                                (prn "clear" @interval)
                                                                (js/window.clearInterval @interval))
      :reagent-render
      (fn []
        [:div.mr3
         [:pre (with-out-str (cljs.pprint/pprint options))]
         [:pre (Math/round (- @timer @start)) "ms / " @step]
         [drawing-canvas {:width width :height height}
          [:<>
           
           (let [movers @movers]
             [:svg
              {:xmlns "http://www.w3.org/2000/svg"
               :width width :height height
               :style {:fontFamily "Playfair Display"}
               :ref (fn [el] (when el (reset! element el)))}
              (when (> (count movers) 0)
                [:path {:d (d movers) :stroke "black" :fill "none"}])
              #_(map-indexed
                 (fn [idx {[x y] :location}]
                   ^{:key idx}
                   [:circle {:cx x :cy y :r 5 :fill (if (= idx 1) "red" "blue")}])
                 movers)
              #_(map-indexed
                 (fn [idx {[vx vy] :velocity [lx ly] :location}]
                   (let [[vx vy] (v/mult [vx vy] 10)]
                     ^{:key idx}
                     [:line {:x1 lx :y1 ly :x2 (+ vx lx) :y2 (+ vy ly) :stroke "black" :stroke-width 2}]))
                 movers)])]]])})))
(defn round2
  "Round a double to the given precision (number of significant digits)"
  [precision d]
  (let [factor (Math/pow 10 precision)]
    (/ (Math/round (* d factor)) factor)))

(defn main []
  [:div.flex.flex-wrap
   (map
    (fn [[go-to-neighbours
         not-to-close
         stay-between-neighbours]]
      [:<>
       [sketch {:go-to-neighbours go-to-neighbours
                :stay-between-neighbours stay-between-neighbours
                :not-to-close not-to-close
                :grow 0.000
                :grow-interval 20}]])
    (combo/cartesian-product
     (map (partial round2 2) (range 0.2 0.5 0.1))
     (map (partial round2 2) (range 0.03 0.1 0.0699))
     (map (partial round2 2) (range 0.03 0.1 0.0699))))])



(comment
  ;; WHAT HAPPENS with neighbours at the edge?
  (let [movers (take 3 (gen/sample (s/gen ::mover)))
        [mx my] (middle-point (neighbours 1 movers))]

    (stay-between-neighbours
     {:mass -1, :location [80 80], :velocity [0.5 1], :acceleration [0.5 -0.5]}
     (neighbours
       0
       '({:mass -1, :location [80 80], :velocity [0.5 1], :acceleration [0.5 -0.5]}
        {:mass 0.5, :location [128 128], :velocity [-2 0.5], :acceleration [1 0.5]}
        {:mass 1, :location [128 128], :velocity [-2 3], :acceleration [-2 -2]})))
)
  )
