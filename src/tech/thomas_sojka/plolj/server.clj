(ns tech.thomas-sojka.plolj.server
  (:require [clj-http.client :as client]
            [clj-ssh.cli :as ssh :refer [default-session-options sftp]]
            [clojure.java.io :as io]
            [clojure.string :as str]
            [compojure.core :refer [defroutes POST]]
            [mikera.image.core :as img]
            [ring.adapter.jetty :refer [run-jetty]]
            [ring.middleware.cors :refer [wrap-cors]]
            [ring.middleware.defaults :refer [api-defaults wrap-defaults]]
            [ring.middleware.multipart-params :refer [wrap-multipart-params]]
            [ring.util.request :refer [body-string]]
            [thi.ng.color.core :as color]
            [thi.ng.geom.svg.adapter :as adapt]
            [thi.ng.geom.svg.core :as svg]
            [thi.ng.math.core :as math]))

(def get-lightness (comp :l color/as-hsla color/int32))
(def width (Math/floor (/ 1748 3)))
(def height (Math/floor (/ 1240 3)))

(defn volatize-image
  ([width height data]
   (volatize-image width height data {}))
  ([width height data
    {:keys [y-line-space dark-ampl light-ampl noise-ampl]
     :or {y-line-space 7 dark-ampl 2 light-ampl 8 noise-ampl 2}}]
   (assert y-line-space)
   (assert dark-ampl)
   (assert light-ampl)
   (assert noise-ampl)
   (for [y (range 0 height y-line-space)]
     (loop [x 0
            i 0
            lines []]
       (if (< x width)
         (let [pixel (get data (+ x (* y width)))
               lightness (get-lightness (or pixel 0))
               x (Math/min (double width) (Math/floor (+ x (math/map-interval lightness
                                                                       [0 1]
                                                                       [dark-ampl light-ampl]))))]
           (recur x
                  (inc i)
                  (conj lines [x (+ y
                                    y-line-space
                                    (if (= (mod i 2) 0) (- noise-ampl) noise-ampl))])))
         lines)))))

(comment
  (->>(svg/svg
       {:width width :height height}
       (->> (img/get-pixels (img/load-image-resource "removed-background/test.jpg"))
            (volatize-image (.getWidth (img/load-image-resource "removed-background/test.jpg"))
                            (.getHeight (img/load-image-resource "removed-background/test.jpg")))
            (map (fn [line] (svg/line-strip line {:stroke "black"})))))
      (adapt/all-as-svg)
      (svg/serialize)
      (spit "test.svg")))

(default-session-options {:strict-host-key-checking :no})

(defn fetch-removed-background-image [file filetype]
  (let [current-date (.format (java.text.SimpleDateFormat. "yyyy-MM-dd-HH-mm") (new java.util.Date))
        temp-filename (str "resources/removed-background/" current-date "." filetype)]
    (io/make-parents temp-filename)
    (let [res (client/post
               "https://api.remove.bg/v1.0/removebg"
               {:headers {"X-Api-Key" "7NkD69NFxhdcx1to2xvKDwn1"}
                :multipart [{:name "Content/type" :content (str "image/" filetype)}
                            {:name "image_file" :content file}
                            {:name "size" :content "auto"}]
                :as :byte-array})]
      (with-open [w (io/output-stream temp-filename)]
        (.write w (:body res)))
      (io/file temp-filename))))

(defonce server (atom nil))


(defroutes public-routes
  (POST "/remove-bg" {multipart-params :multipart-params}
        (let [file (get multipart-params "file")
              [_ filetype] (str/split (:filename file) #"\.")
              file-without-bg (fetch-removed-background-image (io/file (:path (bean (:tempfile file)))) filetype)]
          {:status 200
           :headers {"Content-type" (str "image/" filetype)}
           :body file-without-bg}))
  (POST "/plot" svg
        (spit "resources/current.svg" (body-string svg))
        (sftp "axidraw" :put "resources/current.svg" "plots/current.svg" :username "pi")
        #_(ssh/ssh "axidraw" "/home/pi/.local/bin/axicli -m manual -M walk_mmy --walk_dist -218" :username "pi")
        (ssh/ssh "axidraw" "/home/pi/.local/bin/axicli plots/current.svg" :username "pi")
        #_(ssh/ssh "axidraw" "/home/pi/.local/bin/axicli -m manual -M walk_mmy --walk_dist 218" :username "pi")
        (ssh/ssh "axidraw" "/home/pi/.local/bin/axicli -m manual -M disable_xy" :username "pi")
        {:status 200}))

(when @server
  (.stop @server)
  (reset! server nil))

(when (not @server)
  (reset! server (run-jetty
                  (-> public-routes
                      (wrap-defaults api-defaults)
                      wrap-multipart-params
                      (wrap-cors
                       :access-control-allow-origin [#"http://localhost:8080"]
                       :access-control-allow-methods [:get :put :post :delete]))
                  {:port 8000 :join? false})))
