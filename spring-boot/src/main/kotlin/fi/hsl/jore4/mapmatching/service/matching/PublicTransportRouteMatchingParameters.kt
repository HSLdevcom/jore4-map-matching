package fi.hsl.jore4.mapmatching.service.matching

import java.lang.IllegalArgumentException

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
 * @property roadJunctionMatching contains details for matching road junction
 * nodes. If missing, then road junction node matching is disabled. Road
 * junction points are identified as essential items in producing accurate match
 * results.
 *
 * @throws IllegalArgumentException
 */
data class PublicTransportRouteMatchingParameters(val bufferRadiusInMeters: Double,
                                                  val terminusLinkQueryDistance: Double,
                                                  val roadJunctionMatching: JunctionMatchingParameters?) {

    /**
     * Contains two distance properties with which route points of road junction
     * type are matched with infrastructure network nodes within map-matching.
     *
     * @property junctionNodeMatchDistance the distance in meters within which
     * an infrastructure network node must locate from a route point of road
     * junction type in order to be matched with it.
     * @property junctionNodeClearingDistance the distance in meters within
     * which an infrastructure node must be the only node in proximity of a
     * route point (of road junction type) in order to be accepted as the match
     * for it. In other words, no other infrastructure network nodes are allowed
     * to exist within this distance from the route point for a match to occur.
     *
     * @throws IllegalArgumentException
     */
    data class JunctionMatchingParameters(val junctionNodeMatchDistance: Double,
                                          val junctionNodeClearingDistance: Double) {

        init {
            if (junctionNodeMatchDistance > junctionNodeClearingDistance) {
                throw IllegalArgumentException(
                    "junctionNodeMatchDistance must not be greater than junctionNodeClearingDistance")
            }
        }
    }
}