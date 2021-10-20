package fi.hsl.jore4.mapmatching.repository.infrastructure

/**
 * Contains information about a snap from an arbitrary point to an
 * infrastructure link that is deemed closest to the given point.
 *
 * `closestDistance` is the closest distance from the point being snapped to the
 * infrastructure link.
 * `startNodeId` is the ID of the node at the start point of the link.
 * `endNodeId` is the ID of the node at the end point of the link.
 * `distanceToStartNode` is the distance from the point being snapped to the
 * start node of the link.
 * `distanceToEndNode` is the distance from the point being snapped to the
 * end node of the link.
 */
data class SnappedLinkState(val infrastructureLinkId: Long,
                            val closestDistance: Double,
                            val startNodeId: Long,
                            val endNodeId: Long,
                            val distanceToStartNode: Double,
                            val distanceToEndNode: Double) {

    private val isStartNodeCloser: Boolean
        get() = distanceToStartNode < distanceToEndNode

    /**
     * Returns the ID of the node that is closer to the point being snapped.
     */
    val closerNodeId: Long
        get() = if (isStartNodeCloser) startNodeId else endNodeId

    /**
     * Returns the ID of the node that lies further away from the point being
     * snapped.
     */
    val furtherNodeId: Long
        get() = if (isStartNodeCloser) endNodeId else startNodeId
}