package fi.hsl.jore4.mapmatching.service.routing

import fi.hsl.jore4.mapmatching.model.LatLng
import fi.hsl.jore4.mapmatching.model.VehicleType
import fi.hsl.jore4.mapmatching.service.common.response.RoutingResponse

interface IRoutingService {

    /**
     * Find route through infrastructure network with the given coordinates and
     * vehicle type.
     *
     * @param coordinates input coordinates of which each is matched with the
     * infrastructure network node that the resulting route must pass through.
     * The network node is determined as the closest endpoint of the closest
     * infrastructure link to a given coordinate.
     * @param vehicleType vehicle type constraint for the route. Resulting route
     * must consist of only those infrastructure links that are safely
     * traversable by the given vehicle type.
     * @param linkQueryDistance the distance in meters within which the closest
     * infrastructure link is searched for each given coordinate
     *
     * @return either a successful or failing routing response. A successfully
     * resolved route is matched with the given coordinates and vehicle type. If
     * the closest infrastructure link cannot be found for any of the given
     * coordinates then a failure response is returned.
     */
    fun findRoute(coordinates: List<LatLng>, vehicleType: VehicleType, linkQueryDistance: Int): RoutingResponse
}
