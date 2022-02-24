package fi.hsl.jore4.mapmatching.service.node

import fi.hsl.jore4.mapmatching.model.InfrastructureNodeId
import fi.hsl.jore4.mapmatching.model.NodeIdSequence
import fi.hsl.jore4.mapmatching.model.TrafficFlowDirectionType.AGAINST_DIGITISED_DIRECTION
import fi.hsl.jore4.mapmatching.model.TrafficFlowDirectionType.ALONG_DIGITISED_DIRECTION
import fi.hsl.jore4.mapmatching.model.TrafficFlowDirectionType.BIDIRECTIONAL
import fi.hsl.jore4.mapmatching.repository.infrastructure.SnappedLinkState

object SnappedLinkStateExtension {

    fun SnappedLinkState.toNodeIdList(): List<InfrastructureNodeId> {
        return if (hasDiscreteNodes())
            when (trafficFlowDirectionType) {
                BIDIRECTIONAL -> listOf(closerNodeId, furtherNodeId)
                ALONG_DIGITISED_DIRECTION -> listOf(startNodeId, endNodeId)
                AGAINST_DIGITISED_DIRECTION -> listOf(endNodeId, startNodeId)
            }
        else
            listOf(startNodeId)
    }

    fun SnappedLinkState.getNodeIdSequenceCombinations(): List<NodeIdSequence> {
        val nodeIdList: List<InfrastructureNodeId> = toNodeIdList()

        return if (hasDiscreteNodes() && trafficFlowDirectionType == BIDIRECTIONAL)
            listOf(NodeIdSequence(nodeIdList),
                   NodeIdSequence(nodeIdList.reversed()))
        else
            listOf(NodeIdSequence(nodeIdList))
    }
}
