package fi.hsl.jore4.mapmatching.service.node

import fi.hsl.jore4.mapmatching.model.InfrastructureNodeId
import fi.hsl.jore4.mapmatching.model.NodeIdSequence

sealed interface VisitedNodes

data class VisitNodesOnMultipleLinks(
    val nodesToVisitOnStartLink: VisitedNodesOnLink,
    val viaNodeIds: List<InfrastructureNodeId>,
    val nodesToVisitOnEndLink: VisitedNodesOnLink
) :
    VisitedNodes

sealed interface VisitedNodesOnLink : VisitedNodes {
    fun toListOfNodeIds(): List<InfrastructureNodeId>

    fun toListOfNodeIdSequences(): List<NodeIdSequence>
}

data class VisitSingleNode(val nodeId: InfrastructureNodeId) : VisitedNodesOnLink {
    override fun toListOfNodeIds(): List<InfrastructureNodeId> = listOf(nodeId)

    override fun toListOfNodeIdSequences(): List<NodeIdSequence> = listOf(NodeIdSequence(toListOfNodeIds()))
}

data class VisitNodesOfSingleLinkUnidirectionally(
    val startNodeId: InfrastructureNodeId,
    val endNodeId: InfrastructureNodeId
) :
    VisitedNodesOnLink {
    override fun toListOfNodeIds(): List<InfrastructureNodeId> = listOf(startNodeId, endNodeId)

    override fun toListOfNodeIdSequences(): List<NodeIdSequence> = listOf(NodeIdSequence(toListOfNodeIds()))
}

data class VisitNodesOfSingleLinkBidirectionally(
    val firstNodeId: InfrastructureNodeId,
    val secondNodeId: InfrastructureNodeId
) :
    VisitedNodesOnLink {
    override fun toListOfNodeIds(): List<InfrastructureNodeId> = listOf(firstNodeId, secondNodeId)

    override fun toListOfNodeIdSequences(): List<NodeIdSequence> {
        return listOf(
            NodeIdSequence(listOf(firstNodeId, secondNodeId)),
            NodeIdSequence(listOf(secondNodeId, firstNodeId))
        )
    }
}
