package fi.hsl.jore4.mapmatching.controller.api

import fi.hsl.jore4.mapmatching.model.matching.RoutePoint
import jakarta.validation.Valid
import jakarta.validation.constraints.AssertTrue
import jakarta.validation.constraints.Pattern
import org.geolatte.geom.G2D
import org.geolatte.geom.LineString

/**
 * Contains input data for map-matching a public transport route against the
 * infrastructure network provided by the system.
 *
 * @property routeId optional identifier for the route to be matched
 * @property routeGeometry geometry of the route to be matched in GeoJSON format
 * @property routePoints route points of the route being matched
 * @property matchingParameters optional parameters with which map-matching
 * functionality can be adjusted
 */
data class PublicTransportRouteMatchRequestDTO(
    @field:Pattern(regexp = "[\\w-_ ]{1,50}") val routeId: String?,
    val routeGeometry: LineString<G2D>,
    @field:Valid val routePoints: List<RoutePoint>,
    @field:Valid val matchingParameters: MapMatchingParametersDTO?
) {
    /**
     * Contains parameters that can be adjusted for the purpose of getting
     * optimal results in matching public transport routes against the
     * infrastructure network provided by the system.
     *
     * @property bufferRadiusInMeters the radius in meters that is used to
     * expand the input geometry in all directions. The resulting polygon will
     * be used to restrict the set of available infrastructure links while
     * resolving matching route.
     * @property terminusLinkQueryDistance the distance in meters within which
     * the first or last infrastructure link for matching route is searched in
     * case terminus link cannot be determined via matching public transport
     * stop from route endpoints. Terminus links generally fall partly outside
     * the buffer area used to restrict infrastructure links. Hence, terminus
     * links need to be treated separately.
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
     * @property roadJunctionMatchingEnabled indicates whether road junction
     * nodes should be taken into account in map-matching. If explicitly set to
     * false, then road junction matching is disabled and parameters
     * [junctionNodeMatchDistance] and [junctionNodeClearingDistance] must be
     * null.
     * @property junctionNodeMatchDistance the distance, in meters, within which
     * a node in the infrastructure network must be located from a source route
     * point at road junction, so that the node can be concluded to be the
     * equivalent of the route point. This distance must be less than or equal
     * to [junctionNodeClearingDistance].
     * @property junctionNodeClearingDistance the distance, in meters, within
     * which an infrastructure node must be the only node in the vicinity of a
     * given source route point (at road junction) to be reliably accepted as
     * its peer. In other words, there must be no other infrastructure network
     * nodes at this distance from the route point in order to have a match with
     * high certainty. Without this condition, the false one can be chosen from
     * two (or more) nearby nodes. This distance must be greater than or equal
     * to [junctionNodeMatchDistance].
     * @property fallbackToViaNodesAlgorithm By default, via-graph-edges
     * algorithm is used in route matching. In the event of a matching failure,
     * a retry using via-graph-vertices is performed if this property is set to
     * true.
     */
    data class MapMatchingParametersDTO(
        val bufferRadiusInMeters: Double?,
        val terminusLinkQueryDistance: Double?,
        val terminusLinkQueryLimit: Int?,
        val maxStopLocationDeviation: Double?,
        val roadJunctionMatchingEnabled: Boolean?,
        val junctionNodeMatchDistance: Double?,
        val junctionNodeClearingDistance: Double?,
        val fallbackToViaNodesAlgorithm: Boolean?
    ) {
        private val isRoadJunctionMatchingEnabled: Boolean
            get() = roadJunctionMatchingEnabled != false

        @AssertTrue(message = "false")
        fun isJunctionNodeDistancesAbsentWhenJunctionMatchingDisabled(): Boolean =
            isRoadJunctionMatchingEnabled || junctionNodeMatchDistance == null && junctionNodeClearingDistance == null

        @AssertTrue(message = "false")
        fun isJunctionNodeMatchDistanceNotGreaterThanClearingDistance(): Boolean =
            if (junctionNodeMatchDistance != null && junctionNodeClearingDistance != null) {
                junctionNodeMatchDistance <= junctionNodeClearingDistance
            } else {
                true
            }
    }

    @AssertTrue(message = "false")
    fun isAtLeastTwoRoutePointsGiven(): Boolean = routePoints.size >= 2

    val firstRoutePoint: RoutePoint
        get() = routePoints.first()

    val lastRoutePoint: RoutePoint
        get() = routePoints.last()
}
