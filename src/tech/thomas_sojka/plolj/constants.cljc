(ns tech.thomas-sojka.plolj.constants)

(def browser-dpi 96)

(def height-in-cm 14.8)
(def width-in-cm 10.5)

(defn cm->inch [cm]
  (* cm 0.393701))

;; DIN A6
(def height (Math/floor (* browser-dpi (cm->inch height-in-cm))))
(def width (Math/floor (* browser-dpi (cm->inch width-in-cm))))


