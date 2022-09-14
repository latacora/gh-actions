#!/usr/bin/env bb

#_{:clj-kondo/ignore [:namespace-name-mismatch]}
(ns latacora.github-actions.workflows.version
  (:require [clojure.java.shell :as shell]
            [clojure.string :as str])
  (:import [java.time Instant ZoneId]
           java.time.format.DateTimeFormatter))


(def date-str
  (-> (DateTimeFormatter/ofPattern "yyyyMMdd")
      (.withZone (ZoneId/of "America/Chicago"))
      (.format (Instant/now))))


(defn git
  [& args]
  (let [proc (apply shell/sh (cons "git" args))]
    (if (zero? (:exit proc))
      (str/trim (:out proc))
      (do (println (:err proc))
          (System/exit 1)))))


(defn -main
  [& _args]
  (let [branch-name (git "rev-parse" "--abbrev-ref" "HEAD")
        short-hash (git "rev-parse" "--short" "HEAD")
        untracked-files? (not (str/blank? (git "ls-files" "--others" "--exclude-standard" ".")))
        changed-files? (not (str/blank? (git "diff" "--name-only")))
        staged-files? (not (str/blank? (git "diff" "--cached" "--name-only")))
        worktree-clean? (not (or untracked-files? changed-files? staged-files?))
        version-str (format "%s-%s-%s%s"
                            date-str
                            branch-name
                            short-hash
                            (if worktree-clean? "" "-UNCLEAN"))]
    (when-not worktree-clean?
      (.println *err* (str "worktree unclean:"
                           (when untracked-files? " untracked files;")
                           (when changed-files? " changed files;")
                           (when staged-files? " staged files;"))))
    (print version-str)))


(when (= *file* (System/getProperty "babashka.file"))
  (apply -main *command-line-args*))
