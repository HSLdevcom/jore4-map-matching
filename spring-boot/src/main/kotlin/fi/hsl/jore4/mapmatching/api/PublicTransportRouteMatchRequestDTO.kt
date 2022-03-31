package fi.hsl.jore4.mapmatching.api

import fi.hsl.jore4.mapmatching.model.matching.RoutePoint
import org.geolatte.geom.G2D
import org.geolatte.geom.LineString
import javax.validation.Valid
import javax.validation.constraints.AssertTrue
import javax.validation.constraints.Pattern

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
data class PublicTransportRouteMatchRequestDTO(@field:Pattern(regexp = "[\\w\\d-_ ]{1,50}") val routeId: String?,
                                               val routeGeometry: LineString<G2D>,
                                               @field:Valid val routePoints: List<RoutePoint>,
                                               @field:Valid val matchingParameters: MapMatchingParametersDTO?) {

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
     * @property maxStopLocationDeviation the maximum distance within which two
     * locations given for a public transport stop are allowed to be away from
     * each other, in order to include the stop in the set of route points that
     * are matched with infrastructure links. The first location is the one
     * hosted in this map-matching service (mostly originating from Digiroad)
     * and the second one is the location defined within the client system
     * (invoking this map-matching service). If the distance between these two
     * type of locations exceeds [maxStopLocationDeviation], then the affected
     * stops are discarded from the set of route points that are matched with
     * infrastructure links.
     * @property roadJunctionMatchingEnabled indicates whether road junction
     * nodes should be taken into account in map-matching. If explicitly set to
     * false, then road junction matching is disabled and parameters
     * [junctionNodeMatchDistance] and [junctionNodeClearingDistance] must be
     * null.
     * @property junctionNodeMatchDistance the distance in meters within which
     * an infrastructure network node must locate from a route point of road
     * junction type in order to be matched with it.
     * @property junctionNodeClearingDistance the distance in meters within
     * which an infrastructure node must be the only node in proximity of a
     * route point (of road junction type) in order to be accepted as the match
     * for it. In other words, no other infrastructure network nodes are allowed
     * to exist within this distance from the route point for a match to occur.
     */
    data class MapMatchingParametersDTO(val bufferRadiusInMeters: Double?,
                                        val terminusLinkQueryDistance: Double?,
                                        val terminusLinkQueryLimit: Int?,
                                        val maxStopLocationDeviation: Double?,
                                        val roadJunctionMatchingEnabled: Boolean?,
                                        val junctionNodeMatchDistance: Double?,
                                        val junctionNodeClearingDistance: Double?) {

        private val isRoadJunctionMatchingEnabled: Boolean
            get() = roadJunctionMatchingEnabled?.let { it } != false

        @AssertTrue(message = "false")
        fun isJunctionNodeDistancesAbsentWhenJunctionMatchingDisabled(): Boolean =
            isRoadJunctionMatchingEnabled || junctionNodeMatchDistance == null && junctionNodeClearingDistance == null

        @AssertTrue(message = "false")
        fun isJunctionNodeMatchDistanceNotGreaterThanClearingDistance(): Boolean =
            if (junctionNodeMatchDistance != null && junctionNodeClearingDistance != null)
                junctionNodeMatchDistance <= junctionNodeClearingDistance
            else true
    }

    @AssertTrue(message = "false")
    fun isAtLeastTwoRoutePointsGiven(): Boolean = routePoints.size >= 2

    val firstRoutePoint: RoutePoint
        get() = routePoints.first()

    val lastRoutePoint: RoutePoint
        get() = routePoints.last()
}
