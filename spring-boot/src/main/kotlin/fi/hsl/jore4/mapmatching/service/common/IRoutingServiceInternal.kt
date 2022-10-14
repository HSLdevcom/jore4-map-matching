package fi.hsl.jore4.mapmatching.service.common

import fi.hsl.jore4.mapmatching.model.NodeIdSequence
import fi.hsl.jore4.mapmatching.model.VehicleType
import fi.hsl.jore4.mapmatching.repository.routing.BufferAreaRestriction
import fi.hsl.jore4.mapmatching.repository.routing.PgRoutingPoint
import fi.hsl.jore4.mapmatching.repository.routing.RouteLinkDTO

interface IRoutingServiceInternal {

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
     * @param fractionalStartLocationOnFirstLink the location of route's start
     * point as a fraction of the length of the first infrastructure link on the
     * route. The fraction must be in range [0.0, 1.0].
     * @param fractionalEndLocationOnLastLink the location of route's end point
     * as a fraction of the length of the last infrastructure link on the route.
     * The fraction must be in range [0.0, 1.0].
     * @param bufferAreaRestriction contains data with which geometrical
     * restriction for the target set of infrastructure links can be defined
     * while finding route through infrastructure network.
     *
     * @return a list of route links that together constitute the resulting
     * route. Each route link contains a path element that consists of a
     * reference to an infrastructure link and the direction of traversal on it.
     * If a route could not be found then an empty list is returned.
     */
    fun findRouteViaNodes(nodeIdSequence: NodeIdSequence,
                          vehicleType: VehicleType,
                          fractionalStartLocationOnFirstLink: Double,
                          fractionalEndLocationOnLastLink: Double,
                          bufferAreaRestriction: BufferAreaRestriction? = null)
        : List<RouteLinkDTO>

    /**
     * Find the shortest route through infrastructure network via given points
     * and constrained by the given vehicle type and optional buffer area.
     *
     * @param points list of route points that the route must pass through.
     * @param vehicleType vehicle type constraint for the route. Resulting route
     * must consist of only those infrastructure links that are safely
     * traversable by the given vehicle type.
     * @param simplifyConsecutiveClosedLoopTraversals indicates whether
     * consecutive traversals (full, partial, reversed, reversed partial) on a
     * closed-loop shaped infrastructure link should be replaced by one full
     * traversal in the direction of the first traversal appearing per loop. The
     * handling is applied for all appearances of closed loops in a route. This
     * denotes a compatibility mode for Jore4 where route granularity is defined
     * in terms of whole infrastructure link geometries. Therefore, we may want
     * to prevent inadvertent multi-traversals in closed loops.
     * @param bufferAreaRestriction contains data with which geometrical
     * restriction for the target set of infrastructure links can be defined
     * while finding route through infrastructure network.
     *
     * @return a list of route links that together constitute the resulting
     * route. Each route link contains a path element that consists of a
     * reference to an infrastructure link and the direction of traversal on it.
     * If a route could not be found then an empty list is returned.
     */
    fun findRouteViaPoints(points: List<PgRoutingPoint>,
                           vehicleType: VehicleType,
                           simplifyConsecutiveClosedLoopTraversals: Boolean,
                           bufferAreaRestriction: BufferAreaRestriction? = null)
        : List<RouteLinkDTO>
}
