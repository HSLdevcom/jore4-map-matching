package fi.hsl.jore4.mapmatching.service.node

import fi.hsl.jore4.mapmatching.model.InfrastructureLinkId
import fi.hsl.jore4.mapmatching.model.NodeIdSequence

/**
 * Models alternative sequences of infrastructure network node identifiers
 * between infrastructure links at route start and end. The variance between
 * sequences stems from whether the start and end link needs to be traversed
 * forwards or backwards which we cannot know in advance. Therefore, we need to
 * test which way produces the shortest route while also preserving both the
 * start link and end link in the route.
 *
 * @property startLinkId the identifier for infrastructure link from which route
 * starts
 * @property endLinkId the identifier for infrastructure link at which route
 * ends
 * @property viaNodeIds the identifiers of interim infrastructure network nodes
 * through which the route must pass. This property is just informational (for
 * possibly debugging purposes) and should not be used in resolving the optimal
 * route. This is a "cleaned" version of what user has originally provided e.g.
 * consecutive duplicates are removed.
 * @property nodeIdSequences list of infrastructure network node identifier
 * sequences from which the best needs to be resolved. Each sequence starts with
 * endpoint nodes of the start link and ends with the nodes associated with the
 * end link. [viaNodeIds] are inserted in between. The list contains at most
 * four sequences of node identifiers depending on whether the direction of
 * traversal on terminus links can be determined based on the interim nodes
 * ([viaNodeIds]).
 */
data class NodeSequenceAlternatives(val startLinkId: InfrastructureLinkId,
                                    val endLinkId: InfrastructureLinkId,
                                    val viaNodeIds: NodeIdSequence,
                                    val nodeIdSequences: List<NodeIdSequence>) {

    init {
        if (nodeIdSequences.isEmpty()) {
            throw IllegalArgumentException("At least one node ID sequence needs to be provided")
        }
        if (nodeIdSequences.size > 4) {
            throw IllegalArgumentException("At most four node ID sequence may be provided: ${nodeIdSequences.size}")
        }

        nodeIdSequences.forEach { nodeIdSeq ->
            if (nodeIdSeq.isEmpty()) {
                throw IllegalArgumentException("Empty NodeIdSequence not allowed")
            }

            if (nodeIdSeq.size == 1 && nodeIdSequences.size > 1) {
                throw IllegalArgumentException(
                    """Only one NodeIdSequence should have been provided when there exists a sequence containing only
                        |one node identifier
                    """.trimMargin())
            }
        }
    }

    /**
     * Indicates whether a route can be created based on the alternatives. If there is only single infrastructure
     * node present, a route cannot be formed.
     */
    fun isRoutePossible(): Boolean = nodeIdSequences.size > 1 || nodeIdSequences.first().size > 1

    fun prettyPrint() = "{startLinkId=$startLinkId, endLinkId=$endLinkId, viaNodeIds=$viaNodeIds}"
}
