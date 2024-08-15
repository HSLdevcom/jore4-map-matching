package fi.hsl.jore4.mapmatching.service.routing

import fi.hsl.jore4.mapmatching.model.VehicleType
import fi.hsl.jore4.mapmatching.service.common.response.RoutingResponse
import org.geolatte.geom.G2D
import org.geolatte.geom.Point

interface IRoutingService {
    /**
     * Find route through infrastructure network that matches with the given
     * points and vehicle type.
     *
     * @param viaPoints list of points that are mapped to infrastructure network
     * nodes that the resulting route must pass through. For each given point,
     * the matching network node is determined as the closest endpoint of the
     * closest infrastructure link to the point.
     * @param vehicleType vehicle type constraint for the resulting route. The
     * route must consist of only those infrastructure links that are safely
     * traversable by the given vehicle type.
     * @param extraParameters contains additional extra parameters that affect
     * routing.
     *
     * @return either a successful or failing routing response. A successfully
     * resolved route matches with the given points and vehicle type. If the
     * closest infrastructure link cannot be found for any of the given points
     * then a failure response is returned.
     */
    fun findRoute(
        viaPoints: List<Point<G2D>>,
        vehicleType: VehicleType,
        extraParameters: RoutingExtraParameters
    ): RoutingResponse
}
