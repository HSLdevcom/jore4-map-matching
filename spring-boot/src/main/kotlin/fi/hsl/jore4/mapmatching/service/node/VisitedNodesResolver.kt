package fi.hsl.jore4.mapmatching.service.node

import fi.hsl.jore4.mapmatching.model.InfrastructureNodeId
import fi.hsl.jore4.mapmatching.model.TrafficFlowDirectionType.AGAINST_DIGITISED_DIRECTION
import fi.hsl.jore4.mapmatching.model.TrafficFlowDirectionType.ALONG_DIGITISED_DIRECTION
import fi.hsl.jore4.mapmatching.model.TrafficFlowDirectionType.BIDIRECTIONAL
import fi.hsl.jore4.mapmatching.repository.infrastructure.SnappedLinkState
import fi.hsl.jore4.mapmatching.service.node.SnappedLinkStateExtension.toVisitedNodes

object VisitedNodesResolver {

    fun resolve(startLink: SnappedLinkState, viaNodeIds: List<InfrastructureNodeId>, endLink: SnappedLinkState)
        : VisitedNodes {

        val reducedViaNodeIds: List<InfrastructureNodeId> = reduceViaNodeIds(startLink, endLink, viaNodeIds)

        if (reducedViaNodeIds.isEmpty() && startLink.isOnSameLinkAs(endLink)) {
            return fromSingleLinkWithoutViaNodes(startLink,
                                                 endLink.closestPointFractionalMeasure)
        }

        return VisitNodesOnMultipleLinks(resolveVisitedNodesOnStartLink(startLink, reducedViaNodeIds),
                                         viaNodeIds,
                                         resolveVisitedNodesOnEndLink(endLink, reducedViaNodeIds))
    }

    private fun reduceViaNodeIds(startLink: SnappedLinkState,
                                 endLink: SnappedLinkState,
                                 viaNodeIds: List<InfrastructureNodeId>)
        : List<InfrastructureNodeId> {

        val firstNodeId: InfrastructureNodeId = startLink.closerNodeId
        val lastNodeId: InfrastructureNodeId = endLink.closerNodeId

        return viaNodeIds
            .dropWhile { it == firstNodeId }
            .dropLastWhile { it == lastNodeId }
    }

    private fun fromSingleLinkWithoutViaNodes(link: SnappedLinkState, secondSnapPointFractionalLocation: Double)
        : VisitedNodes {

        return when (link.hasDiscreteNodes()) {
            true -> link.run {
                when (trafficFlowDirectionType) {
                    BIDIRECTIONAL -> {
                        when (closestPointFractionalMeasure.compareTo(secondSnapPointFractionalLocation)) {
                            -1 -> VisitNodesOfSingleLinkUnidirectionally(startNodeId, endNodeId)
                            1 -> VisitNodesOfSingleLinkUnidirectionally(endNodeId, startNodeId)
                            else -> VisitSingleNode(closerNodeId)
                        }
                    }
                    ALONG_DIGITISED_DIRECTION -> {
                        val oneWayTraversal = VisitNodesOfSingleLinkUnidirectionally(startNodeId, endNodeId)

                        if (closestPointFractionalMeasure > secondSnapPointFractionalLocation)
                            VisitNodesOnMultipleLinks(oneWayTraversal,
                                                      emptyList(),
                                                      oneWayTraversal)
                        else
                            oneWayTraversal
                    }
                    AGAINST_DIGITISED_DIRECTION -> {
                        val oneWayTraversal = VisitNodesOfSingleLinkUnidirectionally(endNodeId, startNodeId)

                        if (closestPointFractionalMeasure < secondSnapPointFractionalLocation)
                            VisitNodesOnMultipleLinks(oneWayTraversal,
                                                      emptyList(),
                                                      oneWayTraversal)
                        else
                            oneWayTraversal
                    }
                }
            }
            false -> VisitSingleNode(link.startNodeId)
        }
    }

    private fun resolveVisitedNodesOnStartLink(startLink: SnappedLinkState,
                                               reducedViaNodeIds: List<InfrastructureNodeId>)
        : VisitedNodesOnLink {

        val snappedTerminusNodeId: InfrastructureNodeId = startLink.closerNodeId

        fun isFurtherNodeOfStartLinkAtStartOfReducedListOfViaNodeIds(): Boolean =
            reducedViaNodeIds
                .firstOrNull()
                ?.takeIf { it == startLink.furtherNodeId } != null

        return startLink.run {
            if (trafficFlowDirectionType == ALONG_DIGITISED_DIRECTION && endNodeId == snappedTerminusNodeId
                || trafficFlowDirectionType == AGAINST_DIGITISED_DIRECTION && startNodeId == snappedTerminusNodeId
                || !isFurtherNodeOfStartLinkAtStartOfReducedListOfViaNodeIds()
            )
                toVisitedNodes()
            else
                VisitSingleNode(snappedTerminusNodeId)
        }
    }

    private fun resolveVisitedNodesOnEndLink(endLink: SnappedLinkState, reducedViaNodeIds: List<InfrastructureNodeId>)
        : VisitedNodesOnLink {

        val snappedTerminusNodeId: InfrastructureNodeId = endLink.closerNodeId

        fun isFurtherNodeOfEndLinkAtEndOfReducedListOfViaNodeIds(): Boolean =
            reducedViaNodeIds
                .lastOrNull()
                ?.takeIf { it == endLink.furtherNodeId } != null

        return endLink.run {
            if (trafficFlowDirectionType == ALONG_DIGITISED_DIRECTION && startNodeId == snappedTerminusNodeId
                || trafficFlowDirectionType == AGAINST_DIGITISED_DIRECTION && endNodeId == snappedTerminusNodeId
                || !isFurtherNodeOfEndLinkAtEndOfReducedListOfViaNodeIds()
            )
                toVisitedNodes()
            else
                VisitSingleNode(snappedTerminusNodeId)
        }
    }
}
