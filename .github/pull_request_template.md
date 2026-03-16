Thanks for submitting your Pull Request!

The following environmental variables can be modified to suit your needs:

WILDFLY_RELEASE_VERSION:
description: the version of WildFly to checkout.'

* NARAYANA_REPO:
  * description: the git repo to use for Narayana

* NARAYANA_BRANCH:
  * description: the git branch to checkout for Narayana

* NY_BRANCH:
  * description: Narayana PR branch to test'

* OVERRIDE_NARAYANA_VERSION:
  * description: override the Narayana version'

* JMHARGS:
  * description: JMH benchmark arguments'

* THREAD_COUNTS:
  * description: thread counts for benchmarks'

* COMPARE_STORES:
  * description: run store benchmarks (y/n)'

* COMPARE_IMPLEMENTATIONS:
  * description: run implementation comparison benchmarks (y/n)'

* COMPARE_TRANSPORTS:
  * description: run transport comparison benchmarks (y/n)'

* COMPARE_JOURNAL_PARAMETERS:
  * description: run journal parameter benchmarks (y/n)'

* APP_SERVER_ZIP_LOCATION:
  * description: URL of the app

Pull requests build and run with one of the following JDKs: JDK17, JDK21, and JDK25. If the JDK version is omitted, JDK25 will be used.
