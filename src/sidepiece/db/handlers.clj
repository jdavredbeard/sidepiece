(ns sidepiece.db.handlers
  (:require [sidepiece.db.schema :as s]
            [sidepiece.db.db :as db]
            [datomic.client.api :as d]
            [clojure.instant :as i]))

(defn get-query-for-musician-id-by-key-and-value
  [key value]
  [:find '?musician-id
   :where
   ['?e :musician/id '?musician-id]
   ['?e key value]])

(defn get-musician-ids-by-name
  [name db]
  (conj
   (first
    (d/q
     (get-query-for-musician-id-by-key-and-value :musician/name name)
     (db)))))

(defn transform-calendar-to-time-block-keywords
  [calendar]
  (let [minute (.get calendar java.util.Calendar/MINUTE)
        hour (.get calendar java.util.Calendar/HOUR_OF_DAY)
        day (.get calendar java.util.Calendar/DAY_OF_MONTH)
        month (.get calendar java.util.Calendar/MONTH)
        year (.get calendar java.util.Calendar/YEAR)
        time-block-map {:calendar/time-block-minute (get s/minutes-keyword-map (s/get-minutes-key minute))
                        :calendar/time-block-hour (get s/hours-keyword-map (str hour))
                        :calendar/time-block-day (get s/days-keyword-map (str day))
                        :calendar/time-block-month (get s/months-keyword-map (str month))
                        :calendar/time-block-year (get s/years-keyword-map (str year))}]
    time-block-map))

(defn generate-time-blocks-for-timestamp-range-for-musician-id-with-status
  [timestamp-start block-count musician-id status]
  (map
   (fn [count] (let [calendar (i/read-instant-calendar timestamp-start)]
                 (.add calendar java.util.Calendar/MINUTE (* count 30))
                 (assoc
                  (transform-calendar-to-time-block-keywords calendar)
                  :calendar/time-block-musician-id musician-id
                  :calendar/time-block-status status)))
   (range block-count)))

(defn add-availability-block-for-musician-id
  [timestamp-start block-count musician-id]
  (let [block-data (generate-time-blocks-for-timestamp-range-for-musician-id-with-status
                    timestamp-start
                    block-count
                    musician-id
                    :calendar/time-block-available)]
    (db/transact-all db/conn [block-data])))

(defn find-musician-ids-available-at-timestamp-5
  [timestamp]
  (let [timestamp-block (transform-calendar-to-time-block-keywords (i/read-instant-calendar timestamp))]
    (d/q {:query '[:find ?musician-id
                   :keys musician-id
                   :in $ ?block-year ?block-month ?block-day ?block-hour ?block-minute
                   :where
                   [?c :calendar/time-block-musician-id ?musician-id]
                   [?c :calendar/time-block-year ?block-year]
                   [?c :calendar/time-block-month ?block-month]
                   [?c :calendar/time-block-day ?block-day]
                   [?c :calendar/time-block-hour ?block-hour]
                   [?c :calendar/time-block-minute ?block-minute]
                   [?c :calendar/time-block-status :calendar/time-block-available]]
          :args [(db/db)
                 (:calendar/time-block-year timestamp-block)
                 (:calendar/time-block-month timestamp-block)
                 (:calendar/time-block-day timestamp-block)
                 (:calendar/time-block-hour timestamp-block)
                 (:calendar/time-block-minute timestamp-block)]})))

(defn get-availability-for-musician-id
  [musician-id]
  (d/q {:query '[:find ?year-keyword ?month-keyword ?day-keyword ?hour-keyword ?minute-keyword
                 :keys year month day hour minute
                 :in $ ?musician-id
                 :where
                 [?c :calendar/time-block-musician-id ?musician-id]
                 [?c :calendar/time-block-year ?block-year]
                 [?block-year :db/ident ?year-keyword]
                 [?c :calendar/time-block-month ?block-month]
                 [?block-month :db/ident ?month-keyword]
                 [?c :calendar/time-block-day ?block-day]
                 [?block-day :db/ident ?day-keyword]
                 [?c :calendar/time-block-hour ?block-hour]
                 [?block-hour :db/ident ?hour-keyword]
                 [?c :calendar/time-block-minute ?block-minute]
                 [?block-minute :db/ident ?minute-keyword]
                 [?c :calendar/time-block-status :calendar/time-block-available]]
        :args [(db/db) musician-id]}))

;; (defn generate-booking-id-assertion-for-musician-ids
;;   [musician-ids booking-id]
;;   (reduce
;;    (fn [assertion musician-id]
;;      (conj assertion {:musician/id musician-id :musician/booking-id booking-id}))
;;    []
;;    musician-ids))

(defn add-booking
  [booking]
  (let [booking-id (java.util.UUID/randomUUID)
        booking-calendar-schema (assoc booking :calendar/booking-id booking-id)
        ;; musician-booking-id-assertion (generate-booking-id-assertion-for-musician-ids (:calendar/booked-musician-ids booking) booking-id)
        ]
    (db/transact-all db/conn [[booking-calendar-schema] 
                            ;;   musician-booking-id-assertion
                              ])
    booking-id))


(def bookings
  [;;bob's gigs
   {:calendar/booking-start (i/read-instant-date "1985-04-12T23:20:50.52Z")
    :calendar/booking-end (i/read-instant-date "1985-04-12T23:50:50.52Z")
    :calendar/booking-name "Bob's First Gig!"
    :calendar/booked-musician-ids (get-musician-ids-by-name "Bob Feeley" db/db)}
   {:calendar/booking-start (i/read-instant-date "1985-05-12T20:00:00.00Z")
    :calendar/booking-end (i/read-instant-date "1985-05-12T23:00:00.00Z")
    :calendar/booking-name "Bob's Second Gig!"
    :calendar/booked-musician-ids (get-musician-ids-by-name "Bob Feeley" db/db)}])


(defn add-init-bookings
  []
  (map add-booking bookings)
  (add-availability-block-for-musician-id "2025-04-12T23:20:50.52Z" 5 #uuid "288ffd20-970d-45a7-b728-80e905da612e"))