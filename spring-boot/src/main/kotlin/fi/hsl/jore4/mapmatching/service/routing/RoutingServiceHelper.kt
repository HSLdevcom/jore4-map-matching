package fi.hsl.jore4.mapmatching.service.routing

import fi.hsl.jore4.mapmatching.repository.infrastructure.SnapPointToLinkDTO
import fi.hsl.jore4.mapmatching.repository.infrastructure.SnappedLinkState
import fi.hsl.jore4.mapmatching.service.node.NodeSequenceAlternatives
import fi.hsl.jore4.mapmatching.service.node.NodeSequenceAlternativesCreator
import org.geolatte.geom.G2D
import org.geolatte.geom.Point

object RoutingServiceHelper {

    internal fun findUnmatchedPoints(snaps: Collection<SnapPointToLinkDTO>,
                                     pointsToBeFiltered: List<Point<G2D>>)
        : List<Point<G2D>> {

        val snappedPoints: Set<Point<G2D>> = snaps.map(SnapPointToLinkDTO::point).toSet()

        return pointsToBeFiltered.filter { it !in snappedPoints }
    }

    /**
     * @throws [IllegalArgumentException]
     */
    internal fun createNodeSequenceAlternatives(snaps: Collection<SnapPointToLinkDTO>): NodeSequenceAlternatives {
        val links: List<SnappedLinkState> = snaps.map(SnapPointToLinkDTO::link)

        require(links.isNotEmpty()) { "Must have at least one infrastructure link" }

        val viaLinks: List<SnappedLinkState> = when (links.size) {
            1, 2 -> emptyList()
            else -> links.drop(1).dropLast(1)
        }

        return NodeSequenceAlternativesCreator.create(links.first(), viaLinks, links.last())
    }
}
