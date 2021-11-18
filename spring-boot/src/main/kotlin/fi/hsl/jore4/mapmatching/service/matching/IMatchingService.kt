package fi.hsl.jore4.mapmatching.service.matching

import fi.hsl.jore4.mapmatching.model.VehicleType
import fi.hsl.jore4.mapmatching.model.matching.RoutePoint
import fi.hsl.jore4.mapmatching.service.common.response.RoutingResponse
import org.geolatte.geom.G2D
import org.geolatte.geom.LineString

interface IMatchingService {

    /**
     * Find the shortest route that matches as closely as possible with the
     * given input geometry. The resulting route must visit the infrastructure
     * links that are associated with the public transport stop points appearing
     * as route points of the input route. The public transport stop points are
     * matched with their national identifiers. The resulting route must also be
     * safely traversable by the given vehicle type.
     *
     * @param routeGeometry the input geometry as LineString for which a close
     * match is to be resolved.
     * @param routePoints the route points along the input geometry
     * @param vehicleType vehicle type constraint for the resulting route. The
     * route must consist of only those infrastructure links that are safely
     * traversable by the given vehicle type.
     * @param matchingParameters contains parameters that can be adjusted for
     * the purpose of getting optimal results in matching public transport
     * routes against the infrastructure network provided by the system.
     *
     * @return either a successful or failure-marking routing response.
     */
    fun findMatchForPublicTransportRoute(routeGeometry: LineString<G2D>,
                                         routePoints: List<RoutePoint>,
                                         vehicleType: VehicleType,
                                         matchingParameters: PublicTransportRouteMatchingParameters)
        : RoutingResponse
}
