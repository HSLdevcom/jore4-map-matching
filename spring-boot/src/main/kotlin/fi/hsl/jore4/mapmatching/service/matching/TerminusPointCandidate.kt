package fi.hsl.jore4.mapmatching.service.matching

import fi.hsl.jore4.mapmatching.repository.routing.PgRoutingPoint

/**
 * Models a candidate for start/stop point for a map-matched target route. This class is involved in
 * map-matching a route via network links (graph edges).
 *
 * @property targetPoint a point in the infrastructure network to which a start/stop observation
 * point from map-matching request could be snapped. This target point is used as input for
 * pgRouting via-edge functions. Can be either a virtual node (point along infrastructure link) or
 * a real network mode.
 * @property isAStopPointMatchedByNationalId indicates whether the target point represents a public
 * transport stop point that is matched by a national ID obtained from a map-matching request.
 * @property snapDistance the distance to target point from observation point of map-matching
 * request. This is used while sorting terminus point candidates.
 */
data class TerminusPointCandidate(val targetPoint: PgRoutingPoint,
                                  val isAStopPointMatchedByNationalId: Boolean,
                                  val snapDistance: Double)
