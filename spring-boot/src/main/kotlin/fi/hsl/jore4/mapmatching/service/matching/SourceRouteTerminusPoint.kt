package fi.hsl.jore4.mapmatching.service.matching

import fi.hsl.jore4.mapmatching.model.matching.TerminusType
import org.geolatte.geom.G2D
import org.geolatte.geom.Point

/**
 * Models the first/last point on a source route that is to be map-matched. This is used while
 * searching start/end link candidates for a target route. An infrastructure link along which
 * a terminus stop point is located is prioritised over other link candidates.
 *
 * @property location the coordinates for a terminus point of the route. This holds the first/last
 * coordinate of the LineString geometry of the source route.
 * @property terminusType indicates whether this instance denotes a start or end point of a route
 * @property isStopPoint indicates whether the route is terminated at a public transport stop point
 * @property stopPointNationalId the national ID of the public transport stop at a terminus point of
 * the source route in case the route starts from/ends at a stop point
 */
data class SourceRouteTerminusPoint(val location: Point<G2D>,
                                    val terminusType: TerminusType,
                                    val isStopPoint: Boolean,
                                    val stopPointNationalId: Int?)
