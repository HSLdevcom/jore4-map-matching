package fi.hsl.jore4.mapmatching.api

import fi.hsl.jore4.mapmatching.model.LatLng
import javax.validation.Valid

/**
 * Contains input data for finding a route via given points through the
 * infrastructure network provided by the system.
 *
 * @property routePoints route points of the route
 * @property linkSearchRadius optional parameter for the link search radius
 */
data class PublicTransportRouteFindRequestDTO(@field:Valid val routePoints: List<LatLng>,
                                              val linkSearchRadius: Int?)
