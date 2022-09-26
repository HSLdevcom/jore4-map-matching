package fi.hsl.jore4.mapmatching.service.routing

import fi.hsl.jore4.mapmatching.model.LinkSide
import fi.hsl.jore4.mapmatching.repository.infrastructure.SnapPointToLinkDTO
import fi.hsl.jore4.mapmatching.repository.infrastructure.SnappedLinkState
import fi.hsl.jore4.mapmatching.repository.routing.FractionalLocationAlongLink
import fi.hsl.jore4.mapmatching.repository.routing.NetworkNode
import fi.hsl.jore4.mapmatching.repository.routing.PgRoutingPoint
import org.geolatte.geom.G2D
import org.geolatte.geom.Point

object RoutingServiceHelper {

    internal fun findUnmatchedPoints(snaps: Collection<SnapPointToLinkDTO>,
                                     pointsToBeFiltered: List<Point<G2D>>)
        : List<Point<G2D>> {

        val snappedPoints: Set<Point<G2D>> = snaps.map(SnapPointToLinkDTO::point).toSet()

        return pointsToBeFiltered.filter { it !in snappedPoints }
    }

    fun toPgRoutingPoint(pointOnLink: SnappedLinkState): PgRoutingPoint {
        return if (pointOnLink.isSnappedToStartNode)
            NetworkNode(pointOnLink.startNodeId)
        else if (pointOnLink.isSnappedToEndNode)
            NetworkNode(pointOnLink.endNodeId)
        else
            FractionalLocationAlongLink(pointOnLink.infrastructureLinkId,
                                        pointOnLink.closestPointFractionalMeasure,
                                        LinkSide.BOTH)
    }
}
