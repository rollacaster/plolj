(ns tech.thomas-sojka.plolj.mover
  (:require [tech.thomas-sojka.plolj.vector :as v]))

(defn apply-force [{:keys [mass] :as mover} force]
  (update mover :acceleration #(v/add % (v/div force mass))))

(defn update-mover [{:keys [velocity location] :as mover} acceleration]
  (let [velocity (v/add velocity acceleration)
        location (v/add location velocity)]
    (-> mover
        (assoc :location location)
        (assoc :velocity velocity)
        (assoc :accleration [0 0]))))

(defn create-mover [mass location]
  {:mass mass
   :location location
   :velocity [0.0 0.0]
   :acceleration [0.0 0.0]
   :a-velocity 0.0
   :a-acceleration 0.0
   :angle 0.0})

(defn compute-position [{:keys [acceleration velocity location angle a-velocity a-acceleration] :as mover}]
  (let [new-velocity (v/add acceleration velocity)
        new-location (v/add new-velocity location)
        new-a-velocity (+ a-acceleration a-velocity)
        new-angle (+ new-a-velocity angle)]
    (-> mover
        (assoc :velocity new-velocity)
        (assoc :location new-location)
        (assoc :a-velocity new-a-velocity)
        (assoc :angle new-angle)
        (assoc :a-acceleration 0)
        (assoc :acceleration [0 0]))))

(defn pol-to-cart [[r phi]]
  [(* r (Math/cos phi)) (* r (Math/sin phi))])
