(ns sidepiece.db.schema) 

(defn create-keywords-in-space
  [keyword-space ref-names]
  (map
   (fn [ref-name]
     (keyword (str keyword-space "/" ref-name)))
   ref-names))

(defn create-refs-in-keyword-space
  [keyword-vec]
  (map
   (fn [keyword]
     (assoc {} :db/ident keyword))
   keyword-vec))



(def years-keyword-vec
  (create-keywords-in-space "year" ["twenty-twenty-two"
                                    "twenty-twenty-three"
                                    "twenty-twenty-four"
                                    "twenty-twenty-five"
                                    "twenty-twenty-six"]))
(def years-ref-vec
  (create-refs-in-keyword-space years-keyword-vec))

(def years-keyword-map
  (zipmap ["2022" "2023" "2024" "2025" "2026"] years-keyword-vec))

(def months-keyword-vec
  (create-keywords-in-space "month" ["january"
                                     "february"
                                     "march"
                                     "april"
                                     "may"
                                     "june"
                                     "july"
                                     "august"
                                     "september"
                                     "october"
                                     "november"
                                     "december"]))

(def months-ref-vec
  (create-refs-in-keyword-space months-keyword-vec))

(def months-keyword-map
  (zipmap (map str (range 12)) months-keyword-vec))

(def days-keyword-vec
  (create-keywords-in-space "day" ["one"
                                   "two"
                                   "three"
                                   "four"
                                   "five"
                                   "six"
                                   "seven"
                                   "eight"
                                   "nine"
                                   "ten"
                                   "eleven"
                                   "twelve"
                                   "thirteen"
                                   "fourteen"
                                   "fifteen"
                                   "sixteen"
                                   "seventeen"
                                   "eighteen"
                                   "nineteen"
                                   "twenty"
                                   "twenty-one"
                                   "twenty-two"
                                   "twenty-three"
                                   "twenty-four"
                                   "twenty-five"
                                   "twenty-six"
                                   "twenty-seven"
                                   "twenty-eight"
                                   "twenty-nine"
                                   "thirty"
                                   "thirty-one"]))

(def days-ref-vec
  (create-refs-in-keyword-space days-keyword-vec))

(def days-keyword-map
  (zipmap (map str (range 1 32)) days-keyword-vec))

(def hours-keyword-vec
  (create-keywords-in-space "hour" ["zero"
                                    "one"
                                    "two"
                                    "three"
                                    "four"
                                    "five"
                                    "six"
                                    "seven"
                                    "eight"
                                    "nine"
                                    "ten"
                                    "eleven"
                                    "twelve"
                                    "thirteen"
                                    "fourteen"
                                    "fifteen"
                                    "sixteen"
                                    "seventeen"
                                    "eighteen"
                                    "nineteen"
                                    "twenty"
                                    "twenty-one"
                                    "twenty-two"
                                    "twenty-three"]))

(def hours-ref-vec
  (create-refs-in-keyword-space hours-keyword-vec))

(def hours-keyword-map
  (zipmap (map str (range 24)) hours-keyword-vec))

(def minutes-keyword-vec
  (create-keywords-in-space "minute" ["zero" "thirty"]))

(def minutes-ref-vec
  (create-refs-in-keyword-space minutes-keyword-vec))

(def minutes-keyword-map
  (zipmap ["0" "30"] minutes-keyword-vec))



(defn get-minutes-key
  [minutes]
  (if (< minutes 30)
    "0"
    "30"))

(def init-schema
  [;; musician-schema
   [{:db/ident :musician/id
     :db/valueType :db.type/uuid
     :db/unique :db.unique/identity
     :db/cardinality :db.cardinality/one}
    {:db/ident :musician/name
     :db/valueType :db.type/string
     :db/cardinality :db.cardinality/one}
    {:db/ident :musician/email
     :db/valueType :db.type/string
     :db/cardinality :db.cardinality/one}
    {:db/ident :musician/instrument
     :db/valueType :db.type/ref
     :db/cardinality :db.cardinality/many}
    {:db/ident :musician/booking-id
     :db/valueType :db.type/uuid
     :db/cardinality :db.cardinality/many}
    {:db/ident :musician/booked-id
     :db/valueType :db.type/uuid
     :db/cardinality :db.cardinality/many}]

    ;;booking-schema
   [{:db/ident :calendar/booking-start
     :db/valueType :db.type/instant
     :db/cardinality :db.cardinality/one}
    {:db/ident :calendar/booking-end
     :db/valueType :db.type/instant
     :db/cardinality :db.cardinality/one}
    {:db/ident :calendar/booking-id
     :db/valueType :db.type/uuid
     :db/cardinality :db.cardinality/one
     :db/unique :db.unique/identity}
    {:db/ident :calendar/booking-location
     :db/valueType :db.type/string
     :db/cardinality :db.cardinality/one}
    {:db/ident :calendar/booking-name
     :db/valueType :db.type/string
     :db/cardinality :db.cardinality/one}
    {:db/ident :calendar/booked-by-musician-id
     :db/valueType :db.type/uuid
     :db/cardinality :db.cardinality/one}
    {:db/ident :calendar/booked-musician-ids
     :db/valueType :db.type/uuid
     :db/cardinality :db.cardinality/many}]

   ;; time block schema
   [{:db/ident :calendar/time-block-year
     :db/valueType :db.type/ref
     :db/cardinality :db.cardinality/one}
    {:db/ident :calendar/time-block-month
     :db/valueType :db.type/ref
     :db/cardinality :db.cardinality/one}
    {:db/ident :calendar/time-block-day
     :db/valueType :db.type/ref
     :db/cardinality :db.cardinality/one}
    {:db/ident :calendar/time-block-hour
     :db/valueType :db.type/ref
     :db/cardinality :db.cardinality/one}
    {:db/ident :calendar/time-block-minute
     :db/valueType :db.type/ref
     :db/cardinality :db.cardinality/one}
    {:db/ident :calendar/time-block-status
     :db/valueType :db.type/ref
     :db/cardinality :db.cardinality/one}
    {:db/ident :calendar/time-block-musician-id
     :db/valueType :db.type/uuid
     :db/cardinality :db.cardinality/one}]

   (create-refs-in-keyword-space
    (create-keywords-in-space "calendar" ["time-block-booked"
                                          "time-block-available"
                                          "time-block-pending"]))
   ;; time refs 
   years-ref-vec
   months-ref-vec
   days-ref-vec
   hours-ref-vec
   minutes-ref-vec

    ;;instrument-schema
   (create-refs-in-keyword-space
    (create-keywords-in-space "instrument" ["steel-string-acoustic-guitar"
                                            "nylon-string-acoustic-guitar"
                                            "electric-guitar"
                                            "lapsteel"
                                            "pedal-steel"
                                            "voice"
                                            "kazoo"
                                            "trumpet"
                                            "trombone"
                                            "clarinet"
                                            "sousaphone"
                                            "tuba"
                                            "flugelhorn"
                                            "electric-bass"
                                            "double-bass"
                                            "piano"
                                            "organ"
                                            "electric-keyboard"
                                            "synth"
                                            "drums"
                                            "percussion"
                                            "alto-saxophone"
                                            "tenor-saxophone"
                                            "soprano-saxophone"
                                            "baritone-saxophone"
                                            "bass-saxophone"]))])

(def init-musicians
  [[{:musician/id (java.util.UUID/randomUUID)
     :musician/name "Bob Feeley"
     :musician/email "bobfeeley@example.com"
     :musician/instrument [:instrument/alto-saxophone :instrument/electric-bass]}]
   [{:musician/id (java.util.UUID/randomUUID)
     :musician/name "Sue Robinson"
     :musician/email "suerobinson@example.com"
     :musician/instrument [:instrument/alto-saxophone :instrument/drums :instrument/synth]}]])