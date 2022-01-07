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

        val viaNodeIdsAfterNodesOfTerminusLinksRemoved: List<Long> =
            viaNodeIdsAfterStartLinkNodesRemoved.dropLast(viaNodeIdsBelongingToEndLink.size)

        val mergedNodeIdsOnStartLink: List<Long> = mergeNodeIdsOnStartLink(startLink, viaNodeIdsBelongingToStartLink)
        val mergedNodeIdsOnEndLink: List<Long> = mergeNodeIdsOnEndLink(endLink, viaNodeIdsBelongingToEndLink)

        val mergedNodeIdsOnStartAndEndLink: List<Long> =
            filterOutConsecutiveDuplicates(mergedNodeIdsOnStartLink + mergedNodeIdsOnEndLink)

        // not overlapping with closer nodes of terminus links
        val nonOverlappingViaNodeIdSequence: List<Long>

        if (viaNodeIdsAfterNodesOfTerminusLinksRemoved.isEmpty() && mergedNodeIdsOnStartAndEndLink.size <= 2) {
            // There are no via nodes left after "cleaning" data.
            nonOverlappingViaNodeIdSequence = emptyList()

            // This very special corner case needs to be treated separately in order to have a well-behaving algorithm.
            if (startLink.infrastructureLinkId == endLink.infrastructureLinkId) {
                return createResponseForSingleLinkNoViaNodes(startLink, endLink.infrastructureLinkId)
            }
        } else {
            nonOverlappingViaNodeIdSequence = mergedNodeIdsOnStartLink.drop(1)
                .plus(viaNodeIdsAfterNodesOfTerminusLinksRemoved)
                .plus(mergedNodeIdsOnEndLink.dropLast(1))
        }

        val nodeIdSequencesOnStartLink: List<List<Long>> = getNodeIdSequencesOnStartLink(startLink,
                                                                                         mergedNodeIdsOnStartLink)

        val nodeIdSequencesOnEndLink: List<List<Long>> = getNodeIdSequencesOnEndLink(endLink,
                                                                                     mergedNodeIdsOnEndLink)

        val nodeIdSequenceCombos: List<List<Long>> = nodeIdSequencesOnStartLink
            .flatMap { startNodeIds ->
                nodeIdSequencesOnEndLink.map { endNodeIds ->
                    startNodeIds + viaNodeIdsAfterNodesOfTerminusLinksRemoved + endNodeIds
                }
            }

        return NodeSequenceAlternatives(startLink.infrastructureLinkId,
                                        endLink.infrastructureLinkId,
                                        nonOverlappingViaNodeIdSequence,
                                        nodeIdSequenceCombos)
    }

    private fun createWithoutViaNodes(startLink: SnappedLinkState, endLink: SnappedLinkState)
        : NodeSequenceAlternatives {

        val startLinkId: Long = startLink.infrastructureLinkId
        val endLinkId: Long = endLink.infrastructureLinkId

        if (startLinkId == endLinkId) {
            return createResponseForSingleLinkNoViaNodes(startLink, endLinkId)
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

    private fun mergeNodeIdsOnStartLink(startLink: SnappedLinkState,
                                        followingViaNodeIdsBelongingToStartLink: List<Long>): List<Long> {

        return if (followingViaNodeIdsBelongingToStartLink.isEmpty())
            listOf(startLink.closerNodeId)
        else if (startLink.closerNodeId == followingViaNodeIdsBelongingToStartLink.first())
            followingViaNodeIdsBelongingToStartLink
        else
            listOf(startLink.closerNodeId) + followingViaNodeIdsBelongingToStartLink
    }

    private fun mergeNodeIdsOnEndLink(endLink: SnappedLinkState,
                                      precedingViaNodeIdsBelongingToEndLink: List<Long>): List<Long> {

        return if (precedingViaNodeIdsBelongingToEndLink.isEmpty())
            listOf(endLink.closerNodeId)
        else if (precedingViaNodeIdsBelongingToEndLink.last() == endLink.closerNodeId)
            precedingViaNodeIdsBelongingToEndLink
        else
            precedingViaNodeIdsBelongingToEndLink + endLink.closerNodeId
    }

    private fun getNodeIdSequencesOnStartLink(startLink: SnappedLinkState, nodeIdsOnStartLink: List<Long>)
        : List<List<Long>> {

        return when (nodeIdsOnStartLink.size) {
            0 -> throw IllegalStateException("Cannot have zero node snaps on start link")
            1 -> startLink.getNodeIdSequenceCombinations()
            else -> listOf(nodeIdsOnStartLink)
        }
    }

    private fun getNodeIdSequencesOnEndLink(endLink: SnappedLinkState, nodeIdsOnEndLink: List<Long>)
        : List<List<Long>> {

        return when (nodeIdsOnEndLink.size) {
            0 -> throw IllegalStateException("Cannot have zero node snaps on end link")
            1 -> endLink.getNodeIdSequenceCombinations()
            else -> listOf(nodeIdsOnEndLink)
        }
    }

    private fun createResponseForSingleLinkNoViaNodes(startLink: SnappedLinkState,
                                                      endLinkId: Long): NodeSequenceAlternatives {

        return NodeSequenceAlternatives(startLink.infrastructureLinkId,
                                        endLinkId,
                                        emptyList(),
                                        listOf(startLink.toNodeIdList()))

    }
}
