(ns lms-gradesheet.models.quiz
  (:require [lms-gradesheet.layout :as layout]
            [compojure.core :refer :all]
            [clojure.java.io :as io]
            [clojure.data.json :as json]
            [cheshire.core :as cheshire]
            [ring.util.anti-forgery :as af]
            [monger.core :as mg]
            [monger.collection :as mc]
            [monger.result :refer [ok? has-error?]]
            [clj-time.coerce :as c]
            [clj-time.format :as f]
            [clj-time.local :as l]
            [clojure.pprint :refer :all]))

(def conn (mg/connect))
(def db (mg/get-db conn "quiz"))
(def document "document_answers")
(def quiz-document "document_quiz")

(defn read-file
  [file-path]
  (slurp (io/file (io/resource file-path))))

(defn read-exam
  [v]
  (json/read-str (read-file v) :key-fn keyword))

(defn format-time
  [date]
  (def custom-formatter (f/formatter "yyyyMMdd hh:mm:ss"))
  (f/unparse custom-formatter date))

(defn get-time-now
  []
  (format-time (l/local-now)))

(defn shuffle-choices
  [exam]
  (map (fn [x]
         {:c (shuffle (:c x))
          :q (:q x)
          :l (:l x)}) (shuffle (:questions exam))))

(defn check-answer
  [q answers]
  (let [k (keyword (:l q))]
    (= (k answers) (:a q))))

(defn get-grade
  [answers]
  (let [grade (atom 0)
        exam-file (str "exams/example" (:exam-number answers) ".json")
        exam (read-exam exam-file)
        questions (:questions  exam)]
    (for [q questions
      :let [y (check-answer q answers)]
      :when (true? y)]
      y)))

(defn count-grade
  [inp]
  (count (get-grade inp)))

(defn remove-token
  [form]
  (let [result (dissoc form :__anti-forgery-token)]
    result))


(defn record-grade
  ([form]
   (let [result (remove-token form)
         grade-object (merge result {:created (get-time-now)})
         object (merge grade-object {:grade (count-grade grade-object)})]
     (mc/insert-and-return db document object))))

(defn get-quiz
    ([]
    (mc/find-maps db quiz-document { } ))

    ([quiz-number]
     (mc/find-maps db quiz-document { :quiz-number quiz-number } )))

(defn write-quiz
  [quiz]
  (mc/insert-and-return db quiz-document quiz))

;;(mc/update db quiz-document {:quiz-number (:quiz-number quiz)} quiz {:upsert true})
