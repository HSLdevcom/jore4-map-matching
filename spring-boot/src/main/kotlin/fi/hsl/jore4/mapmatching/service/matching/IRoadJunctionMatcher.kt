package fi.hsl.jore4.mapmatching.service.matching

import fi.hsl.jore4.mapmatching.model.NodeProximity
import fi.hsl.jore4.mapmatching.model.VehicleType
import fi.hsl.jore4.mapmatching.model.matching.RoutePoint

interface IRoadJunctionMatcher {

    /**
     * Searches the local database for infrastructure nodes that can be
     * associated with the given source route points unambiguously, i.e. there
     * must be no other nodes nearby for a match to occur. Only route points of
     * road junction type are attempted to be matched with infrastructure nodes.
     * The matched nodes are indexed in the same order as the route points exist
     * in [routePoints], i.e. there may be gaps between the index numbers found
     * as keys of the result [Map].
     *
     * It is important that the distance parameters are given conservatively
     * such that no wrong connections are made that could ruin map-matching
     * results.
     *
     * @param routePoints points of a source route to be map-matched. Other than
     * road junction points are filtered out from the given route points.
     * @param vehicleType vehicle type constraint to be satisfied while finding
     * infrastructure nodes. A matching node needs to be either a start or end
     * node for such infrastructure link that is safely traversable by the given
     * vehicle type.
     * @param matchDistance the distance, in meters, within which a node in the
     * infrastructure network must be located from a source route point at road
     * junction, so that the node can be concluded to be the equivalent of the
     * route point. This distance must be less than or equal to
     * [clearingDistance].
     * @param clearingDistance the distance, in meters, within which an
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
    fun findInfrastructureNodesMatchingRoadJunctions(routePoints: List<RoutePoint>,
                                                     vehicleType: VehicleType,
                                                     matchDistance: Double,
                                                     clearingDistance: Double)
        : Map<Int, NodeProximity?>
}
