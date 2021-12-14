package fi.hsl.jore4.mapmatching.service.node

import fi.hsl.jore4.mapmatching.model.HasInfrastructureNodeId
import fi.hsl.jore4.mapmatching.repository.infrastructure.SnappedLinkState
import fi.hsl.jore4.mapmatching.service.node.SnappedLinkStateExtension.getNodeIdSequenceCombinations
import fi.hsl.jore4.mapmatching.service.node.SnappedLinkStateExtension.toNodeIdList
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

        val viaNodeIds: List<Long> = viaNodeResolvers.map { it.getInfrastructureNodeId() }

        val filteredViaNodeIds: List<Long> = filterOutConsecutiveDuplicates(viaNodeIds)

        val viaNodeIdsBelongingToStartLink = filteredViaNodeIds.takeWhile { startLink.hasNode(it) }

        val viaNodeIdsAfterStartLinkNodesRemoved: List<Long> =
            filteredViaNodeIds.drop(viaNodeIdsBelongingToStartLink.size)

        val viaNodeIdsBelongingToEndLink: List<Long> = viaNodeIdsAfterStartLinkNodesRemoved
            .reversed()
            .takeWhile { endLink.hasNode(it) }
            .reversed()

        val nodeIdSequencesOnStartLink: List<List<Long>> =
            resolveNodeIdSequencesOnStartLink(startLink, viaNodeIdsBelongingToStartLink)

        val nodeIdSequencesOnEndLink: List<List<Long>> =
            resolveNodeIdSequencesOnEndLink(endLink, viaNodeIdsBelongingToEndLink)

        val viaNodeIdSeqAfterNodesOfTerminusLinksRemoved: List<Long> =
            viaNodeIdsAfterStartLinkNodesRemoved.dropLast(viaNodeIdsBelongingToEndLink.size)

        val nodeIdSequenceCombos: List<List<Long>> = nodeIdSequencesOnStartLink
            .flatMap { startNodeIdSeq ->
                nodeIdSequencesOnEndLink.map { endNodeIdSeq ->
                    when (viaNodeIdSeqAfterNodesOfTerminusLinksRemoved.isNotEmpty()) {
                        true -> startNodeIdSeq + viaNodeIdSeqAfterNodesOfTerminusLinksRemoved + endNodeIdSeq
                        false -> startNodeIdSeq + endNodeIdSeq
                    }
                }
            }

        return NodeSequenceAlternatives(startLink.infrastructureLinkId,
                                        endLink.infrastructureLinkId,
                                        viaNodeIdSeqAfterNodesOfTerminusLinksRemoved,
                                        nodeIdSequenceCombos)
    }

    private fun createWithoutViaNodes(startLink: SnappedLinkState, endLink: SnappedLinkState)
        : NodeSequenceAlternatives {

        val startLinkId: Long = startLink.infrastructureLinkId
        val endLinkId: Long = endLink.infrastructureLinkId

        if (startLinkId == endLinkId) {
            return NodeSequenceAlternatives(startLinkId, endLinkId, emptyList(), listOf(startLink.toNodeIdList()))
        }

        val startLinkNodeIdSequences: List<List<Long>> = startLink.getNodeIdSequenceCombinations()
        val endLinkNodeIdSequences: List<List<Long>> = endLink.getNodeIdSequenceCombinations()

        val nodeIdSequenceCombos: List<List<Long>> = startLinkNodeIdSequences
            .flatMap { startLinkNodeIdSeq ->
                endLinkNodeIdSequences.map { endLinkNodeIdSeq ->
                    startLinkNodeIdSeq + endLinkNodeIdSeq
                }
            }

        val filteredNodeIdSequences: List<List<Long>> = when (startLink.hasSharedNode(endLink)) {
            true -> nodeIdSequenceCombos.map { filterOutConsecutiveDuplicates(it) }
            false -> nodeIdSequenceCombos
        }

        return NodeSequenceAlternatives(startLinkId, endLinkId, emptyList(), filteredNodeIdSequences)
    }

    private fun resolveNodeIdSequencesOnStartLink(startLink: SnappedLinkState,
                                                  followingNodeIdsBelongingToStartLink: List<Long>): List<List<Long>> {

        val isStartLinkTraverseDirectionUndefined =
            followingNodeIdsBelongingToStartLink.isEmpty()
                || followingNodeIdsBelongingToStartLink.all { it == startLink.closerNodeId }

        return when (isStartLinkTraverseDirectionUndefined) {
            true -> startLink.getNodeIdSequenceCombinations()
            false -> listOf(
                filterOutConsecutiveDuplicates(
                    listOf(startLink.closerNodeId) + followingNodeIdsBelongingToStartLink))
        }
    }

    private fun resolveNodeIdSequencesOnEndLink(endLink: SnappedLinkState,
                                                precedingNodeIdsBelongingToEndLink: List<Long>): List<List<Long>> {

        val isEndLinkTraverseDirectionUndefined =
            precedingNodeIdsBelongingToEndLink.isEmpty()
                || precedingNodeIdsBelongingToEndLink.all { it == endLink.closerNodeId }

        return when (isEndLinkTraverseDirectionUndefined) {
            true -> endLink.getNodeIdSequenceCombinations()
            false -> listOf(
                filterOutConsecutiveDuplicates(
                    precedingNodeIdsBelongingToEndLink + endLink.closerNodeId))
        }
    }
}
