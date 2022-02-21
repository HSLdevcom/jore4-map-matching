package fi.hsl.jore4.mapmatching.service.node

import fi.hsl.jore4.mapmatching.model.InfrastructureNodeId
import fi.hsl.jore4.mapmatching.model.NodeIdSequence
import fi.hsl.jore4.mapmatching.repository.infrastructure.SnappedLinkState

object SnappedLinkStateExtension {

    fun SnappedLinkState.toNodeIdList(): List<InfrastructureNodeId> {
        return if (hasDiscreteNodes())
            when (trafficFlowDirectionType) {
                3 -> listOf(endNodeId, startNodeId)
                4 -> listOf(startNodeId, endNodeId)
                else -> listOf(closerNodeId, furtherNodeId)
            }
        else
            listOf(startNodeId)
    }

    fun SnappedLinkState.getNodeIdSequenceCombinations(): List<NodeIdSequence> {
        val nodeIdList: List<InfrastructureNodeId> = toNodeIdList()

        return if (hasDiscreteNodes() && trafficFlowDirectionType == 2)
            listOf(NodeIdSequence(nodeIdList),
                   NodeIdSequence(nodeIdList.reversed()))
        else
            listOf(NodeIdSequence(nodeIdList))
    }
}
