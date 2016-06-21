lein test && lein uberjar && rm -rf ~/.m2/repository/crossref && lein localrepo install target/baleen-0.1.0-SNAPSHOT-standalone.jar crossref/baleen "0.1"
