package fi.hsl.jore4.mapmatching.service.node

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
 * sequences from which the best needs to be resolved. Includes both endpoint
 * nodes for the start link and the end link
 */
data class NodeSequenceAlternatives(val startLinkId: Long,
                                    val endLinkId: Long,
                                    val viaNodeIds: List<Long>,
                                    val nodeIdSequences: List<List<Long>>) {

    init {
        if (nodeIdSequences.isEmpty()) {
            throw IllegalArgumentException("At least one node ID sequence needs to be provided")
        }
        if (nodeIdSequences.size > 4) {
            throw IllegalArgumentException("At most four node ID sequence may be provided: ${nodeIdSequences.size}")
        }
    }

    fun toCompactString() = "{startLinkId=$startLinkId, endLinkId=$endLinkId, viaNodeIds=$viaNodeIds}"
}
