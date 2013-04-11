(ns gh-fork-explorer.core
  (:import (org.apache.lucene.analysis.en EnglishAnalyzer))
  (:require [tentacles.repos :as repos]
            [tentacles.core :as tentacles]
            [tentacles.users :as users]
            [clucy.core :as clucy]
            [clojure.set :as set]))

(def ^:dynamic *tentacles-default-options* {:all-pages true :per-page 100})

(defmacro twd [& body]
  `(tentacles/with-defaults *tentacles-default-options*
     (let [r# ~@body]
       (if (vector? r#)
         (remove empty? r#)
         r#))))

(defonce mforks (memoize repos/forks))

(defrecord GithubCommit
    [sha commit])

(defn mcommits*
  [user repo args]
  (->> (repos/commits user repo args)
      (map map->GithubCommit)))

(defonce mcommits (memoize mcommits*))

(defn commit-document
  [commit]
  (-> commit
      (select-keys [:sha :author :committer :html_url])
      (update-in [:author] get :login)
      (update-in [:committer] get :login)
      (assoc :author-date (get-in commit [:commit :author :date]))
      (assoc :commit-date (get-in commit [:commit :committer :date]))
      (assoc :message (get-in commit [:commit :message]))))

(defn fork-to-user-repo [fork]
  [(get-in fork [:owner :login]) (:name fork)])

(defn forks
  [user repo]
  (map
   (comp 
    #(select-keys % [:url :updated_at :full_name :git_url :owner :size :name])
    #(update-in % [:owner] select-keys [:login :html_url]))
   (twd (mforks user repo))))

(defn commits-by-owner
  "Only return commits by the owner of the given repository."
  ([fork]
     (apply commits-by-owner (fork-to-user-repo fork)))
  ([user repo]
     ;; Tried using relying on GitHub API author filtering, but it
     ;; does not seem to always work as expected so also manually
     ;; filter.
     (filter (fn [e] (= (get-in e [:author :login]) user))
             (twd (mcommits user repo {:author user})))))

(defn community-commits*
  [user repo]
  (->> (forks user repo)
       (map commits-by-owner)
       (remove empty?)))

(defn unmerged?* [user repo commit]
  (-> (twd (repos/compare-commits user repo "master" commit))
      :status
      vector
      (->> (some #{"behind" "identical"}))
      not
      boolean))

(defonce unmerged? (memoize unmerged?*))

(defn unmerged-filter
  [user repo commit-stream]
  (filter (comp (partial unmerged? user repo) :sha first) commit-stream))

(defn community-commits [user repo]
  (->> (community-commits* user repo)
       (unmerged-filter user repo)
       (apply concat)))

(defn newness-filter [user repo]
  identity)

(defn community-commit-documents [user repo]
  (map commit-document (community-commits user repo)))

(defn build-index* [user repo]
  (binding [clucy/*analyzer* (EnglishAnalyzer. clucy/*version*)]
    (let [index (clucy/memory-index)
          documents (community-commit-documents user repo)]
      (doseq [doc documents]
        (clucy/add index doc))
      index)))

(defonce build-index (memoize build-index*))

(defn search [& args]
  (binding [clucy/*analyzer* (EnglishAnalyzer. clucy/*version*)]
    (apply clucy/search args)))

(defn forks-count* [user repo]
  (:forks_count (twd (repos/specific-repo user repo))))

(defonce forks-count (memoize forks-count*))

(defn okay?
  "Return true iff there are a reasonable number of forks to examine."
  [user repo]
  (< (forks-count user repo) 120))

;; Currently unused, but for the future ...

(defonce mbranches (memoize repos/branches))
(defonce mcompare-commits (memoize repos/compare-commits))

(defn branches
  ([fork]
     (apply branches (fork-to-user-repo fork)))
  ([user repo]
     (twd (mbranches user repo))))

(defn community-branches [user repo]
  (let [canonical-branches (set (map :name (branches user repo)))
        fork-branches (map (comp set #(map :name %) branches) (forks user repo))]
    (map #(set/difference % canonical-branches) fork-branches)))

