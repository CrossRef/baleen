# Baleen

A toolkit to help writing Event Data agents in Clojure. An agent using this framework depends on the following things:

 - the "DOI Destinations" API.
 - a Redis instance.
 - a Lagotto instance.
 - Amazon S3.

Therefore config for all 4 must be supplied. 

## Development

This is a library for use in other projects. For quick testing, the following line will test, compile and install in the local Maven repository.

    lein test && lein uberjar && rm -rf ~/.m2/repository/crossref && lein localrepo install target/baleen-0.1.0-SNAPSHOT-standalone.jar crossref/baleen "0.1"

## License

Copyright © 2016 Crossref

Distributed under the MIT License.
