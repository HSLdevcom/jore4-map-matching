package fi.hsl.jore4.mapmatching.service.node

import fi.hsl.jore4.mapmatching.model.TrafficFlowDirectionType.AGAINST_DIGITISED_DIRECTION
import fi.hsl.jore4.mapmatching.model.TrafficFlowDirectionType.ALONG_DIGITISED_DIRECTION
import fi.hsl.jore4.mapmatching.model.TrafficFlowDirectionType.BIDIRECTIONAL
import fi.hsl.jore4.mapmatching.repository.infrastructure.SnappedLinkState

object SnappedLinkStateExtension {

    fun SnappedLinkState.toVisitedNodes(): VisitedNodesOnLink {
        return if (!isOnLinkWithDiscreteNodes() || isSnappedToStartNode)
            VisitSingleNode(startNodeId)
        else if (isSnappedToEndNode)
            VisitSingleNode(endNodeId)
        else {
            when (trafficFlowDirectionType) {
                BIDIRECTIONAL -> VisitNodesOfSingleLinkBidirectionally(startNodeId, endNodeId)

                ALONG_DIGITISED_DIRECTION -> VisitNodesOfSingleLinkUnidirectionally(startNodeId, endNodeId)

                AGAINST_DIGITISED_DIRECTION -> VisitNodesOfSingleLinkUnidirectionally(endNodeId, startNodeId)
            }
        }
    }
}
