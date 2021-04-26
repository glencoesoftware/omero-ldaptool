OMERO LDAP tool
===============

Command line tool for working with OMERO and LDAP

Requirements
============

* OMERO 5.6.x+
* Java 8+

Workflow
========

User lookup
-----------

```
$ omero-ldaptool --help
Usage: <main class> [--help] [--log-level=<logLevel>] (--all |
                    --user=<username>) <config>
      <config>            LDAP configuration properties file
      --all               Print all users
      --help              Display this help and exit
      --log-level=<logLevel>
                          Change logging level; valid values are OFF, ERROR,
                            WARN, INFO, DEBUG, TRACE and ALL. (default: WARN)
      --user=<username>   Username to search
```

The format of "config" is a standard Java properties file which should at a
minimum include the `omero.db.*` and `omero.ldap.*` configuration
options from your OMERO server:

* https://docs.openmicroscopy.org/omero/5.6.3/sysadmins/server-ldap.html
* https://docs.openmicroscopy.org/omero/5.6.3/sysadmins/config.html#ldap

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

