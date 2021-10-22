# jore4-map-matching

## Overview

Provides a microservice and REST API for matching a shortest path through JORE4 infrastructure network against a given sequence of coordinates. Directionality of road links is taken account. Vehicle type specific turn restrictions are not yet implemented.

Initially, the map matching and navigation services are based on Digiroad infrastructure network but later on the infrastructure network will be supplemented with other sources as well.

The microservice consists of (1) Spring Boot application written in [Kotlin](https://kotlinlang.org/) and (2) an internal PostgreSQL database enabled with [PostGIS](https://postgis.net/) and [pgRouting](https://pgrouting.org/) extensions.

## Running app in production setup

The application is started with:

```
    ./development.sh start
```

The application will be available through http://localhost:3200. The application uses a pre-populated database and is ready for use.

## Running app within development

Within development the application can be started with:

```
    ./development.sh start:dev
```

This avoids rebuilding Docker image every the code is changed. The application is started locally via Maven (not inside a Docker container) but the database dependencies are launched via docker-compose setup.

The application will be available through http://localhost:8080. The database used by the application is initially empty. Hence, no meaningful routing results can be expected (unless some data is populated e.g. from a dump available through [Digiroad import repository](https://github.com/HSLdevcom/jore4-digiroad-import-experiment)).

## How to use API

API NOT YET IMPLEMENTED.

## Building

The application is built with [Maven](https://maven.apache.org/). The application is using [Flyway](https://flywaydb.org/) for database migrations and [jOOQ](https://www.jooq.org/) as query builder.

There exist two Maven profiles: `dev` and `prod`. By default, Maven uses `dev` profile.

In `prod` profile, the database is expected to be already set up with the schema definitions and data. Hence, no database migrations are run during application start.

In `dev` profile, there are two databases used:
-  _Development database_ containing the application data. The database is initially empty. Database migrations are run into the database during application start.
- _Test database_ that is used in tests and within jOOQ metaclass generation. Database migrations are run during Maven build within the `process-resource` lifecycle phase which occurs just before source code compilation. The jOOQ metadata classes are updated in the same lifecycle phase after the migrations.

With `dev` profile one needs to create a user-specific build configuration file e.g. as follows:

```
    touch spring-boot/profiles/dev/config.$(whoami).properties
```

## Development notes

Within each build cycle, the test database is cleaned and re-initialised with database migrations and jOOQ code generation is invoked. This way, the validity of migration scripts is always verified and the jOOQ classes are kept up-to-date.

The development database can be re-initialised (without recreating it) by running:

```
    mvn antrun:run@init-db-properties properties:read-project-properties flyway:clean flyway:migrate
```

Currently, there is a discrepancy between production database and the development/test database with regard to schema arrangement. In production database, **postgis** and **pgrouting** extensions are created into _public_ schema whereas in the development/test database the extensions are created into a separate _extensions_ schema.  Having a separate _extensions_ schema makes it easier to develop the app. This discrepancy does not affect the functioning of the app.

In the development/test database there exists also a _flyway_ schema (for keeping account of database migrations) that is not present in the production database since database migrations are not run in the production setup.

## Docker Reference

- Needs **postgis** and **pgrouting** extensions enabled in the production database.
- Currently, there exist three databases in the docker-compose setup:
    1. One for productional use that is pre-populated with infrastructure network data and pgRouting topology (exported from [digiroad-import repo](https://github.com/HSLdevcom/jore4-digiroad-import-experiment)). In the future, this might get replaced by linking to JORE4 database via FDW (Foreign Data Wrapper extension)
    2. Development database. Initially empty with no schema, tables or data.
    3. Test database. Initially empty like development database. Used within test execution and for generating jOOQ metaclasses.
- When Azure flexible-server enables pgRouting extension, it is considered that the map-matching service could be changed to point directly to the JORE4 database (in production setup) instead of having its own database
- How to load infrastructure network into mapmatchingdb

Ports:

8080

Environment variables:

The application uses Spring Boot which allows overwriting configuration properties as described
[here](https://docs.spring.io/spring-boot/docs/current/reference/html/features.html#features.external-config.typesafe-configuration-properties.relaxed-binding.environment-variables).
The docker container is also able to
[read secrets](https://github.com/HSLdevcom/jore4-tools#read-secretssh) and expose
them as environment variables.

The following configuration properties are to be defined for each environment:

| Config property         | Environment variable    | Secret name      | Example                                                                            | Description                                                                      |
| ----------------------  | ----------------------- | ---------------- | ---------------------------------------------------------------------------------- | -------------------------------------------------------------------------------- |
| -                       | SECRET_STORE_BASE_PATH  | -                | /mnt/secrets-store                                                                 | Directory containing the docker secrets                                          |
| db.url                  | DB_URL                  | db-url           | jdbc:postgresql://jore4-mapmatchingdb:5432/jore4mapmatching?stringtype=unspecified | The JDBC URL of the database containing the routing data (based on Digiroad)     |
|                         | DB_HOSTNAME             | db-hostname      | jore4-mapmatchingdb                                                                | The IP/hostname of the routing database (if DB_URL is not set)                   |
|                         | DB_PORT                 | db-port          | 5432                                                                               | The port of the routing database (if DB_URL is not set)                          |
|                         | DB_DATABASE             | db-database      | jore4mapmatching                                                                   | The name of the routing database (if DB_URL is not set)                          |
| db.username             | DB_USERNAME             | db-username      | mapmatching                                                                        | Username for the routing database                                                |
| db.password             | DB_PASSWORD             | db-password      | ****                                                                               | Password for the routing database                                                |

More properties can be found from `/profiles/prod/config.properties`
