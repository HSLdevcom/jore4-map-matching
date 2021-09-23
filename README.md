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
    docker-compose up
```

The app will be available through http://localhost:3200.

After changes has been made to the app source code, one needs to update the Docker image with:

```
    docker-compose build
```

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

For testing migration SQL scripts, the database can be resetted into its initial state (with all migrations undone) with:

```
    mvn flyway:clean
```
