package fi.hsl.jore4.mapmatching.service.routing

import fi.hsl.jore4.mapmatching.repository.infrastructure.SnapPointToLinkDTO
import fi.hsl.jore4.mapmatching.repository.infrastructure.SnappedLinkState
import fi.hsl.jore4.mapmatching.service.node.NodeSequenceProducer
import org.geolatte.geom.G2D
import org.geolatte.geom.Point

object RoutingServiceHelper {

    internal fun findUnmatchedPoints(snaps: Collection<SnapPointToLinkDTO>,
                                     allPoints: List<Point<G2D>>)
        : List<Point<G2D>> {

        val snappedPoints = snaps.map { it.point }.toSet()

        return allPoints.filter { !snappedPoints.contains(it) }
    }

    internal fun createNodeSequenceProducer(snaps: Collection<SnapPointToLinkDTO>): NodeSequenceProducer {
        val links: List<SnappedLinkState> = snaps.map { it.link }

        return NodeSequenceProducer(links)
    }
}
