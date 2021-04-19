(ns tech.thomas-sojka.plolj.core
  (:require [reagent.dom :as dom]
            [canvas2svg]
            [tech.thomas-sojka.plolj.volatize :as volatize]
            ["react-router-dom" :as router]))

(defn app []
  [:> (.-BrowserRouter router)
   [:div.overflow-hidden
    {:style {:height "100vh"
             :display "flex"
             :align-items "center"}}
    [:> (.-Route router)
     {:path "/"
      :exact true}
     [volatize/main]]]])

(dom/render [app] (js/document.getElementById "app"))

(defn init [])


