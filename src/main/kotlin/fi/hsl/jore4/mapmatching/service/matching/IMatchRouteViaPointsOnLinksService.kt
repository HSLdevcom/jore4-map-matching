package fi.hsl.jore4.mapmatching.service.matching

import fi.hsl.jore4.mapmatching.model.VehicleType
import fi.hsl.jore4.mapmatching.model.matching.RoutePoint
import fi.hsl.jore4.mapmatching.service.common.response.RoutingResponse
import org.geolatte.geom.G2D
import org.geolatte.geom.LineString

interface IMatchRouteViaPointsOnLinksService {
    /**
     * Finds the shortest route through the infrastructure network provided by
     * the system that matches as closely as possible with the given source
     * geometry. The given [sourceRoutePoints] are used as hints in a heuristic
     * that selects the most likely infrastructure elements (links and nodes) to
     * the resulting route. The resulting route must be safely traversable by
     * the given vehicle type.
     *
     * This internal service variant uses points-on-links based algorithm (via
     * graph edges) while finding a matching route.
     *
     * @param sourceRouteGeometry the source geometry as a LineString for which
     * a closely matching target LineString geometry is to be resolved.
     * @param sourceRoutePoints the route points along the source route
     * geometry. Route points are tried to be connected to infrastructure links
     * and nodes in the local database. The purpose of this is to help select
     * the most locally appropriate path from among multiple options.
     * @param vehicleType vehicle type constraint for the resulting route. The
     * route must consist of only those infrastructure links that are safely
     * traversable by the given vehicle type.
     * @param matchingParameters contains parameters that can be adjusted to get
     * optimal matching results.
     *
     * @return either a successful or failure-marking routing response.
     */
    fun findMatchForPublicTransportRoute(
        sourceRouteGeometry: LineString<G2D>,
        sourceRoutePoints: List<RoutePoint>,
        vehicleType: VehicleType,
        matchingParameters: PublicTransportRouteMatchingParameters
    ): RoutingResponse
}
