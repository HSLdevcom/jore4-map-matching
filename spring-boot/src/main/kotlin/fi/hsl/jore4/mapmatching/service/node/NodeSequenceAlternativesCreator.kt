package fi.hsl.jore4.mapmatching.service.node

import fi.hsl.jore4.mapmatching.model.HasInfrastructureNodeId
import fi.hsl.jore4.mapmatching.model.InfrastructureLinkId
import fi.hsl.jore4.mapmatching.model.InfrastructureNodeId
import fi.hsl.jore4.mapmatching.model.NodeIdSequence
import fi.hsl.jore4.mapmatching.repository.infrastructure.SnappedLinkState
import fi.hsl.jore4.mapmatching.service.node.SnappedLinkStateExtension.getNodeIdSequenceCombinations
import fi.hsl.jore4.mapmatching.service.node.SnappedLinkStateExtension.toNodeIdSequence
import fi.hsl.jore4.mapmatching.util.CollectionUtils.filterOutConsecutiveDuplicates

object NodeSequenceAlternativesCreator {

    /**
     * Produce [NodeSequenceAlternatives] object containing possible sequences
     * of infrastructure network node identifiers based on given parameters.
     * Variance is caused by using different ordering of endpoint nodes for both
     * start and end link.
     */
    fun create(startLink: SnappedLinkState, viaNodeResolvers: List<HasInfrastructureNodeId>, endLink: SnappedLinkState)
        : NodeSequenceAlternatives {

        if (viaNodeResolvers.isEmpty()) {
            return createWithoutViaNodes(startLink, endLink)
        }

        val viaNodeIds: List<InfrastructureNodeId> = viaNodeResolvers.map { it.getInfrastructureNodeId() }

        val filteredViaNodeIds: List<InfrastructureNodeId> = filterOutConsecutiveDuplicates(viaNodeIds)

        val viaNodeIdsBelongingToStartLink = filteredViaNodeIds.takeWhile { startLink.hasNode(it) }

        val viaNodeIdsAfterStartLinkNodesRemoved: List<InfrastructureNodeId> =
            filteredViaNodeIds.drop(viaNodeIdsBelongingToStartLink.size)

        val viaNodeIdsBelongingToEndLink: List<InfrastructureNodeId> = viaNodeIdsAfterStartLinkNodesRemoved
            .reversed()
            .takeWhile { endLink.hasNode(it) }
            .reversed()

        val viaNodeIdsAfterNodesOfTerminusLinksRemoved: List<InfrastructureNodeId> =
            viaNodeIdsAfterStartLinkNodesRemoved.dropLast(viaNodeIdsBelongingToEndLink.size)

        val mergedNodeIdsOnStartLink: List<InfrastructureNodeId> =
            mergeNodeIdsOnStartLink(startLink, viaNodeIdsBelongingToStartLink)

        val mergedNodeIdsOnEndLink: List<InfrastructureNodeId> =
            mergeNodeIdsOnEndLink(endLink, viaNodeIdsBelongingToEndLink)

        val mergedNodeIdsOnStartAndEndLink: List<InfrastructureNodeId> =
            filterOutConsecutiveDuplicates(mergedNodeIdsOnStartLink + mergedNodeIdsOnEndLink)

        // not overlapping with closer nodes of terminus links
        val nonOverlappingViaNodeIdSequence: NodeIdSequence

        if (viaNodeIdsAfterNodesOfTerminusLinksRemoved.isEmpty() && mergedNodeIdsOnStartAndEndLink.size <= 2) {
            // There are no via nodes left after "cleaning" data.
            nonOverlappingViaNodeIdSequence = NodeIdSequence.empty()

            // This very special corner case needs to be treated separately in order to have a well-behaving algorithm.
            if (startLink.infrastructureLinkId == endLink.infrastructureLinkId) {
                return createResponseForSingleLinkNoViaNodes(startLink, endLink.infrastructureLinkId)
            }
        } else {
            nonOverlappingViaNodeIdSequence = NodeIdSequence(mergedNodeIdsOnStartLink.drop(1)
                                                                 + viaNodeIdsAfterNodesOfTerminusLinksRemoved
                                                                 + mergedNodeIdsOnEndLink.dropLast(1))
        }

        val nodeIdSequencesOnStartLink: List<NodeIdSequence> = getNodeIdSequencesOnStartLink(startLink,
                                                                                             mergedNodeIdsOnStartLink)

        val nodeIdSequencesOnEndLink: List<NodeIdSequence> = getNodeIdSequencesOnEndLink(endLink,
                                                                                         mergedNodeIdsOnEndLink)

        val nodeIdSequenceCombos: List<NodeIdSequence> = nodeIdSequencesOnStartLink
            .flatMap { startNodeIdSeq ->
                nodeIdSequencesOnEndLink.map { endNodeIdSeq ->
                    startNodeIdSeq
                        .concat(NodeIdSequence(viaNodeIdsAfterNodesOfTerminusLinksRemoved))
                        .concat(endNodeIdSeq)
                }
            }

        return NodeSequenceAlternatives(startLink.infrastructureLinkId,
                                        endLink.infrastructureLinkId,
                                        nonOverlappingViaNodeIdSequence,
                                        nodeIdSequenceCombos)
    }

    private fun createWithoutViaNodes(startLink: SnappedLinkState, endLink: SnappedLinkState)
        : NodeSequenceAlternatives {

        val startLinkId: InfrastructureLinkId = startLink.infrastructureLinkId
        val endLinkId: InfrastructureLinkId = endLink.infrastructureLinkId

        if (startLinkId == endLinkId) {
            return createResponseForSingleLinkNoViaNodes(startLink, endLinkId)
        }

        val startLinkNodeIdSequences: List<NodeIdSequence> = startLink.getNodeIdSequenceCombinations()
        val endLinkNodeIdSequences: List<NodeIdSequence> = endLink.getNodeIdSequenceCombinations()

        val nodeIdSequenceCombos: List<NodeIdSequence> = startLinkNodeIdSequences
            .flatMap { startLinkNodeIdSeq ->
                endLinkNodeIdSequences.map { endLinkNodeIdSeq ->
                    startLinkNodeIdSeq.concat(endLinkNodeIdSeq)
                }
            }

        val filteredNodeIdSequences: List<NodeIdSequence> = when (startLink.hasSharedNode(endLink)) {
            true -> nodeIdSequenceCombos.map { it.duplicatesRemoved() }
            false -> nodeIdSequenceCombos
        }

        return NodeSequenceAlternatives(startLinkId, endLinkId, NodeIdSequence.empty(), filteredNodeIdSequences)
    }

    private fun mergeNodeIdsOnStartLink(startLink: SnappedLinkState,
                                        followingViaNodeIdsBelongingToStartLink: List<InfrastructureNodeId>)
        : List<InfrastructureNodeId> {

        return if (followingViaNodeIdsBelongingToStartLink.isEmpty())
            listOf(startLink.closerNodeId)
        else if (startLink.closerNodeId == followingViaNodeIdsBelongingToStartLink.first())
            followingViaNodeIdsBelongingToStartLink
        else
            listOf(startLink.closerNodeId) + followingViaNodeIdsBelongingToStartLink
    }

    private fun mergeNodeIdsOnEndLink(endLink: SnappedLinkState,
                                      precedingViaNodeIdsBelongingToEndLink: List<InfrastructureNodeId>)
        : List<InfrastructureNodeId> {

        return if (precedingViaNodeIdsBelongingToEndLink.isEmpty())
            listOf(endLink.closerNodeId)
        else if (precedingViaNodeIdsBelongingToEndLink.last() == endLink.closerNodeId)
            precedingViaNodeIdsBelongingToEndLink
        else
            precedingViaNodeIdsBelongingToEndLink + endLink.closerNodeId
    }

    private fun getNodeIdSequencesOnStartLink(startLink: SnappedLinkState,
                                              nodeIdsOnStartLink: List<InfrastructureNodeId>): List<NodeIdSequence> {

        return when (nodeIdsOnStartLink.size) {
            0 -> throw IllegalStateException("Cannot have zero node snaps on start link")
            1 -> startLink.getNodeIdSequenceCombinations()
            else -> listOf(NodeIdSequence(nodeIdsOnStartLink))
        }
    }

    private fun getNodeIdSequencesOnEndLink(endLink: SnappedLinkState,
                                            nodeIdsOnEndLink: List<InfrastructureNodeId>): List<NodeIdSequence> {

        return when (nodeIdsOnEndLink.size) {
            0 -> throw IllegalStateException("Cannot have zero node snaps on end link")
            1 -> endLink.getNodeIdSequenceCombinations()
            else -> listOf(NodeIdSequence(nodeIdsOnEndLink))
        }
    }

    private fun createResponseForSingleLinkNoViaNodes(startLink: SnappedLinkState,
                                                      endLinkId: InfrastructureLinkId): NodeSequenceAlternatives {

        return NodeSequenceAlternatives(startLink.infrastructureLinkId,
                                        endLinkId,
                                        NodeIdSequence.empty(),
                                        listOf(startLink.toNodeIdSequence()))

    }
}