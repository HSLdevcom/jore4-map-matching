# jore4-map-matching

## Overview

Provides a microservice and REST API for matching a shortest path through JORE4 infrastructure network against a given sequence of coordinates. Directionality of road links is taken account.

Vehicle mode specific link selection is not yet implemented even though the support for it exists already in the data model. Vehicle type specific turn restrictions are not yet either implemented.

Initially, the map matching and navigation services are based on Digiroad infrastructure network but later on the infrastructure network will be supplemented with other infrastructure sources as well.

The microservice consists of (1) Spring Boot application written in [Kotlin](https://kotlinlang.org/) and (2) an internal PostgreSQL database enabled with [PostGIS](https://postgis.net/) and [pgRouting](https://pgrouting.org/) extensions.

## How to use API

API NOT YET IMPLEMENTED.

## Building

The app is built with [Maven](https://maven.apache.org/). The application is using [Flyway](https://flywaydb.org/) for database migrations and [jOOQ](https://www.jooq.org/) as query builder.

There are defined two Maven profiles: `dev` and `prod`. By default, Maven uses `dev` profile.

In `prod` profile, which is used by docker-compose setup, the database migrations are executed when the app starts.

In `dev` profile the database migrations are instead run during Maven build within `process-resource` lifecycle phase which occurs before source code compilation. The jOOQ metadata classes are updated in the same lifecycle phase.

With `dev` profile one needs to make a copy of the build configuration file as follows:

```
    cp profiles/dev/config.properties profiles/dev/config.<my-username>.properties
```

## Running the app in docker-compose

The application can be started with:

```
    ./development start
```

The app will be available through http://localhost:3200.

## Running the app within development

Within development it is intended to start the PostGIS database via docker-compose and start the application manually with Maven (in order to avoid the need to rebuild Docker image) as follows:

```
    ./development start:deps
    cd spring-boot
    mvn clean spring-boot:run
```

Then, the app will be available through http://localhost:8080.

## Populating database

Within development, the currently recommended way of importing data is to:
1. Start map-matching server with the commands below. Within launch, the server will run database migration scripts into the database that will initialise the database with schema, tables, constraints and indices. Also a couple of enumeration tables are also populated with a fixed set of data.
    ```
        ./development start:deps
        cd spring-boot
        mvn spring-boot:run
    ```
2. Populate table data for infrastructure links, infrastructure sources, network topology and associations of links to vehicle types. This does not involve creating tables neither populating enumeration tables which already contain data.

Digiroad links can be exported from [Digiroad import repository](https://github.com/HSLdevcom/jore4-digiroad-import-experiment).

To generate a Digiroad-based dump containing table data to be populated into map-matching database, issue the following commands in the Digiroad import repository:

```
    ./build_docker_image.sh
    ./import_digiroad_shapefiles.sh
    ./export_pgdump_routing.sh
```

To populate data from the dump generated (in `workdir/pgdump` directory), issue the following command (with `<date>` placeholder replaced with a proper value):

```
    pg_restore -1 -a --use-list=digiroad_r_routing_<date>.pgdump.no-enums.only-links.list -h localhost -p 19000 -d jore4mapmatching -U mapmatching digiroad_r_routing_<date>.pgdump 

```

The list argument passed to `pg_restore` command will constrain the restoration of dump to only data of selected tables. Hence, enumeration tables are excluded as well as create table statements.

## Development notes

For testing migration SQL scripts, the database can be reset into its initial state (with all migrations undone) with:

```
    mvn flyway:clean
```

## Docker Reference

- Needs pgRouting-enabled test database to be deployed.
- Currently we should fill it up with Digiroad data (exported from [Digiroad import repository](https://github.com/HSLdevcom/jore4-digiroad-import-experiment)), in the future could link to JORE4 database with FDW (Foreign Data Wrapper extension)
- When flexible-server enables pgRouting extension, the map-matching service will point directly to the JORE4 database
instead of having its own database
- Warning: currently map-matching always runs its own migrations. This is fine while it uses its own Docker database, but should not do so when using the JORE4 database in the future
- How to load Digiroad export to mapmatchingdb

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
| db.url                  | DB_URL                  | db-url           | jdbc:postgresql://jore4-mapmatchingdb:5432/jore4mapmatching?stringtype=unspecified | The JDBC URL of the database containing the routing (+Digiroad) data             |
|                         | DB_HOSTNAME             | db-hostname      | jore4-mapmatchingdb                                                                | The IP/hostname of the routing database (if DB_URL is not set)                   |
|                         | DB_PORT                 | db-port          | 5432                                                                               | The port of the routing database (if DB_URL is not set)                          |
|                         | DB_DATABASE             | db-database      | jore4mapmatching                                                                   | The name of the routing database (if DB_URL is not set)                          |
| db.username             | DB_USERNAME             | db-username      | mapmatching                                                                        | Username for the routing database                                                |
| db.password             | DB_PASSWORD             | db-password      | ****                                                                               | Password for the routing database                                                |

More properties can be found from `/profiles/prod/config.properties`
