(ns tech.thomas-sojka.plolj.utils
  (:require ["opentype.js" :as opentype]))

(defn load-font [url]
  (js/Promise.
   (fn [resolve reject]
     (.load opentype url
            (fn [err font]
              (if err
                (reject err)
                (resolve font)))))))

(defn text-path [font text font-size]
  (let [path (.getPath font text 0 0 font-size)]
    {:bbox (.getBoundingBox path)
     :d (.toPathData path)}))

(defn translate [x y]
  (str "translate(" (or x 0) "," (or y 0) ")"))
