package fi.hsl.jore4.mapmatching.service.matching

import fi.hsl.jore4.mapmatching.repository.infrastructure.SnappedPointOnLink

/**
 * Contains data this is used select and prioritise terminus link candidates while map-matching
 * a source route to a target route.
 */
data class TerminusLinkSelectionInput(
    val sourceRouteStartPoint: SourceRouteTerminusPoint,
    val closestStartLinks: List<SnappedPointOnLink>,
    val sourceRouteEndPoint: SourceRouteTerminusPoint,
    val closestEndLinks: List<SnappedPointOnLink>
)
