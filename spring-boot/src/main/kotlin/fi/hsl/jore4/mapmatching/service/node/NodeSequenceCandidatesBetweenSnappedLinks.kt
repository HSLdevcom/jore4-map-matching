package fi.hsl.jore4.mapmatching.service.node

import fi.hsl.jore4.mapmatching.model.NodeIdSequence
import fi.hsl.jore4.mapmatching.repository.infrastructure.SnappedPointOnLink
import kotlin.math.max
import kotlin.math.min

/**
 * Models candidate sequences of infrastructure network nodes related to a pair
 * of infrastructure links that are selected as candidate terminus links for a
 * source route to be map-matched. The links are snapped from source route's
 * start and end points.
 *
 * While map-matching a source route, multiple
 * [NodeSequenceCandidatesBetweenSnappedLinks] objects are resolved based on the
 * geometry and route points of source route. Of these candidates the most
 * suitable needs to be selected to constitute a route through infrastructure
 * network.
 *
 * Variance between node sequences stems from whether the terminus links need to
 * be traversed forwards or backwards which cannot be predicted in advance in
 * case of bidirectional links. Therefore, we need to test which way produces
 * the shortest route while preserving both terminus links as part of the route.
 *
 * @property pointOnStartLink holds data about route start point snapped to
 * infrastructure link
 * @property pointOnEndLink holds data about route end point snapped to
 * infrastructure link
 * @property nodeIdSequences list of infrastructure network node identifier
 * sequences. Each sequence starts with one or both endpoint nodes of the start
 * link and ends with one or both nodes associated with the route's end link.
 * The list may contain at most four sequences of node identifiers depending on
 * whether the directions of traversal on terminus links can be determined by
 * interim nodes.
 */
data class NodeSequenceCandidatesBetweenSnappedLinks(val pointOnStartLink: SnappedPointOnLink,
                                                     val pointOnEndLink: SnappedPointOnLink,
                                                     val nodeIdSequences: List<NodeIdSequence>)
    : Comparable<NodeSequenceCandidatesBetweenSnappedLinks> {

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
     * Indicates whether a route can be created based on the alternatives. If
     * there is only single infrastructure node present, a route cannot be
     * formed.
     */
    fun isRoutePossible(): Boolean = nodeIdSequences.size > 1 || nodeIdSequences.first().size > 1

    private fun getDistanceToCloserTerminusLink(): Double = min(pointOnStartLink.closestDistance, pointOnEndLink.closestDistance)

    private fun getDistanceToFurtherTerminusLink(): Double = max(pointOnStartLink.closestDistance, pointOnEndLink.closestDistance)

    // sorting node sequence candidates by closest distances to terminus links
    override fun compareTo(other: NodeSequenceCandidatesBetweenSnappedLinks): Int {
        val closestDistanceToTerminusLink1: Double = getDistanceToCloserTerminusLink()
        val closestDistanceToTerminusLink2: Double = other.getDistanceToCloserTerminusLink()

        if (closestDistanceToTerminusLink1 < closestDistanceToTerminusLink2)
            return -1
        else if (closestDistanceToTerminusLink1 > closestDistanceToTerminusLink2)
            return 1

        return getDistanceToFurtherTerminusLink().compareTo(other.getDistanceToFurtherTerminusLink())
    }
}
