package fi.hsl.jore4.mapmatching.service.matching

import fi.hsl.jore4.mapmatching.repository.routing.PgRoutingPoint

/**
 * Models a candidate for start/stop point for a map-matched target route. This class is involved in
 * map-matching a route via network links (graph edges).
 *
 * @property targetPoint an input point for pgRouting via-edge functions. Could be either a point
 * along infrastructure link or a network node.
 * @property isAStopPointMatchedByNationalId indicates whether the point is a public transport stop
 * point that is matched by a national ID obtained from a map-matching request.
 * @property closestDistance the distance from the point to the closest infrastructure link. Used in
 * sorting terminus point candidates.
 */
data class TerminusPointCandidate(val targetPoint: PgRoutingPoint,
                                  val isAStopPointMatchedByNationalId: Boolean,
                                  val closestDistance: Double)
