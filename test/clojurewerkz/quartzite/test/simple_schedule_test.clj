(ns clojurewerkz.quartzite.test.simple-schedule-test
  (:require [clojurewerkz.quartzite.scheduler :as sched]
            [clojurewerkz.quartzite.jobs      :as j]
            [clojurewerkz.quartzite.triggers  :as t]
            [clojurewerkz.quartzite.schedule.calendar-interval :as calin]
            [clojure.test :refer :all]))

;;
;; Group Names operations
;;

(j/defjob NoOpJob
  [ctx])

(defn make-durable-no-op-job
  [name job-group]
  (j/build
   (j/store-durably)
   (j/of-type NoOpJob)
   (j/with-identity name job-group)))

(defn make-no-op-trigger
  [job name job-group]
  (t/build
   (t/start-now)
   (t/with-identity name job-group)
   (t/with-description "descroption")
   (t/for-job job)
   (t/with-schedule (calin/schedule
                     (calin/with-interval-in-hours 4)))))

(deftest test-add-job
  (let [s      (-> (sched/initialize) sched/start)
        job-id "job-in-test-add-job"
        job-group "test-job1"
        job1 (make-durable-no-op-job job-id job-group)
        jk (j/key job-id job-group)]

    (sched/add-job s job1)

    (is (.equals (sched/get-job s jk) job1))
    (is (.equals job1 (sched/get-job s job-group job-id)))
    (is (zero? (count (sched/get-triggers-of-job s jk))))
    (sched/shutdown s)))

(deftest test-add-trigger
  (let [s      (-> (sched/initialize) sched/start)
        job-id "job-in-test-add-job"
        job-group "test-job1"
        job1 (make-durable-no-op-job job-id job-group)
        jk (j/key job-id job-group)]

    (sched/add-job s job1)

    (is (.equals (sched/get-job s jk) job1))
    (is (zero? (count (sched/get-triggers-of-job s jk))))

    (let [trig-id "trigger-in-test-add-trigger"
          trig-group "test-trigger1"
          trig1 (make-no-op-trigger job1 trig-id trig-group)]

      (sched/add-trigger s trig1)

      (let [triggers (sched/get-triggers-of-job s jk)]
        (is (= 1 (count triggers)))
        (is (.equals (first triggers) trig1)))
      (sched/shutdown s))))
