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
     */
    data class MapMatchingParametersDTO(val bufferRadiusInMeters: Double?, val terminusLinkQueryDistance: Double?)

    @AssertTrue(message = "false")
    fun isAtLeastTwoRoutePointsGiven(): Boolean = routePoints.size >= 2

    val firstRoutePoint: RoutePoint
        get() = routePoints.first()

    val lastRoutePoint: RoutePoint
        get() = routePoints.last()
}
