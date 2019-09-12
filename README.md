OMERO LDAP tool
===============

Command line tool for working with OMERO and LDAP

Requirements
============

* OMERO 5.4.x+
* Java 8+

Workflow
========

User lookup
-----------

```
$ omero-ldaptool --help
Usage: <main class> [--debug] [--help] <config> <username>
      <config>     LDAP configuration properties file
      <username>   Username to search for
      --debug      Set logging level to DEBUG
      --help       Display this help and exit
Exception in thread "main" java.lang.NullPointerException
    at com.glencoesoftware.ldaptool.Main.main(Main.java:90)
```

The format of "config" is a standard Java properties file which should at a
minimum include the `omero.db.*` and `omero.ldap.*` configuration
options from your OMERO server.

Development Installation
========================

1. Clone the repository::

    git clone git@github.com:glencoesoftware/omero-ldaptool.git

1. Run the Gradle build and utilize the artifacts as required::

    ./gradlew installDist
    cd build/install
    ...

Running Tests
=============

Using Gradle run the unit tests:

    ./gradlew test

Eclipse Configuration
=====================

1. Run the Gradle Eclipse task::

    ./gradlew eclipse

