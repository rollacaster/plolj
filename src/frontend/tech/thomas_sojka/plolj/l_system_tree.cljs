(ns tech.thomas-sojka.plolj.l-system-tree
  (:require
   [tech.thomas-sojka.plolj.components :refer [plot-canvas]]))

(defn l-system [current-axiom rules]
  (apply str
         (reduce
          (fn [next-axiom axiom]
            (concat next-axiom (or (some-> (filter #(= (str axiom) (:from %)) rules) first :to) (str axiom))))
          ""
          current-axiom)))

(def alphabet
  {"|" (fn [{:keys [length]}] [:line-up length])
   "-" (fn [{:keys [angle]}] [:rotate-left angle])
   "+" (fn [{:keys [angle]}] [:rotate-right angle])
   "⬤" (fn [{:keys [radius]}] [:circle radius])
   "(" (fn [_] [:push-matrix])
   ")" (fn [_] [:pop-matrix ])})

(defn alpha->desc [{:keys [system length angle radius]}]
  (map (fn [letter] ((alphabet (str letter)) {:length length :angle angle :radius radius})) system))

(defn l-system-generations [{:keys [order length angle axiom rules radius]}]
  (->> (reduce (fn [systems idx]
                 (let [{:keys [system length angle radius]} (nth systems idx)]
                   (concat
                    systems
                    [{:system (l-system system rules)
                      :length (/ length 2)
                      :angle angle
                      :radius radius}])))
               [{:system axiom
                 :length length
                 :angle angle
                 :radius radius}]
               (range order))
       last))

(defn rotate [[x1 y1] [x2 y2] angle]
  [(+ (- (* (- x2 x1) (Math/cos angle)) (* (- y2 y1) (Math/sin angle))) x1)
   (+ (+ (* (- y2 y1) (Math/cos angle)) (* (- x2 x1) (Math/sin angle))) y1)])

(defn from [lines]
  (if (= (first (last lines)) :pop)
    {:lines (vec (butlast lines))
     :start (last (last lines))}
    {:start (last (last lines))
     :lines lines}))

(defn coords [steps]
  (->> (reduce (fn [state [type arg]]
                 (-> state
                     (update :lines (fn [lines]
                                      (case type
                                        :line-up (let [{:keys [start lines]} (from lines)]
                                                   (conj lines
                                                         [start
                                                          (let [[x y] start] (rotate start [x (- y arg)] (:rotate state)))]))
                                        :line-down (let [{:keys [start lines]} (from lines)]
                                                     (conj lines
                                                           [start
                                                            (let [[x y] start] (rotate start [x (+ y arg)] (:rotate state)))]))
                                        :pop-matrix (conj lines [:pop (:start (last (:matrix state)))])
                                        lines)))
                     (update :rotate (fn [angle]
                                       (case type
                                         :rotate-left (- angle arg)
                                         :rotate-right (+ angle arg)
                                         :pop-matrix (:angle (last (:matrix state)))
                                         angle)))
                     (update :matrix (fn [matrix]
                                       (case type
                                         :push-matrix (conj matrix {:angle (:rotate state)
                                                                    :start (last (last (:lines state)))})
                                         :pop-matrix (pop matrix)
                                         matrix)))
                     (update :circles (fn [circles]
                                        (case type
                                          :circle (conj circles [(:start (from (:lines state))) arg])
                                          circles)))))
               {:lines [[[0 0]]]
                :rotate 0
                :circles []
                :matrix []}
               steps)))

(defn svg [{:keys [lines circles]}]
  (->> (concat
        (->> lines
             (filter #(and (= (count %) 2) (every? vector? %)))
             (map (fn [[[x1 y1] [x2 y2]]] [:line {:x1 x1 :y1 y1 :x2 x2 :y2 y2 :stroke "black"}])))
        (->> circles
             (map (fn [[[x y] r]] [:circle {:cx x :cy y :r r}]))))
       (into [:<>])
       vec))

(defn run [desc]
  (->> desc
       l-system-generations
       alpha->desc
       coords
       svg))

(defn main []
  (let [width 300
        height 300]
    [plot-canvas {:width width :height height}
     [:g
      {:transform "translate(170, 295)"}
      (run
        {:name "tree"
         :rules [{:from "|", :to "||-(-|+|⬤-+|⬤)+(+|-|⬤-|⬤)"}]
         :axiom "|"
         :angle (* (/ Math/PI 180) 25)
         :length 88
         :radius 3
         :order 3})]]))
