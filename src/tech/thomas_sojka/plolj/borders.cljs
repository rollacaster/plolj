(ns tech.thomas-sojka.plolj.borders
  (:require [tech.thomas-sojka.plolj.paths
             :refer
             [path-1 path-2 path-3 path-4 path-5]]
            [tech.thomas-sojka.plolj.utils :refer [scale tr-and translate]]))

(defn floral-bottom-border []
  [:g {:transform (scale 0.8)}
   [:path {:d  path-1 :stroke-width "1.4"}]])

(defn divider-1 []
  [:g {:transform (tr-and (scale 0.2) (translate 0 -325))}
   [:path {:d path-2
           :style {:fill "#000000" :stroke "none" :stroke-width "0.1"}}]])

(defn divider-2 []
  [:g {:transform (tr-and (scale 0.2) (translate 0 -325))}
   [:path {:style {:fill "#000000" :stroke "none" :stroke-width "0.133333"}
           :d path-3
           :transform "scale(0.75000001)"}]])

(defn divider-3 []
  [:g {:transform (tr-and (scale 0.2) (translate 0 -325))}
   [:path {:style {:fill "#000000" :stroke "none" :stroke-width "0.09999975"}
           :d path-4}]])

(defn divider-4 []
  [:g {:transform (tr-and (scale 0.1) (translate 0 -325))}
   [:path {:d path-5
           :style {:fill "#000000" :stroke "none" :stroke-width "0.1"}}]])







