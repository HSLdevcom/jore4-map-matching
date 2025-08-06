package fi.hsl.jore4.mapmatching.repository.routing

import fi.hsl.jore4.mapmatching.model.NodeIdSequence
import fi.hsl.jore4.mapmatching.model.VehicleType

interface IRoutingRepository {
    /**
     * Finds the shortest route through infrastructure network via given nodes
     * (graph vertices) and constrained by the given vehicle type and optional
     * buffer area.
     *
     * @param nodeIdSequence sequence of identifiers for infrastructure network
     * nodes that the route must pass through. The sequence must not contain
     * consecutive duplicate entries.
     * @param vehicleType vehicle type constraint for the route. The resulting
     * route must consist of only those infrastructure links that are safely
     * traversable by the given vehicle type.
     * @param bufferAreaRestriction contains data with which geometrical
     * restriction for the target set of infrastructure links can be defined
     * while finding route through infrastructure network.
     *
     * @return a list of route links that together constitute the resulting
     * route. Each route link contains a path element that consists of a
     * reference to an infrastructure link and the direction of traversal on it.
     * If a route could not be found then an empty list is returned.
     */
    fun findRouteViaNetworkNodes(
        nodeIdSequence: NodeIdSequence,
        vehicleType: VehicleType,
        bufferAreaRestriction: BufferAreaRestriction? = null
    ): List<RouteLink>

    /**
     * Finds the shortest route through infrastructure network via given nodes
     * (graph vertices) and constrained by the given vehicle type and optional
     * buffer area.
     *
     * @param nodeIdSequence sequence of identifiers for infrastructure network
     * nodes that the route must pass through. The sequence must not contain
     * consecutive duplicate entries.
     * @param vehicleType vehicle type constraint for the route. The resulting
     * route must consist of only those infrastructure links that are safely
     * traversable by the given vehicle type.
     * @param fractionalStartLocationOnFirstLink the location of the route's
     * start point as a fraction of the length of the first infrastructure link
     * on the route. The fraction must be in range [0.0, 1.0].
     * @param fractionalEndLocationOnLastLink the location of the route's end
     * point as a fraction of the length of the last infrastructure link on the
     * route. The fraction must be in range [0.0, 1.0].
     * @param bufferAreaRestriction contains data with which geometrical
     * restriction for the target set of infrastructure links can be defined
     * while finding route through infrastructure network.
     *
     * @return a list of route links that together constitute the resulting
     * route. Each route link contains a path element that consists of a
     * reference to an infrastructure link and the direction of traversal on it.
     * If a route could not be found then an empty list is returned.
     */
    fun findRouteViaNetworkNodes(
        nodeIdSequence: NodeIdSequence,
        vehicleType: VehicleType,
        fractionalStartLocationOnFirstLink: Double,
        fractionalEndLocationOnLastLink: Double,
        bufferAreaRestriction: BufferAreaRestriction? = null
    ): List<RouteLink>

    /**
     * Finds the shortest route through infrastructure network via given points
     * along infrastructure links (graph edges) and constrained by the given
     * vehicle type and optional buffer area.
     *
     * @param points list of route points that the route must pass through. Note
     * that at least one source point must not be an infrastructure node but a
     * fractional location along an infrastructure link.
     * @param vehicleType vehicle type constraint for the route. The resulting
     * route must consist of only those infrastructure links that are safely
     * traversable by the given vehicle type.
     * @param bufferAreaRestriction contains data with which geometrical
     * restriction for the target set of infrastructure links can be defined
     * while finding route through infrastructure network.
     *
     * @return a list of route links that together constitute the resulting
     * route. Each route link contains a path element that consists of a
     * reference to an infrastructure link and the direction of traversal on it.
     * If a route could not be found, then an empty list is returned.
     */
    fun findRouteViaPointsOnLinks(
        points: List<PgRoutingPoint>,
        vehicleType: VehicleType,
        bufferAreaRestriction: BufferAreaRestriction? = null
    ): List<RouteLink>
}
