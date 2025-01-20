package fi.hsl.jore4.mapmatching.repository.infrastructure

import org.geolatte.geom.G2D
import org.geolatte.geom.Point

// Result of finding multiple closest links to a point
data class SnapPointToLinksResult(
    val point: Point<G2D>,
    val queryDistance: Double,
    val limit: Int,
    val closestLinks: List<SnappedPointOnLink>
)
