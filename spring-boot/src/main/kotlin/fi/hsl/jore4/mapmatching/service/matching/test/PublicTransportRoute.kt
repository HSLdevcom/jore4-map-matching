package fi.hsl.jore4.mapmatching.service.matching.test

import fi.hsl.jore4.mapmatching.model.matching.RoutePoint
import org.geolatte.geom.G2D
import org.geolatte.geom.LineString

/**
 * Route source for map-matching bulk tester
 */
data class PublicTransportRoute(val routeId: String,
                                val routeGeometry: LineString<G2D>,
                                val routePoints: List<RoutePoint>)
