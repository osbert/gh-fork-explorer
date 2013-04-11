(ns gh-fork-explorer.handler
  (:use gh-fork-explorer.core
        compojure.core
        compojure.handler
        hiccup.core
        hiccup.page
        hiccup.element
        hiccup.form))

(defprotocol PHTMLRenderable
  (render [obj] "My HTML representation."))

(defn pretty-integer
  ;; NOTE: This is about 4x slower than the regex version, but perhaps
  ;; can be useful in the event there is no regex support, and is sort
  ;; of a neat theoretical implementation.
  ([n sep]
     (-> n
         str
         reverse
         (->> (partition 3 3 nil)
              (map #(clojure.string/join "" %))
              (clojure.string/join sep)
              reverse
              (clojure.string/join ""))))
  ([n]
     (pretty-integer n ",")))


(defn pretty-integer-regex
  [n]
  ;; Regex implementation taken from Rails::ActiveSupport.
  (clojure.string/replace n #"(\d)(?=(\d\d\d)+(?!\d))" "$1,"))

(defprotocol PHTMLTable
  (header [obj] "Sequence of header attribute names.")
  (body [obj] "Sequence of rows."))

(defn table [obj]
  [:table
   [:thead
    [:tr
     (for [x (header obj)]
       [:th x])]]
   [:tbody
    (for [row (body obj)]
      [:tr
       (for [datum row]
         [:td datum])])]
   [:tfoot
    
    ]])

(defn tableize [commits]
  (let [tableized (reify
                    PHTMLTable
                    (header [_] ["Date" "Author" "Message"])
                    (body [_] (map (fn [commit] [(or (get-in commit [:commit :author :date]) (get commit :commit-date))
                                                (or (get-in commit [:commit :author :name]) (get commit :author))
                                                (or (get-in commit [:commit :message]) (get commit :message))])
                                   commits)))]
    (reify
      PHTMLRenderable
      (render [_] (table tableized)))))

(defn main-form [user repo q]
  (form-to [:get "/"]
           (label "user" "Username")
           (text-field "user" user)
           (label "repo" "Repository")
           (text-field "repo" repo)
           (label "q" "Search terms")
           (text-field "q" q)
           (submit-button "Examine Forks")))

(defroutes app-routes
  (GET "/" [user repo q]
       (html5
        [:head
         [:title "GitHub Fork Explorer" (if (and user repo) (format " - %s/%s" user repo))]]
        [:body
         [:h1 "GitHub fork explorer"]
         (main-form user repo q)
         (if (and user repo)
           (if (okay? user repo)
             (if (not (empty? q))
               (-> (build-index user repo)
                   (search q 10)
                   tableize
                   render)
               (-> (community-commits user repo)
                   tableize
                   render))
             (format "Sorry, %s/%s has %s forks (50 max)" user repo (pretty-integer-regex (forks-count user repo)))))])))

(def app
  (-> app-routes
      site))
