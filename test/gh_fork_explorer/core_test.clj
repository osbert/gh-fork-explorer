(ns gh-fork-explorer.core-test
  (:use clojure.test
        gh-fork-explorer.core
        ring.mock.request)
  (:require [gh-fork-explorer.handler :as web]))

(deftest app-routes-test
  (testing "/"
    (let [response (web/app (request :get "/"))]
      (is (= (:status response) 200)))))
