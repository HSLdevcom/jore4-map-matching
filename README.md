# jore4-map-matching

## Overview

Provides a microservice and REST API for matching a shortest path through JORE4 infrastructure network against a given sequence of coordinates. Directionality of road links is taken account. Vehicle type specific turn restrictions are not yet implemented.

Initially, the map matching and navigation services are based on Digiroad infrastructure network but later on the infrastructure network will be supplemented with other sources as well.

The microservice consists of (1) Spring Boot application written in [Kotlin](https://kotlinlang.org/) and (2) an internal PostgreSQL database enabled with [PostGIS](https://postgis.net/) and [pgRouting](https://pgrouting.org/) extensions.

## How to use API

API NOT YET IMPLEMENTED.

## Implementation details

The app is built with [Maven](https://maven.apache.org/). There are defined two Maven profiles: `dev` and `prod`. In `prod` profile, which is used by docker-compose setup, the database migrations are executed when the app starts. In `dev` profile the database migrations are instead run during Maven build.

In `dev` profile the database migrations are run within `process-resource` lifecycle phase (before source code compilation). The jOOQ metadata classes are updated in the same lifecycle phase.

The application is using [Flyway](https://flywaydb.org/) for database migrations and [jOOQ](https://www.jooq.org/) as query builder.

## Running the app in docker-compose

The application can be started with:

```
    docker-compose up --build
```

The app will be available through http://localhost:3200.

## Running the app within development

Within development it is meant to start the PostGIS database via docker-compose and start the application manually with Maven (in order to avoid the need to rebuild Docker image) as follows:

```
    docker-compose up -d postgis
    cd spring-boot
    mvn clean spring-boot:run
```

Then, the app will be available through http://localhost:8080.

By default, Maven uses `dev` profile. With `dev` profile one needs to make a copy of the build configuration file as follows:

```
    cp profiles/dev/config.properties profiles/dev/config.<my-username>.properties
```

## Development notes

For testing migration SQL scripts, the database can be reset into its initial state (with all migrations undone) with:

```
    mvn flyway:clean
```

Initially will provide map matching and navigation services based upon Digiroad infrastructure network for other Jore4 components.

## Docker Reference

- Needs pgrouting test database to be deployed.
- Currently we should fill it up with digiroad data (exported from digiroad-import repo), in the future could link to jore4 db with FDW
- When flexible-server enables pgrouting extension, the map-matching service will point directly to the jore4 database
instead of having its own database
- Warning: currently map-matching always runs its own migrations. This is fine while it uses its own docker database, but should not do so
when using the jore4 database in the future
- How to load digiroad export to mapmatchingdb

Ports:

8080

Environment variables:

The application uses spring boot which allows overwriting configuration properties as described
[here](https://docs.spring.io/spring-boot/docs/current/reference/html/features.html#features.external-config.typesafe-configuration-properties.relaxed-binding.environment-variables).
The docker container is also able to
[read secrets](https://github.com/HSLdevcom/jore4-tools#read-secretssh) and expose
them as environment variables.

The following configuration properties are to be defined for each environment:

| Config property         | Environment variable    | Secret name      | Example                                                                            | Description                                                                      |
| ----------------------  | ----------------------- | ---------------- | ---------------------------------------------------------------------------------- | -------------------------------------------------------------------------------- |
| -                       | SECRET_STORE_BASE_PATH  | -                | /mnt/secrets-store                                                                 | Directory containing the docker secrets                                          |
| db.url                  | DB_URL                  | db-url           | jdbc:postgresql://jore4-mapmatchingdb:5432/jore4mapmatching?stringtype=unspecified | The jdbc url of the database containing the routing (+Digiroad) data             |
|                         | DB_HOSTNAME             | db-hostname      | jore4-mapmatchingdb                                                                | The IP/hostname of the routing database (if DB_URL is not set)                   |
|                         | DB_PORT                 | db-port          | 5432                                                                               | The port of the routing database (if DB_URL is not set)                          |
|                         | DB_DATABASE             | db-database      | jore4mapmatching                                                                   | The name of the routing database (if DB_URL is not set)                          |
| db.username             | DB_USERNAME             | db-username      | mapmatching                                                                        | Username for the routing database                                                |
| db.password             | DB_PASSWORD             | db-password      | ****                                                                               | Password for the routing database                                                |

More properties can be found from `/profiles/prod/config.properties`
