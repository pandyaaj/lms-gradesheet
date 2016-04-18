(ns lms-gradesheet.routes.home
  (:require [lms-gradesheet.layout :as layout]
            [lms-gradesheet.models.quiz :as quiz]
            [compojure.core :refer [defroutes GET POST]]
            [ring.util.http-response :refer [ok]]
            [clojure.java.io :as io]))

(defn home-page []
  (layout/render
   "home.html" {:home (-> "docs/home.md" io/resource slurp)}))

(defn about-page []
  (layout/render
   "about.html" {:about-content (-> "docs/about.md" io/resource slurp)}))

(defn emit-quiz-message
  [title body]
  (layout/render "quiz-msg.html" {:message-title title
                                  :message-body body}))

(defn save-submission
  [form]
  (quiz/record-grade form)
  (let [grade-object (quiz/remove-token form)
        grade (quiz/count-grade grade-object)]
    (emit-quiz-message "Success"
                     (str "Your Grade: " grade))))


(defn grade-quiz
  [form]
  (cond
   (= (:cin form) "")
   (emit-quiz-message "Not Accepted"
                      "CIN cannot be empty")
   :else (save-submission form)))

(defn show-quiz
  [number]
  (let [exam (first (quiz/get-quiz number))]
    (layout/render "quiz.html" exam)))


(defn write-quiz [x]
  (let [exam-file (str "exams/example" x ".json")
          exam (quiz/read-exam exam-file)]
      (quiz/write-quiz exam)))

(defn write-quizes []
  (write-quiz 1)
  (write-quiz 2)
  (write-quiz 3)
  (write-quiz 4))

(defroutes home-routes
  (GET "/" [] (home-page))
  (GET "/about" [] (about-page))
  (GET "/write-quizes" [] (write-quizes))
  (GET "/quiz/:number" [number] (show-quiz number))
  (POST "/submit" [& form] (grade-quiz form)))

