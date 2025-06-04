Fairchive&#174; 
===============

Fairchive is an [open source][] web application for sharing, citing, analyzing, and preserving FAIR research data 
developed by the [ICM development team][] at the [University of Warsaw][].

It is a detached fork from [IQSS Dataverse][] developed by the [Data Science and Products team](http://www.iq.harvard.edu/people/people/data-science-products) at 
the [Institute for Quantitative Social Science](http://iq.harvard.edu/) and the [Dataverse community][]).

[![Coverage Status](https://coveralls.io/repos/github/CeON/dataverse/badge.svg?branch=develop)](https://coveralls.io/github/CeON/dataverse?branch=develop)

[University of Warsaw]: https://en.uw.edu.pl
[ICM development team]: https://devs.icm.edu.pl
[IQSS Dataverse]: https://github.com/IQSS/dataverse
[Dataverse community]: https://dataverse.org/developers
[open source]: LICENSE.md

# Building

Build fairchive with running all the tests (unit and integration tests):

```bash
$ ./mvnw clean install
```

Build fairchive without running tests:

```bash
$ ./mvnw clean install -DskipTests -Ddocker.skip
```

# Development environment

The recommended development environment is based on docker. The first time the dev environment needs to be installed with:

```bash
$ ./dev install
```

This will create all the required docker images and containers and run the fairchive installer. Installed services and their ports:

* smtp, UI: 8025 smtp: 1025
* postgres: 5432
* solr: 8983
* keycloak: 7070 (admin dns: local.admin.keycloak non-admin: local.keycloak)
* glassfish:
  * 8080: Fairchive
  * 4848: Admin console
  * 9009: Debug

Once installed the environment can be started and used as follows:

```bash
$ ./dev start
$ # restart the whole environment
$ ./dev restart
$ # restart glassfish service
$ ./dev restart glassfish
$ # show service logs
$ ./dev logs glassfish
$ # show solr logs
$ ./dev logs solr
$ # stop the environment
$ ./dev stop
$ # show environment help
$ ./dev help
```

To operate with the glassfish application server:

```bash
$ # start the glassfish server
$ ./dev glassfish start
$ # list all the deployed applications
$ ./dev glassfish apps
$ # re-deploy dataverse (uses the war located in fairchive-dist/target/dist)
$ ./dev glassfish redeploy
$ # show glassfish commands 
$ ./dev glassfish help
```

Glassfish is started in debug mode by default. You can connect to it with the IDE with: `-agentlib:jdwp=transport=dt_socket,server=y,suspend=n,address=9009`

# Running integration tests

All tests:

```bash
./mvnw verify
```

Single test:

```bash
./mvnw verify -Dit.test=UserNotificationRepositoryIT -pl fairchive-persistence
```


Integration test dependencies can be started manually in order to execute integration tests through the IDE:

```bash
./mvnw docker:start -pl fairchive-webapp
```

Once started, all the integration tests can be run through the IDE. When finished, containers can be stopped with:

```bash
./mvnw docker:stop -pl fairchive-webapp
```

# CI builds

CI builds can be configured with the provided [Jenkinsfile](Jenkinsfile). It defines the following 6 stages:

* Prepare: Just creates the image used for the build. It is based on openjdk:8u342-jdk and has been adapted to run on jenkins (jenkins user has been added)
* Build: Performs clean build of the checked out branch
* Unit tests: Executes the unit tests and collects the results
* Integration tests: Executes the integration tests and collects the results. Required docker containers (solr & postgres) are started and it's made sure that they're in the same network as the container running the tests 
* Deploy: Publishes the snapshot artifacts to artifactory. This usually should only be performed on develop, but there's an option to override the check and let it execute also on other branches. 
* Release: Perform the release of the current development version. The next development version will be set according to the nextDevVersion parameter (default is patch). If current version is 1.0.3-SNAPSHOT the next dev version will be:
  - patch: 1.0.4-SNAPSHOT 
  - minor: 1.1.0-SNAPSHOT 
  - major: 2.0.0-SNAPSHOT

All stages are parameterized and can be skipped as desired for a given jenkins job.
