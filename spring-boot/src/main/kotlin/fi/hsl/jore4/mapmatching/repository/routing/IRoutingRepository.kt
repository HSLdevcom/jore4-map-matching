package fi.hsl.jore4.mapmatching.repository.routing

import fi.hsl.jore4.mapmatching.model.NodeIdSequence
import fi.hsl.jore4.mapmatching.model.VehicleType

interface IRoutingRepository {

    /**
     * Find the shortest route through infrastructure network via given nodes
     * and constrained by the given vehicle type and optional buffer area.
     *
     * @param nodeIdSequence sequence of identifiers for infrastructure network
     * nodes that the route must pass through. The sequence must not contain
     * consecutive duplicate entries.
     * @param vehicleType vehicle type constraint for the route. Resulting route
     * must consist of only those infrastructure links that are safely
     * traversable by the given vehicle type.
     * @param bufferAreaRestriction contains data that with which geometrical
     * restriction for the target set of infrastructure links can be defined
     * while finding route through infrastructure network.
     *
     * @return a list of route links that together constitute the resulting
     * route. Each route link contains a path element that consists of a
     * reference to an infrastructure link and the direction of traversal on it.
     * If a route could not be found then an empty list is returned.
     */
    fun findRouteViaNetworkNodes(nodeIdSequence: NodeIdSequence,
                                 vehicleType: VehicleType,
                                 bufferAreaRestriction: BufferAreaRestriction? = null)
        : List<RouteLinkDTO>
}
