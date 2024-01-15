OMERO LDAP tool
===============

Command line tool for working with OMERO and LDAP

Requirements
============

* OMERO 5.6.x+
* Java 11+

Workflow
========

For all commands, the format of the LDAP configuration file specified by `--config`
is a standard Java properties file which should at a minimum include
the `omero.db.*` and `omero.ldap.*` configuration options from your OMERO server:

* https://omero.readthedocs.io/en/stable/sysadmins/server-ldap.html
* https://omero.readthedocs.io/en/stable/sysadmins/config.html#ldap


User lookup
-----------

```
$ omero-ldaptool search -h
Usage: omero-ldaptool search [-hV] --config=<config> [--log-level=<logLevel>]
                             (--all | --user=<username> [--user=<username>]...)
      --all               Print all users
      --config=<config>   LDAP configuration properties file
  -h, --help              Show this help message and exit.
      --log-level=<logLevel>
                          Change logging level; valid values are OFF, ERROR,
                            WARN, INFO, DEBUG, TRACE and ALL. (default: WARN)
      --user=<username>   Username to search
  -V, --version           Print version information and exit.
```


Password check
--------------

```
$ omero-ldaptool password -h
Usage: omero-ldaptool password [-hV] --config=<config> [--log-level=<logLevel>]
                               [--tries=<tries>] <dn>
      <dn>                DN to check password for
      --config=<config>   LDAP configuration properties file
  -h, --help              Show this help message and exit.
      --log-level=<logLevel>
                          Change logging level; valid values are OFF, ERROR,
                            WARN, INFO, DEBUG, TRACE and ALL. (default: WARN)
      --tries=<tries>     Number of times to retry the password check (default:
                            1)
  -V, --version           Print version information and exit.
```


Development Installation
========================

1. Clone the repository::

        git clone https://github.com/glencoesoftware/omero-ldaptool.git

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

