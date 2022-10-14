package fi.hsl.jore4.mapmatching.service.routing

/**
 * Contains additional extra parameters that affect routing algorithm.
 *
 * @property linkQueryDistance the distance in meters within which the closest
 * infrastructure link is searched for each given source point
 * @property simplifyConsecutiveClosedLoopTraversals indicates whether
 * consecutive traversals (full, partial, reversed, reversed partial) on a
 * closed-loop shaped infrastructure link should be replaced by one full
 * traversal in the direction of the first traversal appearing per loop. The
 * handling is applied for all appearances of closed loops in a route. This
 * denotes a compatibility mode for Jore4 where route granularity is defined in
 * terms of whole infrastructure link geometries. Therefore, we may want to
 * prevent inadvertent multi-traversals in closed loops.
 */
data class RoutingExtraParameters(val linkQueryDistance: Int,
                                  val simplifyConsecutiveClosedLoopTraversals: Boolean)
