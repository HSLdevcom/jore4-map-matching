package fi.hsl.jore4.mapmatching.api

import fi.hsl.jore4.mapmatching.model.LatLng
import javax.validation.Valid

/**
 * Contains input data for finding a route via given points through the
 * infrastructure network provided by the system.
 *
 * @property routePoints route points of the route
 * @property linkSearchRadius optional parameter for the link search radius
 * @property simplifyClosedLoopTraversals optional parameter that indicates
 * whether consecutive traversals (full, partial, reversed, reversed partial) on
 * a closed-loop shaped infrastructure link should be replaced by one full
 * traversal in the direction of the first traversal appearing per loop. The
 * handling is applied for all appearances of closed loops in a route. This
 * denotes a compatibility mode for Jore4 where route granularity is defined in
 * terms of whole infrastructure link geometries. Therefore, we may want to
 * prevent inadvertent multi-traversals in closed loops.
 */
data class PublicTransportRouteFindRequestDTO(@field:Valid val routePoints: List<LatLng>,
                                              val linkSearchRadius: Int?,
                                              val simplifyClosedLoopTraversals: Boolean?)
