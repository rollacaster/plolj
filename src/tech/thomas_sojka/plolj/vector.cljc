(ns tech.thomas-sojka.plolj.vector)

(defn add
  ([v] v)
  ([[x1 y1] [x2 y2]]
   [(+ x1 x2) (+ y1 y2)])
  ([v1 v2 & vs]
   (apply add (add v1 v2) vs)))

(defn sub
  ([v1 v2]
   (vector (- (first v1) (first v2))
           (- (second v1) (second v2))))
  ([v1 v2 & vs]
   (apply sub (sub v1 v2) vs)))

(defn mult [v n]
  (mapv (fn [i] (* i n)) v))

(defn div [[x y] n] (if (or (= n 0) (= n 0.0))
                      (vector x y)
                      (vector (/ x n) (/ y n))))

(defn mag
  ([v]
   (Math/sqrt (apply + (map (fn [i] (* i i)) v)))))

(defn dist [v1 v2]
  (mag (sub v2 v1)))

(defn unit [v]
  (mult
   v
   (/ 1 (let [magnitude (mag v)] (if (= magnitude 0.0) 0.0001 magnitude)))))

(defn normalize [v]
  (let [m (mag v)]
    (if (not (= m 0.0)) (div v m) v)))

(defn limit [[x y] top]
  (if (> (mag [x y]) top)
    (mult (normalize [x y]) top)
    [x y]))

(defn rotate [[x y] angle]
  [(- (* (Math/cos angle) x) (* (Math/sin angle) y))
   (+ (* (Math/sin angle) x) (* (Math/cos angle) y))])

(defn from-angle
  ([angle]
   (from-angle angle 1))
  ([angle length]
   [(* length (Math/cos angle)) (* length (Math/sin angle))]))


