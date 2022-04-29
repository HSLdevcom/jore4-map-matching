package fi.hsl.jore4.mapmatching.service.routing

import fi.hsl.jore4.mapmatching.model.HasInfrastructureNodeId
import fi.hsl.jore4.mapmatching.model.InfrastructureNodeId
import fi.hsl.jore4.mapmatching.repository.infrastructure.SnapPointToLinkDTO
import fi.hsl.jore4.mapmatching.repository.infrastructure.SnappedLinkState
import fi.hsl.jore4.mapmatching.service.node.NodeSequenceCandidatesBetweenSnappedLinks
import fi.hsl.jore4.mapmatching.service.node.NodeSequenceCombinationsCreator
import fi.hsl.jore4.mapmatching.service.node.VisitedNodes
import fi.hsl.jore4.mapmatching.service.node.VisitedNodesResolver
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
     * @throws [IllegalArgumentException] if [snaps] is empty
     */
    internal fun createNodeSequenceCandidates(snaps: Collection<SnapPointToLinkDTO>)
        : NodeSequenceCandidatesBetweenSnappedLinks {

        val links: List<SnappedLinkState> = snaps.map(SnapPointToLinkDTO::link)

        require(links.isNotEmpty()) { "Must have at least one infrastructure link" }

        val viaNodeIds: List<InfrastructureNodeId> = when (links.size) {
            1, 2 -> emptyList()
            else -> links.drop(1).dropLast(1).map(HasInfrastructureNodeId::getInfrastructureNodeId)
        }

        val snappedStartLink: SnappedLinkState = links.first()
        val snappedEndLink: SnappedLinkState = links.last()

        val nodesToVisit: VisitedNodes = VisitedNodesResolver.resolve(snappedStartLink, viaNodeIds, snappedEndLink)

        return NodeSequenceCandidatesBetweenSnappedLinks(snappedStartLink,
                                                         snappedEndLink,
                                                         NodeSequenceCombinationsCreator.create(nodesToVisit))
    }
}
