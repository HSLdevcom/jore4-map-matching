package fi.hsl.jore4.mapmatching.repository.routing

import fi.hsl.jore4.mapmatching.model.VehicleType

interface IRoutingRepository {

    /**
     * Find route through infrastructure network via given nodes and constrained
     * by the given vehicle type.
     *
     * @param vehicleType vehicle type constraint for the route. Resulting route
     * must consist of only those infrastructure links that are safely
     * traversable by the given vehicle type.
     * @param nodeIds list of identifiers for infrastructure network nodes that
     * the route must pass through. The list must not contain consecutive
     * duplicate entries.
     *
     * @return a list of route links that together constitute the resulting
     * route. Each route link contains a path element that consists of a
     * reference to an infrastructure link and the direction of traversal on it.
     * If a matching route could not be found then an empty list is returned.
     */
    fun findRouteViaNetworkNodes(vehicleType: VehicleType, nodeIds: List<Long>): List<RouteLinkDTO>
}
