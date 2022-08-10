package fi.hsl.jore4.mapmatching.test

/**
 * A base class for integration tests. Current strategy within integration tests is to utilise
 * a pre-populated PostgreSQL database enabled with PostGIS & pgRouting extensions running inside
 * a Docker container. This involves the same Docker image that is used by the actual map-matching
 * service.
 */
abstract class IntegrationTest
