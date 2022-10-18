package fi.hsl.jore4.mapmatching.service.matching

import fi.hsl.jore4.mapmatching.repository.infrastructure.SnappedLinkState

/**
 * Models an infrastructure link as the candidate for the first/last link for a map-matched target
 * route. This class is involved in map-matching a route via network nodes (graph vertices).
 *
 * @property snappedLink a snapped point on infrastructure link at either end of route
 * @property terminusStopPointMatchFoundByNationalId indicates whether the infrastructure link ID of
 * [snappedLink] is the same as is associated with the public transport stop point matched by
 * a national ID obtained from a map-matching request.
 */
data class TerminusLinkCandidate(val snappedLink: SnappedLinkState,
                                 val terminusStopPointMatchFoundByNationalId: Boolean)
