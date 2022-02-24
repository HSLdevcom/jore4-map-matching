package fi.hsl.jore4.mapmatching.service.node

import fi.hsl.jore4.mapmatching.model.InfrastructureNodeId
import fi.hsl.jore4.mapmatching.model.NodeIdSequence

/**
 * Models alternative sequences of infrastructure network node identifiers
 * between infrastructure links at route start and end points. The variance
 * between sequences stems from whether the start and/or end link need to be
 * traversed forwards or backwards which we cannot know in advance in case of
 * bidirectional links. Therefore, we need to test which way produces the
 * shortest route while preserving both terminus links in the route.
 *
 * @property nodeIdSequences list of infrastructure network node identifier
 * sequences from which the best needs to be resolved. Each sequence starts with
 * endpoint nodes of the start link and ends with the nodes associated with the
 * route's end link. The list contains at most four sequences of node
 * identifiers depending on whether direction of traversal on terminus links can
 * be determined based on the interim nodes.
 */
data class NodeSequenceAlternatives(val nodeIdSequences: List<NodeIdSequence>) {

    init {
        require(nodeIdSequences.isNotEmpty()) { "At least one node ID sequence must be provided" }
        require(nodeIdSequences.size <= 4) {
            "At most four node ID sequence may be provided: ${nodeIdSequences.size}"
        }

        nodeIdSequences.forEach { nodeIdSeq ->
            require(!nodeIdSeq.isEmpty()) { "Empty NodeIdSequence not allowed" }

            require(nodeIdSeq.size > 1 || nodeIdSequences.size == 1) {
                "Only one NodeIdSequence should have been provided when there exists a sequence containing only one node identifier"
            }
        }
    }

    /**
     * Indicates whether a route can be created based on the alternatives. If there is only single infrastructure
     * node present, a route cannot be formed.
     */
    fun isRoutePossible(): Boolean = nodeIdSequences.size > 1 || nodeIdSequences.first().size > 1

    fun prettyPrint(): String {
        val startNodeIds: List<InfrastructureNodeId> = nodeIdSequences.map { it.list.first() }.distinct()
        val endNodeIds: List<InfrastructureNodeId> = nodeIdSequences.map { it.list.last() }.distinct()

        // It is sufficient to grab via node IDs from the first sequence because via node IDs are
        // the same for all sequences since we just picked all the unique start and end node IDs
        // above.
        val viaNodeIds: List<InfrastructureNodeId> = nodeIdSequences[0].list
            .dropWhile { it in startNodeIds }
            .dropLastWhile { it in endNodeIds }

        return """{"startNodeIds": $startNodeIds, "viaNodeIds": $viaNodeIds, "endNodeIds": $endNodeIds}"""
    }
}
