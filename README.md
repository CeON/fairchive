Dataverse&#174; 
===============

Dataverse is an [open source][] web application for sharing, citing, analyzing, and preserving research data (developed by the [Data Science and Products team](http://www.iq.harvard.edu/people/people/data-science-products) at the [Institute for Quantitative Social Science](http://iq.harvard.edu/) and the [Dataverse community][]).

[dataverse.org][] is our home on the web and shows a map of Dataverse installations around the world, a list of [features][], [integrations][] that have been made possible through [REST APIs][], our development [roadmap][], and more.

We maintain a demo site at [demo.dataverse.org][] which you are welcome to use for testing and evaluating Dataverse.

To install Dataverse, please see our [Installation Guide][] which will prompt you to download our [latest release][].

To discuss Dataverse with the community, please join our [mailing list][], participate in a [community call][], chat with us at [chat.dataverse.org][], or attend our annual [Dataverse Community Meeting][].

We love contributors! Please see our [Contributing Guide][] for ways you can help.

Dataverse is a trademark of President and Fellows of Harvard College and is registered in the United States.

[![Dataverse Project logo](src/main/webapp/resources/images/dataverseproject_logo.jpg?raw=true "Dataverse Project")](http://dataverse.org)

[![Coverage Status](https://coveralls.io/repos/github/CeON/dataverse/badge.svg?branch=develop)](https://coveralls.io/github/CeON/dataverse?branch=develop)

[dataverse.org]: https://dataverse.org
[demo.dataverse.org]: https://demo.dataverse.org
[Dataverse community]: https://dataverse.org/developers
[Installation Guide]: http://guides.dataverse.org/en/latest/installation/index.html
[latest release]: https://github.com/IQSS/dataverse/releases
[features]: https://dataverse.org/software-features
[roadmap]: https://dataverse.org/goals-roadmap-and-releases
[integrations]: https://dataverse.org/integrations
[REST APIs]: http://guides.dataverse.org/en/latest/api/index.html
[Contributing Guide]: CONTRIBUTING.md
[mailing list]: https://groups.google.com/group/dataverse-community
[community call]: https://dataverse.org/community-calls
[chat.dataverse.org]: http://chat.dataverse.org
[Dataverse Community Meeting]: https://dataverse.org/events
[open source]: LICENSE.md

# Building

Build dataverse with running all the tests (unit and integration tests):

```bash
$ ./mvnw clean install
```

Build dataverse without running tests:

```bash
$ ./mvnw clean install -DskipTests -Ddocker.skip
```

# Development environment

The recommended development environment is based on docker. The first time the dev environment needs to be installed with:

```bash
$ ./dev install
```

This will create all the required docker images and containers and run the dataverse installer. Installed services and their ports:

* smtp, UI: 8025 smtp: 1025
* postgres: 5432
* solr: 8983
* keycloak: 7070 (admin dns: local.admin.keycloak non-admin: local.keycloak)
* glassfish:
  * 8080: Dataverse
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
$ # re-deploy dataverse (uses the war located in dataverse-dist/target/dist)
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
./mvnw verify -Dit.test=UserNotificationRepositoryIT -pl dataverse-persistence
```


Integration test dependencies can be started manually in order to execute integration tests through the IDE:

```bash
./mvnw docker:start -pl dataverse-webapp
```

Once started, all the integration tests can be run through the IDE. When finished, containers can be stopped with:

```bash
./mvnw docker:stop -pl dataverse-webapp
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