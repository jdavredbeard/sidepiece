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

(defn time-block-exists?
  [time-block-hash]
  (let [query-result (d/q {:query '[:find ?c
                                    :keys time-block-eid
                                    :in $ ?time-block-hash
                                    :where
                                    [?c :calendar/time-block-hash-id ?time-block-hash]]
                           :args [(db/db) time-block-hash]})]
    (println query-result)
    (if (= [] query-result)
      false
      true)))

(defn generate-time-block-tx-data
  [calendar musician-id status]
  (let [minute (.get calendar java.util.Calendar/MINUTE)
        hour (.get calendar java.util.Calendar/HOUR_OF_DAY)
        day (.get calendar java.util.Calendar/DAY_OF_MONTH)
        month (.get calendar java.util.Calendar/MONTH)
        year (.get calendar java.util.Calendar/YEAR)
        minute-keyword (get s/minutes-keyword-map (s/get-minutes-key minute))
        hour-keyword (get s/hours-keyword-map (str hour))
        day-keyword (get s/days-keyword-map (str day))
        month-keyword (get s/months-keyword-map (str month))
        year-keyword (get s/years-keyword-map (str year))
        time-block-hash (hash [year-keyword month-keyword day-keyword hour-keyword minute-keyword])
        time-block-map {:calendar/time-block-minute minute-keyword
                        :calendar/time-block-hour hour-keyword
                        :calendar/time-block-day day-keyword
                        :calendar/time-block-month month-keyword
                        :calendar/time-block-year year-keyword 
                        :calendar/time-block-musician-id musician-id
                        :calendar/time-block-status status}]
    (println time-block-hash)
    (if (time-block-exists? time-block-hash)
      (assoc time-block-map :db/id [:calendar/time-block-hash-id time-block-hash])
      (assoc time-block-map :calendar/time-block-hash-id time-block-hash))))


(defn transform-calendar-to-time-block-keywords
  [calendar]
  (let [minute (.get calendar java.util.Calendar/MINUTE)
        hour (.get calendar java.util.Calendar/HOUR_OF_DAY)
        day (.get calendar java.util.Calendar/DAY_OF_MONTH)
        month (.get calendar java.util.Calendar/MONTH)
        year (.get calendar java.util.Calendar/YEAR)
        minute-keyword (get s/minutes-keyword-map (s/get-minutes-key minute))
        hour-keyword (get s/hours-keyword-map (str hour))
        day-keyword (get s/days-keyword-map (str day))
        month-keyword (get s/months-keyword-map (str month))
        year-keyword (get s/years-keyword-map (str year))
        time-block-map {:calendar/time-block-minute minute-keyword
                        :calendar/time-block-hour hour-keyword
                        :calendar/time-block-day day-keyword
                        :calendar/time-block-month month-keyword
                        :calendar/time-block-year year-keyword}]
    time-block-map))

(defn generate-time-block-tx-data-for-block-count
  [timestamp-start block-count musician-id status]
  (map
   (fn [count] (let [calendar (i/read-instant-calendar timestamp-start)]
                 (.add calendar java.util.Calendar/MINUTE (* count 30)) 
                 (generate-time-block-tx-data calendar musician-id status)))
   (range block-count)))

(defn add-block-with-status-for-musician-id
  [timestamp-start block-count musician-id block-status]
  (let [block-data (generate-time-block-tx-data-for-block-count
                    timestamp-start
                    block-count
                    musician-id
                    block-status)]
    (db/transact-all db/conn [block-data])))

(defn add-availability-block-for-musician-id
  [timestamp-start block-count musician-id]
  (add-block-with-status-for-musician-id
   timestamp-start
   block-count
   musician-id
   :calendar/time-block-available))

(defn add-booked-block-for-musician-id
  [timestamp-start block-count musician-id]
  (add-block-with-status-for-musician-id
   timestamp-start
   block-count
   musician-id
   :calendar/time-block-booked))

(defn add-pending-block-for-musician-id
  [timestamp-start block-count musician-id]
  (add-block-with-status-for-musician-id
   timestamp-start
   block-count
   musician-id
   :calendar/time-block-pending))

(defn find-musician-ids-available-at-timestamp
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
  (add-availability-block-for-musician-id 
   "2025-04-12T23:20:50.52Z" 
   5 
   #uuid "288ffd20-970d-45a7-b728-80e905da612e"))

;; (h/add-booked-block-for-musician-id
;;  "2025-04-12T23:20:50.52Z"
;;  5
;;  #uuid "288ffd20-970d-45a7-b728-80e905da612e")

