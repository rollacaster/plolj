(ns tech.thomas-sojka.plolj.postcard
  (:require
   [clojure.string :as str]
   [reagent.core :as r]
   [tech.thomas-sojka.plolj.constants :refer [cm->pixel]]
   [tech.thomas-sojka.plolj.utils :refer [load-font text-path translate]]))

(def font (r/atom nil))

(defn- split-paragraphs [text max-size]
  (reduce
   (fn [paragraphs word]
     (let [current-sentence (last paragraphs)]
       (if (> (+ (count current-sentence) (count word)) max-size)
         (conj paragraphs word)
         (update paragraphs (dec (count paragraphs)) str (str (if (> (count current-sentence) 1)
                                                                " " "")
                                                              word)))))
   [""]
   (str/split text #" ")))

(defn- text [{:keys [x y]} content]
  (when content
    [:g {:transform (translate x y)}
     [:path {:d (:d (text-path @font content 16))}]]))

(defn text-block [{:keys [x y width]} block]
  (map-indexed
   (fn [idx sentence]
     [text {:key idx :x x :y (+ y (* idx 20))} sentence])
   (split-paragraphs
    block
    width)))

(defn- separator [{:keys [width height]}]
  [:line {:stroke "black"
          :x1 (/ width 2)
          :x2 (/ width 2)
          :y1 (* 0.1 height)
          :y2 (* 0.9 height)}])

(defn- stamp [{:keys [width height]}]
  [:rect {:x (* width 0.85)
          :y (* 0.1 height)
          :fill "transparent"
          :stroke "black"
          :width (* width 0.1)
          :height (* height 0.2)}])

(defn- address-lines [{:keys [width height]}]
  [:<>
   (map-indexed
    (fn [idx height-percentage]
      [:g {:key idx :transform (translate (* width 0.55) (* height-percentage height))}
       [:line {:stroke "black"
               :x1 0
               :x2 (* (/ width 2) 0.8)
               :y1 0
               :y2 0}]])
    (range 0.45 0.9 0.10))])

(defn- font-loading []
  (-> (load-font "fonts/Roboto/Roboto-Regular.ttf")
      (.then (fn [res] (reset! font res))))
  (fn [children]
    (when @font children)))

(defn postcard-separator [{:keys [width height]}]
  [:<>
   [:line {:stroke "black"
           :x1 (* 0.4 width)
           :y1 (/ height 2)
           :x2 (* 0.6 width)
           :y2 (/ height 2)}]
   [:line {:stroke "black"
           :x1 (/ width 2)
           :y1 (* 0.4 height)
           :x2 (/ width 2)
           :y2 (* 0.6 height)}]])

(defn postcard [{:keys [content]}]
  (let [width (cm->pixel 14.8)
        height (cm->pixel 10.5)]
    [font-loading
     [:<>
      (when content [content {:width width :height height}])
      [separator {:width width :height height}]
      [stamp {:width width :height height}]
      [address-lines {:width width :height height}]]]))
