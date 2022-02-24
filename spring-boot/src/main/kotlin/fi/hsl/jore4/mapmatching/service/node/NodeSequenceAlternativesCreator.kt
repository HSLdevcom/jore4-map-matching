package fi.hsl.jore4.mapmatching.service.node

import fi.hsl.jore4.mapmatching.model.InfrastructureNodeId
import fi.hsl.jore4.mapmatching.model.NodeIdSequence
import fi.hsl.jore4.mapmatching.util.CollectionUtils.filterOutConsecutiveDuplicates

object NodeSequenceAlternativesCreator {

    /**
     * Produce [NodeSequenceAlternatives] object containing possible sequences
     * of infrastructure network node identifiers based on given parameters.
     */
    fun create(nodesToVisit: VisitedNodes): NodeSequenceAlternatives {
        val nodeIdSequences: List<NodeIdSequence> = when (nodesToVisit) {
            is VisitedNodesOnLink -> nodesToVisit.toListOfNodeIdSequences()

            is VisitNodesOnMultipleLinks -> toListOfNodeIdSequences(nodesToVisit)
        }

        return NodeSequenceAlternatives(nodeIdSequences)
    }

    private fun toListOfNodeIdSequences(nodesToVisit: VisitNodesOnMultipleLinks): List<NodeIdSequence> {
        val (nodesToVisitOnStartLink: VisitedNodesOnLink,
            viaNodeIds: List<InfrastructureNodeId>,
            nodesToVisitOnEndLink: VisitedNodesOnLink) = nodesToVisit

        return nodesToVisitOnStartLink.toListOfNodeIdSequences().flatMap { startNodeIdSeq ->

            nodesToVisitOnEndLink.toListOfNodeIdSequences().map { endNodeIdSeq ->

                val concatenatedViaNodeIds: List<InfrastructureNodeId> =
                    startNodeIdSeq.list + viaNodeIds + endNodeIdSeq.list

                NodeIdSequence(filterOutConsecutiveDuplicates(concatenatedViaNodeIds))
            }
        }
    }
}
