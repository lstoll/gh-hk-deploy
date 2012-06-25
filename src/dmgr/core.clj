(ns dmgr.core
  (:use compojure.core
        lstoll.utils
        ring.util.serve
        ring.adapter.jetty)
  (:require [compojure.route :as route]
            [compojure.handler :as handler]
            [clojure.java.io :as io]
            [clojure.string :as str]
            [conch.core])
  (:gen-class))

(defn expand-path [path]
  (.getCanonicalPath (java.io.File. path)))


(defn run
  [app-path cmd]
  (let [exec ["sh" "-c" cmd :env (merge (into {} (System/getenv))
                                        {"GIT_SSH" (str (expand-path ".") "/bin/git-ssh")
                                         "SSH_KEY" (str app-path "/.ssh/id_rsa")})]
        proc (apply conch.core/proc exec)]
    (future (conch.core/stream-to :out proc *out*))
    (future (conch.core/stream-to :err proc *out*))
    (conch.core/exit-code proc)))

;; Dont format functions like this at home, kids.
(defn deploy
  [app]
  ;; Set up the env
  (let [app-path (expand-path app)]
    (when-not (.isDirectory (io/file app))
      (.mkdir (io/file app))
      (.mkdir (io/file (str app-path "/.ssh")))
      (spit (str app-path "/.ssh/id_rsa") (env (str app "_SSH_KEY"))))
    ;; git clone the repo if it's not there, else update it
    (if (= 0 (if (.isDirectory (io/file (str app "/repo")))
               (do
                 (log "Starting repo update")
                 (run app-path (str "cd " app "/repo && git fetch && git reset --hard origin/master")))
               (do
                 (log "Starting repo clone")
                 (run app-path (str "cd " app " && git clone " (env (str app "_GITHUB_REPO")) " repo")))))
      ;; git push it to heroku
      (do
        (log "Pushing repo to Heroku")
        (run app-path (str "cd " app "/repo && git push -f " (env (str app "_HEROKU_REPO")) " master"))
        (println "... Finished")))))

(defn valid-key?
  [key]
  (= key (env "ACCESS_KEY" "SETME")))

(defroutes main-routes
  (GET "/" [] "<h1>Nothing to see here, move along</h1>")
  (POST "/deploy" {{app :app key :key} :params} (if (valid-key? key)
                                                  (do (future (deploy app)) "OK")
                                                  {:status 403 :body "DENIED"}))
  (route/not-found "<h1>FOUR-OH-FOUR!</h1>"))

(def app
  (handler/site main-routes))
;; (serve-headless app)

(defn -main [] (run-jetty #'app {:port (Integer/parseInt (env "PORT" "5000")) :join true}))