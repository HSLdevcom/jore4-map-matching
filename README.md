# jore4-map-matching

## Overview

Provides a microservice and REST API for matching a shortest path through JORE4 infrastructure network against a given sequence of coordinates. Directionality of road links is taken account.

Vehicle mode specific link selection is not yet implemented even though the support for it exists already in the data model. Vehicle type specific turn restrictions are not yet implemented either.

Initially, the map matching and navigation services are based on Digiroad infrastructure network but later on the infrastructure network will be supplemented with other infrastructure sources as well.

The microservice consists of:
1. Spring Boot application written in [Kotlin](https://kotlinlang.org/)
2. PostgreSQL database enabled with [PostGIS](https://postgis.net/) and [pgRouting](https://pgrouting.org/) extensions

## Running app from Docker container

To start the application from a Docker container, run:

```sh
./development.sh start
```

The application is available at http://localhost:3005. The application uses a database containing Digiroad infrastructure links and is ready to use as such.

## Running app as local Java process

During development, the application is started with its database dependencies as follows:

```sh
./development.sh run
```

This avoids having to rebuild the Docker image every time the code is changed. The application is started locally via Maven (not inside a Docker container), but the database dependencies are started as Docker containers.

The application is then available at http://localhost:8080. The database used by the application is initially empty. Hence, meaningful routing results cannot be expected unless some data is populated e.g. from a database dump that is available through [Digiroad import repository](https://github.com/HSLdevcom/jore4-digiroad-import).

If you want to start only the database dependencies first and start the application itself later, for example from your IDE, you can run:

```sh
./development.sh start:deps
```

## Routing API

The structure of HTTP(S) request line for routing request is:

```
POST /api/route/{version}/{profile}
```

An example of routing request body is given below:

```json
{
    "routePoints": [
        {"lng": 24.939419922, "lat": 60.159945501},
        {"lng": 24.941977423, "lat": 60.160799285},
        {"lng": 24.941165404, "lat": 60.161371007}
    ],
    "linkSearchRadius": 150
}
```

The table below describes the request parameters part of the URI path.

| Parameter     | Description |
| ------------- | ----------- |
| `version`     | Version of the service. `v1` for `route` service. |
| `profile`     | Mode of transportation and optional vehicle type separated by a slash. E.g. `bus/tall_electric_bus`. |

The table below describes the main request body elements.

| Element                        | Description |
| ------------------------------ | ----------- |
| `routePoints`                  | Latitude and longitude points of the route |
| `linkSearchRadius`             | Optional parameter which limits search radius (in meters) while finding closest infrastructure link for each given coordinate. Defaults to `150` meters if not present. |
| `simplifyClosedLoopTraversals` | Optional boolean parameter that indicates whether consecutive traversals (full, partial, reversed, reversed partial) on a closed-loop shaped infrastructure link should be replaced by one full traversal in the direction of the first traversal appearing per loop. The handling is applied for all appearances of closed loops in a route. This denotes a compatibility mode for Jore4 where route granularity is defined in terms of whole infrastructure link geometries. Therefore, we may want to prevent inadvertent multi-traversals in closed loops. Defaults to `true`. |

The table below describes the supported profiles that consist of transportation mode and optional vehicle type. Profiles are used to constrain infrastructure links to those safely traversable for given combination of transportation mode and optional vehicle type.

| Profile                 | Description                        |
| ----------------------- | ---------------------------------- |
| `bus`                   | Shortcut for `bus/generic_bus`     |
| `bus/generic_bus`       | Is is sufficient that some type of bus may pass safely through the result route. |
| `bus/tall_electric_bus` | Require that tall electric buses must pass safely through the result route. |
| `metro`                 | Shortcut for `metro/generic_metro` |
| `metro/generic_metro`   | Metro                              |
| `train`                 | Shortcut for `train/generic_train` |
| `train/generic_train`   | Train                              |
| `tram`                  | Shortcut for `tram/generic_tram`   |
| `tram/generic_tram`     | Tram                               |

### Deprecated GET API

The same requests can currently be made using a GET request, but it is not guaranteed to work for long requests.

```
GET /api/route/{version}/{profile}/{coordinates}[.{format}]?option=value&option=value
```

Example request:

```sh
curl "https://<host>:<port>/api/route/v1/bus/24.95324,60.16980~24.83849,60.16707"
```
Version and profile are same as in the POST request.

The tables below describes the request parameters part of the URI path.

| Parameter     | Description |
| ------------- | ----------- |
| `version`     | Version of the service. `v1` for `route` service. |
| `profile`     | Mode of transportation and optional vehicle type separated by a slash. E.g. `bus/tall_electric_bus`. |
| `coordinates` | String of format `{longitude},{latitude}~{longitude},{latitude}[~{longitude},{latitude} ...]` |
| `format`      | This parameter is optional and defaults to `json` which is the only supported value. |

| Option               | Description |
| -------------------- | ------------|
| `link_search_radius` | Limit search radius (in meters) while finding closest infrastructure link for each given coordinate. Defaults to `150` meters if not present. |

## Common response format

An example JSON response for successful processing of routing/map-matching request has the following structure:

```json
{
    "code": "Ok",
    "routes": [
        {
            "geometry": {
                "type": "LineString",
                "coordinates": [ [24.957757980575668, 60.168433705609274], ... ]
            },
            "paths": [
                {
                    "infrastructureLinkId": 225656,
                    "externalLinkRef": {
                        "externalLinkId": "441874",
                        "infrastructureSource": "digiroad_r"
                    },
                    "isTraversalForwards": true,
                    "geometry": {
                        "type": "LineString",
                        "coordinates": [ [24.957757980575668, 60.168433705609274], ... ]
                    },
                    "infrastructureLinkName": {
                        "fi": "Meritullintori",
                        "sv": "Sjötullstorget"
                    },
                    "distance": 101.8133766916051,
                    "weight": 101.8133766916051
                },
                ...
            ],
            "distance": 6000.976119656034,
            "weight": 6000.976119656034
        }
    ]
}
```

The `infrastructureLinkId` attribute in the response is local to the service. Linking to other infrastructure sources (such as **Digiroad R**) may be done using a pair of `externalLinkId` and `infrastructureSource` attributes. The geometry for an entire route is given in GeoJSON format at route's top-level `geometry` attribute. A `geometry` attribute is also provided for each individual infrastructure link appearing in `paths`. The `isTraversalForwards` attribute tells whether a single link is traversed either forwards or backwards (`null` not possible) with regard to its directed `LineString` geometry.

The table below describes the possible response codes.

| Response code  | Description |
| ---------------| ------------|
| `Ok`           | Request was successfully parsed and a route was successfully resolved. |
| `InvalidUrl`   | An error occurred while parsing request parameters or options. |
| `InvalidValue` | Invalid values given e.g. at least two distinct coordinates must be given. |
| `NoSegment`    | Could not resolve a route for given parameters. |

## Map-matching API - for public transport routes

Map-matching API accepts HTTP(S) POST requests.

The request line for map-matching request has the following form:

```
POST /api/match/public-transport-route/{version}/{profile}[.{format}]
```

The table below describes the parameters of the URI path.

| Parameter | Description |
| --------- | ----------- |
| `version` | Version of the match service. `v1` is the only option for `match` service for the time being. |
| `profile` | Mode of transportation and optional vehicle type separated by a slash. See above for more details. |
| `format`  | This parameter is optional and defaults to `json` which is the only supported value. |

An example of map-matching request body is given below:

```json
{
    "routeId": "1234X-1",
    "routeGeometry": {
        "type": "LineString",
        "coordinates": [
            [24.952603, 60.165209],
            [24.952566, 60.165521],
            [24.952445, 60.165728],
            ...
        ]
    },
    "routePoints": [
        {
            "type": "PUBLIC_TRANSPORT_STOP",
            "location": {
                "type": "Point",
                "coordinates": [24.952569, 60.165421]
            },
            "projectedLocation": {
                "type": "Point",
                "coordinates": [24.952603, 60.165209]
            },
            "nationalId": 123456,
            "passengerId": "H1234"
        },
        {
            "type": "ROAD_JUNCTION",
            "location": {
                "type": "Point",
                "coordinates": [24.952445, 60.165728]
            }
        },
        {
            "type": "ROAD_JUNCTION",
            "location": {
                "type": "Point",
                "coordinates": [24.951316, 60.165692]
            }
        },
        ...
    ],
    "matchingParameters": {
        "bufferRadiusInMeters": 100.0,
        "roadJunctionMatchingEnabled": false
    }
}
```

The table below describes the main request body elements.

| Element              | Description |
| -------------------- | ----------- |
| `routeId`            | Optional string identifier for the route being matched. May be helpful in debugging when viewing logs. |
| `routeGeometry`      | `LineString` geometry of the route being matched in GeoJSON format |
| `routePoints`        | Route points of the route being matched |
| `matchingParameters` | Optional set of parameters with which map-matching functionality can be adjusted or fine-tuned |

Route points have the properties described in the table below.

| Property            | Description |
| ------------------- | ----------- |
| `type`              | The type of route point. One of the following: `PUBLIC_TRANSPORT_STOP`, `ROAD_JUNCTION`, `OTHER` |
| `location`          | `Point` geometry for the route point in GeoJSON format (see example above) |
| `projectedLocation` | `Point` geometry in GeoJSON format (see example above) for public transport stop location projected onto centerline of infrastructure link. Optional; allowed only when `type` is `PUBLIC_TRANSPORT_STOP`. |
| `nationalId`        | The national stop ID for public transport stop. The property is optional, has value of number type, and is allowed only when `type` is `PUBLIC_TRANSPORT_STOP`. |
| `passengerId`       | The short ID for public transport stop. The string property is mandatory when `type` is `PUBLIC_TRANSPORT_STOP`; otherwise not allowed. |

The optional adjustable map-matching parameters are described in the following below.

| Parameter                      | Description |
| ------------------------------ | ----------- |
| `bufferRadiusInMeters`         | The radius in meters that is used to expand the input geometry in all directions. The resulting polygon will be used to restrict the set of available infrastructure links while resolving matching route. Defaults to 50 meters. |
| `terminusLinkQueryDistance`    | The distance in meters within which the first or last infrastructure link for matching route is searched in case terminus link cannot be determined via matching public transport stop from route endpoints. Terminus links generally fall partly outside the buffer area used to restrict infrastructure links. Hence, terminus links need to be treated separately. Defaults to 50 meters. |
| `terminusLinkQueryLimit`       | The maximum number of the closest infrastructure links that are considered as terminus links at both ends of route. Defaults to 5. |
| `maxStopLocationDeviation`     | The maximum distance between two locations defined for a public transport stop, one given in the map-matching request and the other in the local database, used as a condition for matching the stop point (represented by some route point) to infrastructure links in the local database. If the distance between these two type of locations exceeds `maxStopLocationDeviation` for a stop point, then the stop point is not included in the results. Defaults to 80 meters. |
| `fallbackToViaNodesAlgorithm`  | By default, via-graph-edges algorithm is used in route matching. In the event of a matching failure, a retry using via-graph-vertices is performed if this property is set to true. Defaults to true. |
| `roadJunctionMatchingEnabled`  | Indicates whether road junction nodes should be taken into account in map-matching. If explicitly set to false, then road junction matching is disabled and parameters `junctionNodeMatchDistance` and `junctionNodeClearingDistance` must be absent or null. Defaults to true. |
| `junctionNodeMatchDistance`    | The distance, in meters, within which a node in the infrastructure network must be located from a source route point at road junction, so that the node can be concluded to be the equivalent of the route point. Must not be greater than `junctionNodeClearingDistance`. Defaults to 5 meters. |
| `junctionNodeClearingDistance` | The distance, in meters, within which an infrastructure node must be the only node in the vicinity of a given source route point (at road junction) to be reliably accepted as its peer. In other words, there must be no other infrastructure network nodes at this distance from the route point in order to have a match with high certainty. Without this condition, the false one can be chosen from two (or more) nearby nodes. This distance must be greater than or equal to `matchDistance`. Defaults to 30 meters. |

## Building

The application is built with [Maven](https://maven.apache.org/). The application is using [Flyway](https://flywaydb.org/) for database migrations and [jOOQ](https://www.jooq.org/) as query builder.

There exist two Maven profiles: `dev` and `prod`. By default, Maven uses `dev` profile.

In `prod` profile, the database is expected to be already set up with the schema definitions and infrastructure data. Hence, no database migrations are run during application start.

In `dev` profile, all three databases are involved:
-  _Development database_ containing the application data. The database is initially empty. Database migrations are run into the database during application start.
- _Test database_ that is used in jOOQ code generation. Database migrations are run during Maven build within the `process-resource` lifecycle phase which occurs just before source code compilation. The jOOQ classes are updated in the same lifecycle phase after the migrations.
- _Pre-populated database_ is used in Spring integration tests which rely upon pre-populated data (currently same data as in production) originating from [Digiroad](https://vayla.fi/en/transport-network/data/digiroad/data).

With `dev` profile one needs to create a user-specific build configuration file e.g. as follows:

```sh
touch spring-boot/profiles/dev/config.$(whoami).properties
```

## Populating data to development database

Within developing the application, the currently recommended way of importing infrastructure data is to:
1. Start map-matching server with the commands below. Within launch, the server will run database migration scripts into the development database. The scripts will initialise schemas, tables, constraints and indices. Also a couple of enumeration tables are populated with a fixed set of data.

    ```sh
    ./development.sh run
    ```
2. Populate data (infrastructure links, infrastructure sources, network topology, associations of links to vehicle types and public transport stops) from [Digiroad import repository](https://github.com/HSLdevcom/jore4-digiroad-import). This does not involve creating tables neither populating enumeration tables (which already contain data coming from the migration scripts).

To generate a Digiroad-based dump, issue the following commands in the Digiroad import repository:

```sh
./build_docker_image.sh
./import_digiroad_shapefiles.sh
./export_routing_schema.sh
```

To restore table data from the dump (generated into `workdir/pgdump` directory), issue the following command (with `<date>` placeholder replaced with a proper value):

```sh
pg_restore -1 -a --use-list=digiroad_r_routing_<date>.pgdump.no-enums.links-and-stops.list -h localhost -p 18000 -d jore4mapmatching -U mapmatching digiroad_r_routing_<date>.pgdump 
```

The list argument passed to `pg_restore` command will constrain the restoration of the dump file to data of selected tables only. Hence, enumeration tables are excluded as well as create table statements.

## Development notes

Within each build cycle, the test database is cleaned and re-initialised with database migrations and jOOQ code generation is invoked. This way, the validity of migration scripts is always verified and the jOOQ classes are kept up-to-date.

The development database can be re-initialised (without recreating it) by running:

```sh
mvn properties:read-project-properties flyway:clean flyway:migrate
```

Currently, there is a discrepancy between pre-populated database and the development/test database with regard to schema arrangement. In pre-populated database, **postgis** and **pgrouting** extensions are created into _public_ schema whereas in the development/test database the extensions are created into a separate _extensions_ schema.  Having a separate _extensions_ schema makes it easier to develop the app. This discrepancy does not affect the functioning of the app.

In the development/test database there exists also a _flyway_ schema (for keeping account of database migrations) that is not present in the pre-populated database since database migrations are not run in the production setup.

## Docker Reference

- Needs **postgis** and **pgrouting** extensions enabled in the production database.
- Currently, there exist three databases in the docker-compose setup:
    1. One for testing with pre-populated data with infrastructure network data and pgRouting topology (exported from [digiroad-import repo](https://github.com/HSLdevcom/jore4-digiroad-import)). In the future, this might get replaced by linking to JORE4 database via FDW (Foreign Data Wrapper extension)
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

## License

The project license is in [`LICENSE`](./LICENSE).

Digiroad data has been licensed with Creative Commons BY 4.0 license by the
[Finnish Transport Infrastructure Agency](https://vayla.fi/en/transport-network/data/digiroad/data).
