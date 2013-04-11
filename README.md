# gh-fork-explorer

A Clojure library designed to explore GitHub forks for commits that
have not yet been merged into the canonical master, do not have an
associated pull request, or do not have an issue filed. 

## Usage 

```clojure
;; Add to project.clj
:dependencies [[osbert/gh-fork-explorer "0.1.0"]]

;; NOTE: This example is pretty useless because there are no forks.

(use 'gh-fork-explorer.core)

;; Build a search index for all forks on osbert/gh-fork-explorer
(defonce lb-index (build-index "osbert" "gh-fork-explorer"))

;; Search through this index looking for commits that mention "tentacle"
(search lb-index "tentacle" 10)
```

## License

Copyright © 2013 Osbert Feng

Distributed under the Eclipse Public License, the same as Clojure.
