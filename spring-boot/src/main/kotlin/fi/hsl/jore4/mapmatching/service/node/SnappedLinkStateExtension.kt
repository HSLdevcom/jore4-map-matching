package fi.hsl.jore4.mapmatching.service.node

import fi.hsl.jore4.mapmatching.model.InfrastructureNodeId
import fi.hsl.jore4.mapmatching.model.NodeIdSequence
import fi.hsl.jore4.mapmatching.repository.infrastructure.SnappedLinkState

object SnappedLinkStateExtension {

    fun SnappedLinkState.toNodeIdList(): List<InfrastructureNodeId> = listOf(closerNodeId, furtherNodeId)

    fun SnappedLinkState.toNodeIdSequence() = NodeIdSequence(toNodeIdList())

    fun SnappedLinkState.getNodeIdSequenceCombinations(): List<NodeIdSequence> {
        val nodeIds: List<InfrastructureNodeId> = toNodeIdList()

        return listOf(NodeIdSequence(nodeIds), NodeIdSequence(nodeIds.reversed()))
    }
}
