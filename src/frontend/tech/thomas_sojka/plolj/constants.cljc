(ns tech.thomas-sojka.plolj.constants)

(def browser-dpi 96)

(def height-in-cm 14.8)
(def width-in-cm 10.5)

(defn cm->inch [cm]
  (* cm 0.393701))

(defn cm->pixel [cm]
  (* browser-dpi (cm->inch cm)))

;; DIN A6
(def height (Math/floor (cm->pixel height-in-cm)))
(def width (Math/floor (cm->pixel width-in-cm)))


