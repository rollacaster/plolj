(ns tech.thomas-sojka.plolj.core
  (:require ["react-router-dom" :as router]
            canvas2svg
            [reagent.dom :as dom]
            [tech.thomas-sojka.plolj.circulation :as circulation]
            [tech.thomas-sojka.plolj.differential-growth :as differential-growth]
            [tech.thomas-sojka.plolj.donut :as donut]
            [tech.thomas-sojka.plolj.sinogram :as sinogram]
            [tech.thomas-sojka.plolj.volatize :as volatize]))

(defn app []
  [:> (.-BrowserRouter router)
   [:div.overflow-hidden
    {:style {:height "100vh"
             :display "flex"
             :align-items "center"}}
    [:> (.-Route router)
     {:path "/"
      :exact true}
     [volatize/main]]
    [:> (.-Route router)
     {:path "/donut"
      :exact true}
     [donut/main]]
    [:> (.-Route router)
     {:path "/circulation"
      :exact true}
     [circulation/main]]
    [:> (.-Route router)
     {:path "/sinogram"
      :exact true}
     [sinogram/main]]
    [:> (.-Route router)
     {:path "/differential-growth"}
     [differential-growth/main]]]])

(dom/render [app] (js/document.getElementById "app"))

(defn init [])


