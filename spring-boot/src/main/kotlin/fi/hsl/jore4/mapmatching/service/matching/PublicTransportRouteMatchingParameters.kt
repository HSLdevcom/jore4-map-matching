package fi.hsl.jore4.mapmatching.service.matching

/**
 * Contains variable parameters that can be adjusted while map-matching public
 * transport routes against the infrastructure network provided by the system.
 *
 * @property bufferRadiusInMeters the radius in meters that is used to expand
 * input route geometry in all directions. The resulting polygon will be used
 * to restrict the set of available infrastructure links (using ST_Contains
 * and/or ST_Intersects function) while resolving matching route.
 * @property terminusLinkQueryDistance the distance in meters within which the
 * first or last infrastructure link for matching route is searched in case
 * terminus link cannot be determined via matching public transport stop from
 * route endpoints. Terminus links generally fall partly outside the buffer area
 * used to restrict infrastructure links. Hence, terminus links need to be
 * treated separately.
 */
data class PublicTransportRouteMatchingParameters(val bufferRadiusInMeters: Double,
                                                  val terminusLinkQueryDistance: Double)
