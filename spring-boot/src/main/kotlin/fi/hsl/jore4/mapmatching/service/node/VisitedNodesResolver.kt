package fi.hsl.jore4.mapmatching.service.node

import fi.hsl.jore4.mapmatching.model.InfrastructureNodeId
import fi.hsl.jore4.mapmatching.model.TrafficFlowDirectionType.AGAINST_DIGITISED_DIRECTION
import fi.hsl.jore4.mapmatching.model.TrafficFlowDirectionType.ALONG_DIGITISED_DIRECTION
import fi.hsl.jore4.mapmatching.model.TrafficFlowDirectionType.BIDIRECTIONAL
import fi.hsl.jore4.mapmatching.repository.infrastructure.SnappedPointOnLink
import fi.hsl.jore4.mapmatching.service.node.SnappedPointOnLinkExtension.toVisitedNodes

object VisitedNodesResolver {
    fun resolve(
        pointOnStartLink: SnappedPointOnLink,
        viaNodeIds: List<InfrastructureNodeId>,
        pointOnEndLink: SnappedPointOnLink
    ): VisitedNodes {
        val reducedViaNodeIds: List<InfrastructureNodeId> =
            reduceViaNodeIds(pointOnStartLink, pointOnEndLink, viaNodeIds)

        if (reducedViaNodeIds.isEmpty() && pointOnStartLink.isOnSameLinkAs(pointOnEndLink)) {
            return fromSingleLinkWithoutViaNodes(
                pointOnStartLink,
                pointOnEndLink.closestPointFractionalMeasure
            )
        }

        return VisitNodesOnMultipleLinks(
            resolveVisitedNodesOnStartLink(pointOnStartLink, reducedViaNodeIds),
            viaNodeIds,
            resolveVisitedNodesOnEndLink(pointOnEndLink, reducedViaNodeIds)
        )
    }

    private fun reduceViaNodeIds(
        pointOnStartLink: SnappedPointOnLink,
        pointOnEndLink: SnappedPointOnLink,
        viaNodeIds: List<InfrastructureNodeId>
    ): List<InfrastructureNodeId> {
        val firstNodeId: InfrastructureNodeId = pointOnStartLink.closerNodeId
        val lastNodeId: InfrastructureNodeId = pointOnEndLink.closerNodeId

        return viaNodeIds
            .dropWhile { it == firstNodeId }
            .dropLastWhile { it == lastNodeId }
    }

    private fun fromSingleLinkWithoutViaNodes(
        pointOnLink: SnappedPointOnLink,
        secondSnapPointFractionalLocation: Double
    ): VisitedNodes {
        return when (pointOnLink.isOnLinkWithDiscreteNodes()) {
            true ->
                pointOnLink.run {
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

                            if (closestPointFractionalMeasure > secondSnapPointFractionalLocation) {
                                VisitNodesOnMultipleLinks(
                                    oneWayTraversal,
                                    emptyList(),
                                    oneWayTraversal
                                )
                            } else {
                                oneWayTraversal
                            }
                        }
                        AGAINST_DIGITISED_DIRECTION -> {
                            val oneWayTraversal = VisitNodesOfSingleLinkUnidirectionally(endNodeId, startNodeId)

                            if (closestPointFractionalMeasure < secondSnapPointFractionalLocation) {
                                VisitNodesOnMultipleLinks(
                                    oneWayTraversal,
                                    emptyList(),
                                    oneWayTraversal
                                )
                            } else {
                                oneWayTraversal
                            }
                        }
                    }
                }
            false -> VisitSingleNode(pointOnLink.startNodeId)
        }
    }

    private fun resolveVisitedNodesOnStartLink(
        pointOnStartLink: SnappedPointOnLink,
        reducedViaNodeIds: List<InfrastructureNodeId>
    ): VisitedNodesOnLink {
        val snappedTerminusNodeId: InfrastructureNodeId = pointOnStartLink.closerNodeId

        fun isFurtherNodeOfStartLinkAtStartOfReducedListOfViaNodeIds(): Boolean =
            reducedViaNodeIds
                .firstOrNull()
                ?.takeIf { it == pointOnStartLink.furtherNodeId } != null

        return pointOnStartLink.run {
            if (trafficFlowDirectionType == ALONG_DIGITISED_DIRECTION && endNodeId == snappedTerminusNodeId ||
                trafficFlowDirectionType == AGAINST_DIGITISED_DIRECTION && startNodeId == snappedTerminusNodeId ||
                !isFurtherNodeOfStartLinkAtStartOfReducedListOfViaNodeIds()
            ) {
                toVisitedNodes()
            } else {
                VisitSingleNode(snappedTerminusNodeId)
            }
        }
    }

    private fun resolveVisitedNodesOnEndLink(
        pointOnEndLink: SnappedPointOnLink,
        reducedViaNodeIds: List<InfrastructureNodeId>
    ): VisitedNodesOnLink {
        val snappedTerminusNodeId: InfrastructureNodeId = pointOnEndLink.closerNodeId

        fun isFurtherNodeOfEndLinkAtEndOfReducedListOfViaNodeIds(): Boolean =
            reducedViaNodeIds
                .lastOrNull()
                ?.takeIf { it == pointOnEndLink.furtherNodeId } != null

        return pointOnEndLink.run {
            if (trafficFlowDirectionType == ALONG_DIGITISED_DIRECTION && startNodeId == snappedTerminusNodeId ||
                trafficFlowDirectionType == AGAINST_DIGITISED_DIRECTION && endNodeId == snappedTerminusNodeId ||
                !isFurtherNodeOfEndLinkAtEndOfReducedListOfViaNodeIds()
            ) {
                toVisitedNodes()
            } else {
                VisitSingleNode(snappedTerminusNodeId)
            }
        }
    }
}
