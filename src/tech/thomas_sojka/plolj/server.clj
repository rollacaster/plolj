(ns tech.thomas-sojka.plolj.server
  (:require [clj-http.client :as client]
            [clojure.java.io :as io]
            [image-resizer.resize :as resize]
            [tech.thomas-sojka.plolj.constants :as constants]
            [tech.thomas-sojka.clj-axidraw.core :as axidraw]))

(defn fetch-removed-background-image
  ([image-name]
   (fetch-removed-background-image image-name "public/images/"))
  ([image-name image-path]
   (let [res (client/post
              "https://api.remove.bg/v1.0/removebg"
              {:headers {"X-Api-Key" "7NkD69NFxhdcx1to2xvKDwn1"}
               :multipart [{:name "Content/type" :content "image/png"}
                           {:name "image_file" :content (io/file (str image-path image-name))}
                           {:name "size" :content "auto"}]
               :as :byte-array})]
     (with-open [w (io/output-stream "resources/" image-name)]
       (.write w (:body res))))))
(-> (io/file "public/images/manu.png")
    (resize/resize-fn constants/width constants/height))

(comment
  (axidraw/estimate
   "resources/person.svg")
  (fetch-removed-background-image "public/person.png"))
