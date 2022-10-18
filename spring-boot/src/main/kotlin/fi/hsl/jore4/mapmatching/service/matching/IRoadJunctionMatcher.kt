package fi.hsl.jore4.mapmatching.service.matching

import fi.hsl.jore4.mapmatching.model.NodeProximity
import fi.hsl.jore4.mapmatching.model.VehicleType
import fi.hsl.jore4.mapmatching.model.matching.RoutePoint
import fi.hsl.jore4.mapmatching.service.matching.PublicTransportRouteMatchingParameters.JunctionMatchingParameters

interface IRoadJunctionMatcher {

    /**
     * Finds infrastructure nodes from the local database that can be matched
     * with the given [routePoints]. Only route points of road junction type
     * are tried to be matched with infrastructure nodes. The matched nodes are
     * indexed in the same order as the route points exist in [routePoints],
     * that is, there may be gaps between the index numbers found as keys of the
     * result [Map].
     *
     * @param routePoints points of a source route to be map-matched. Other than
     * road junction points are filtered out from the given route points.
     * @param vehicleType vehicle type constraint to be satisfied while finding
     * infrastructure nodes. A matching node needs to be either a start or end
     * node for such infrastructure link that is safely traversable by the given
     * vehicle type.
     * @param matchingParameters contains additional parameters for matching
     * road junction nodes from the infrastructure network.
     */
    fun findInfrastructureNodesMatchingRoadJunctions(routePoints: List<RoutePoint>,
                                                     vehicleType: VehicleType,
                                                     matchingParameters: JunctionMatchingParameters)
        : Map<Int, NodeProximity?>
}
