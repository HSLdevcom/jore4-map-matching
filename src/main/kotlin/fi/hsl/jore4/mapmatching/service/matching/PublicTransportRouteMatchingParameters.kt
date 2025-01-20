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
 * @property terminusLinkQueryLimit the maximum number of the closest
 * infrastructure links that are considered as terminus links at both ends of
 * route
 * @property maxStopLocationDeviation the maximum distance between two
 * locations defined for a public transport stop, one given in the
 * map-matching request and the other in the local database, used as a
 * condition for matching the stop point (represented by some route point)
 * to infrastructure links in the local database. If the distance between
 * these two type of locations exceeds [maxStopLocationDeviation] for a stop
 * point, then the stop point is not included in the results.
 * @property fallbackToViaNodesAlgorithm By default, via-graph-edges algorithm
 * is used in route matching. In the event of a matching failure, a retry using
 * via-graph-vertices is performed if this property is set to true.
 * @property roadJunctionMatching contains parameters for tuning heuristics to
 * match source route points with infrastructure network nodes. If missing, then
 * road junction node matching is disabled.
 *
 * @throws IllegalArgumentException
 */
data class PublicTransportRouteMatchingParameters(
    val bufferRadiusInMeters: Double,
    val terminusLinkQueryDistance: Double,
    val terminusLinkQueryLimit: Int,
    val maxStopLocationDeviation: Double,
    val fallbackToViaNodesAlgorithm: Boolean,
    val roadJunctionMatching: JunctionMatchingParameters?
) {
    /**
     * Contains distance properties to tune heuristics for matching route points
     * with infrastructure network nodes (of road junction type). It is
     * desirable to get as many matches as possible without false guesses that
     * could ruin match results. Conservative values are recommended.
     *
     * @property matchDistance the distance, in meters, within which a node in
     * the infrastructure network must be located from a source route point at
     * road junction, so that the node can be concluded to be the equivalent of
     * the route point. This distance must be less than or equal to
     * [clearingDistance].
     * @property clearingDistance the distance, in meters, within which an
     * infrastructure node must be the only node in the vicinity of a given
     * source route point (at road junction) to be reliably accepted as its
     * peer. In other words, there must be no other infrastructure network nodes
     * at this distance from the route point in order to have a match with high
     * certainty. Without this condition, the false one can be chosen from two
     * (or more) nearby nodes. This distance must be greater than or equal to
     * [matchDistance].
     *
     * @throws IllegalArgumentException if match distance is greater than
     * clearing distance
     */
    data class JunctionMatchingParameters(val matchDistance: Double, val clearingDistance: Double) {
        init {
            require(matchDistance <= clearingDistance) {
                "matchDistance must not be greater than clearingDistance"
            }
        }
    }
}
