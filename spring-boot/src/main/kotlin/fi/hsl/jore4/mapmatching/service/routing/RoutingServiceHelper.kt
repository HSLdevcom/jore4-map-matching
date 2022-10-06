package fi.hsl.jore4.mapmatching.service.routing

import fi.hsl.jore4.mapmatching.repository.infrastructure.SnapPointToLinkResult
import org.geolatte.geom.G2D
import org.geolatte.geom.Point

object RoutingServiceHelper {

    internal fun findUnmatchedPoints(snaps: Collection<SnapPointToLinkResult>,
                                     pointsToBeFiltered: List<Point<G2D>>)
        : List<Point<G2D>> {

        val snappedLocations: Set<Point<G2D>> = snaps.map(SnapPointToLinkResult::point).toSet()

        return pointsToBeFiltered.filter { it !in snappedLocations }
    }
}
