(ns tech.thomas-sojka.plolj.borders
  (:require [tech.thomas-sojka.plolj.paths
             :refer
             [path-1 path-2 path-3 path-4 path-5]]))

(defn floral-bottom-border []
  [:path {:d  path-1 :stroke-width "1.4"}])

(defn divider-1 []
  [:path {:d path-2
          :style {:fill "#000000" :stroke "none" :stroke-width "0.1"}}])

(defn divider-2 []
  [:path {:style {:fill "#000000" :stroke "none" :stroke-width "0.133333"}
          :d path-3}])

(defn divider-3 []
  [:path {:style {:fill "#000000" :stroke "none" :stroke-width "0.09999975"}
          :d path-4}])

(defn divider-4 []
  [:path {:d path-5
          :style {:fill "#000000" :stroke "none" :stroke-width "0.1"}}])







