package fi.hsl.jore4.mapmatching.service.matching

import fi.hsl.jore4.mapmatching.model.matching.TerminusType
import org.geolatte.geom.G2D
import org.geolatte.geom.Point

/**
 * Models the first or last point on route.
 */
data class RouteTerminusPoint(val location: Point<G2D>,
                              val terminusType: TerminusType,
                              val isStopPoint: Boolean,
                              val stopPointNationalId: Int?)
