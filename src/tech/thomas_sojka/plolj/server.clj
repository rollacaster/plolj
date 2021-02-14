(ns tech.thomas-sojka.plolj.server
  (:require [clj-http.client :as client]
            [clj-ssh.cli :as ssh :refer [default-session-options sftp]]
            [clojure.java.io :as io]
            [clojure.string :as str]
            [compojure.core :refer [defroutes POST]]
            [ring.adapter.jetty :refer [run-jetty]]
            [ring.middleware.cors :refer [wrap-cors]]
            [ring.middleware.defaults :refer [api-defaults wrap-defaults]]
            [ring.middleware.multipart-params :refer [wrap-multipart-params]]
            [ring.util.request :refer [body-string]]))

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
