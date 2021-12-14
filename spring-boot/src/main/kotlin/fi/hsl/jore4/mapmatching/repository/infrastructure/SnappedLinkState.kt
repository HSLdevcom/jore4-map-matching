package fi.hsl.jore4.mapmatching.repository.infrastructure

import fi.hsl.jore4.mapmatching.model.HasInfrastructureNodeId
import fi.hsl.jore4.mapmatching.model.NodeProximity
import fi.hsl.jore4.mapmatching.util.MathUtils
import java.lang.IllegalArgumentException

/**
 * Contains information about a snap from an arbitrary point to an
 * infrastructure link that is deemed closest to the given point.
 *
 * @property infrastructureLinkId the identifier of the infrastructure link to
 * which the point is snapped
 * @property closestDistance the closest distance from the point being snapped
 * to the infrastructure link
 * @property startNode the node at the start point of the infrastructure link
 * @property endNode the node at the end point of the infrastructure link
 */
@Suppress("unused", "MemberVisibilityCanBePrivate")
data class SnappedLinkState(val infrastructureLinkId: Long,
                            val closestDistance: Double,
                            val startNode: NodeProximity,
                            val endNode: NodeProximity)
    : HasInfrastructureNodeId {

    init {
        // check validity

        if (startNode.id == endNode.id) {
            throw IllegalArgumentException(
                "infrastructureLink=$infrastructureLinkId: endpoint nodes must have different identifiers")
        }

        if (closestDistance < 0.0) {
            throw IllegalArgumentException("closestDistance must be greater than or equal to 0.0: $closestDistance")
        }
        if (closestDistance > startNode.distanceToNode) {
            throw IllegalArgumentException("closestDistance cannot be greater than the distance to start node")
        }
        if (closestDistance > endNode.distanceToNode) {
            throw IllegalArgumentException("closestDistance cannot be greater than the distance to end node")
        }
    }

    val isStartNodeCloser: Boolean
        get() = MathUtils.compare(startNode.distanceToNode, endNode.distanceToNode) < 0

    val isEndNodeCloser: Boolean
        get() = MathUtils.compare(startNode.distanceToNode, endNode.distanceToNode) > 0

    /**
     * Returns the ID of the node that is closer to the point being snapped.
     */
    val closerNodeId: Long
        get() = if (!isEndNodeCloser) startNode.id else endNode.id

    /**
     * Returns the ID of the node that lies further away from the point being
     * snapped.
     */
    val furtherNodeId: Long
        get() = if (!isEndNodeCloser) endNode.id else startNode.id

    override fun getInfrastructureNodeId() = closerNodeId

    fun hasNode(nodeId: Long) = startNode.id == nodeId || endNode.id == nodeId

    fun hasSharedNode(that: SnappedLinkState) = hasNode(that.startNode.id) || hasNode(that.endNode.id)
}
