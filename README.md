# jore4-map-matching

## Overview

Provides a microservice and REST API for matching a shortest path through JORE4 infrastructure network against a given sequence of coordinates. Directionality of road links is taken account.

Vehicle mode specific link selection is not yet implemented even though the support for it exists already in the data model. Vehicle type specific turn restrictions are not yet implemented either.

Initially, the map matching and navigation services are based on Digiroad infrastructure network but later on the infrastructure network will be supplemented with other infrastructure sources as well.

The microservice consists of:
1. Spring Boot application written in [Kotlin](https://kotlinlang.org/)
2. PostgreSQL database enabled with [PostGIS](https://postgis.net/) and [pgRouting](https://pgrouting.org/) extensions

## Running app in production setup

The application is started with:

```sh
./development.sh start
```

The application will be available through http://localhost:3200. The application uses a pre-populated database and is ready for use.

## Running app within development

Within development the application can be started with:

```sh
./development.sh start:dev
```

This avoids rebuilding Docker image every the code is changed. The application is started locally via Maven (not inside a Docker container) but the database dependencies are launched via docker-compose setup.

The application will be available through http://localhost:8080. The database used by the application is initially empty. Hence, no meaningful routing results can be expected (unless some data is populated e.g. from a dump available through [Digiroad import repository](https://github.com/HSLdevcom/jore4-digiroad-import)).

## How to use API

API NOT YET IMPLEMENTED.

## Building

The application is built with [Maven](https://maven.apache.org/). The application is using [Flyway](https://flywaydb.org/) for database migrations and [jOOQ](https://www.jooq.org/) as query builder.

There exist two Maven profiles: `dev` and `prod`. By default, Maven uses `dev` profile.

In `prod` profile, the database is expected to be already set up with the schema definitions and data. Hence, no database migrations are run during application start.

In `dev` profile, there are two databases used:
-  _Development database_ containing the application data. The database is initially empty. Database migrations are run into the database during application start.
- _Test database_ that is used in tests and within jOOQ code generation. Database migrations are run during Maven build within the `process-resource` lifecycle phase which occurs just before source code compilation. The jOOQ classes are updated in the same lifecycle phase after the migrations.

With `dev` profile one needs to create a user-specific build configuration file e.g. as follows:

```sh
touch spring-boot/profiles/dev/config.$(whoami).properties
```

## Populating data within development

Within development, the currently recommended way of importing infrastructure data is to:
1. Start map-matching server with the commands below. Within launch, the server will run database migration scripts into the development database. The scripts will initialise schemas, tables, constraints and indices. Also a couple of enumeration tables are populated with a fixed set of data.

    ```sh
    ./development.sh start:dev
    ```
2. Populate data (infrastructure links, infrastructure sources, network topology and associations of links to vehicle types) from [Digiroad import repository](https://github.com/HSLdevcom/jore4-digiroad-import). This does not involve creating tables neither populating enumeration tables (which already contain data coming from the migration scripts).

To generate a Digiroad-based dump, issue the following commands in the Digiroad import repository:

```sh
./build_docker_image.sh
./import_digiroad_shapefiles.sh
./export_routing_schema.sh
```

To restore table data from the dump (generated into `workdir/pgdump` directory), issue the following command (with `<date>` placeholder replaced with a proper value):

```sh
pg_restore -1 -a --use-list=digiroad_r_routing_<date>.pgdump.no-enums.only-links.list -h localhost -p 18000 -d jore4mapmatching -U mapmatching digiroad_r_routing_<date>.pgdump
```

The list argument passed to `pg_restore` command will constrain the restoration of the dump file to data of selected tables only. Hence, enumeration tables are excluded as well as create table statements.

## Development notes

Within each build cycle, the test database is cleaned and re-initialised with database migrations and jOOQ code generation is invoked. This way, the validity of migration scripts is always verified and the jOOQ classes are kept up-to-date.

The development database can be re-initialised (without recreating it) by running:

```sh
mvn properties:read-project-properties flyway:clean flyway:migrate
```

Currently, there is a discrepancy between production database and the development/test database with regard to schema arrangement. In production database, **postgis** and **pgrouting** extensions are created into _public_ schema whereas in the development/test database the extensions are created into a separate _extensions_ schema.  Having a separate _extensions_ schema makes it easier to develop the app. This discrepancy does not affect the functioning of the app.

In the development/test database there exists also a _flyway_ schema (for keeping account of database migrations) that is not present in the production database since database migrations are not run in the production setup.

## Docker Reference

- Needs **postgis** and **pgrouting** extensions enabled in the production database.
- Currently, there exist three databases in the docker-compose setup:
    1. One for productional use that is pre-populated with infrastructure network data and pgRouting topology (exported from [digiroad-import repo](https://github.com/HSLdevcom/jore4-digiroad-import)). In the future, this might get replaced by linking to JORE4 database via FDW (Foreign Data Wrapper extension)
    2. Development database. Initially empty with no schema, tables or data.
    3. Test database. Initially empty like development database. Used within test execution and for generating jOOQ classes.
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
| ----------------------- | ----------------------- | ---------------- | ---------------------------------------------------------------------------------- | -------------------------------------------------------------------------------- |
| -                       | SECRET_STORE_BASE_PATH  | -                | /mnt/secrets-store                                                                 | Directory containing the docker secrets                                          |
| db.url                  | DB_URL                  | db-url           | jdbc:postgresql://jore4-mapmatchingdb:5432/jore4mapmatching?stringtype=unspecified | The JDBC URL of the database containing the routing data (based on Digiroad)     |
|                         | DB_HOSTNAME             | db-hostname      | jore4-mapmatchingdb                                                                | The IP/hostname of the routing database (if DB_URL is not set)                   |
|                         | DB_PORT                 | db-port          | 5432                                                                               | The port of the routing database (if DB_URL is not set)                          |
|                         | DB_DATABASE             | db-database      | jore4mapmatching                                                                   | The name of the routing database (if DB_URL is not set)                          |
| db.username             | DB_USERNAME             | db-username      | mapmatching                                                                        | Username for the routing database                                                |
| db.password             | DB_PASSWORD             | db-password      | ****                                                                               | Password for the routing database                                                |

More properties can be found from `/profiles/prod/config.properties`
