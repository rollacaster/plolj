(ns tech.thomas-sojka.plolj.core
  (:require ["react-router-dom" :as router]
            canvas2svg
            [reagent.dom :as dom]
            [tech.thomas-sojka.plolj.circulation :as circulation]
            [tech.thomas-sojka.plolj.differential-growth :as differential-growth]
            [tech.thomas-sojka.plolj.donut :as donut]
            [tech.thomas-sojka.plolj.sinogram :as sinogram]
            [tech.thomas-sojka.plolj.volatize :as volatize]
            [tech.thomas-sojka.plolj.eye-of-sine :as eye-of-sine]
            [tech.thomas-sojka.plolj.tricles :as tricles]))

(def projects [{:path "/eye-of-sine"
                :component eye-of-sine/main}
               {:path "/tricles"
                :component tricles/main}
               {:path "/volatize"
                :component volatize/main}
               {:path "/donut"
                :component donut/main}
               {:path "/sinogram"
                :component sinogram/main}
               {:path "/circulation"
                :component circulation/main}
               {:path "/differential-growth"
                :component differential-growth/main}])

(defn main []
  [:div.ph4
   (map
    (fn [{:keys [path component]}]
      ^{:key path}
      [:div.mb3
       [:> (.-Route router)
        {:path path
         :exact true}
        [:div.overflow-hidden
         {:style {:display "flex"
                  :align-items "center"}}
         [component]]]] )
    projects)])

(defn app []
  [:> (.-BrowserRouter router)
   [:div.ph4
    [:nav.flex
     (map
      (fn [{:keys [path]}]
        ^{:key path}
        [:div.pa2
         [:> (.-Link router)
          {:to path} path]])
      projects)]]
   [main]])

(dom/render [app] (js/document.getElementById "app"))

(defn init [])


