package fi.hsl.jore4.mapmatching.repository.infrastructure

import fi.hsl.jore4.mapmatching.model.HasInfrastructureNodeId
import fi.hsl.jore4.mapmatching.model.InfrastructureLinkId
import fi.hsl.jore4.mapmatching.model.InfrastructureNodeId
import fi.hsl.jore4.mapmatching.model.NodeProximity

/**
 * Contains information about a snap from an arbitrary point to an
 * infrastructure link that is deemed closest to the given point.
 *
 * @property infrastructureLinkId the identifier of the infrastructure link to
 * which the point is snapped
 * @property closestDistance the closest distance from the point being snapped
 * to the infrastructure link
 * @property startNode the node at start point of the infrastructure link
 * @property endNode the node at end point of the infrastructure link
 */
@Suppress("unused", "MemberVisibilityCanBePrivate")
data class SnappedLinkState(val infrastructureLinkId: InfrastructureLinkId,
                            val closestDistance: Double,
                            val startNode: NodeProximity,
                            val endNode: NodeProximity)
    : HasInfrastructureNodeId {

    init {
        // check validity

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
        get() = startNode.distanceToNode < endNode.distanceToNode

    val isEndNodeCloser: Boolean
        get() = startNode.distanceToNode > endNode.distanceToNode

    /**
     * Returns the node that is closer to the point being snapped.
     */
    val closerNode: NodeProximity
        get() = if (!isEndNodeCloser) startNode else endNode

    /**
     * Returns the node that is closer to the point being snapped.
     */
    val furtherNode: NodeProximity
        get() = if (!isEndNodeCloser) endNode else startNode

    /**
     * Returns the ID of the node that is closer to the point being snapped.
     */
    val closerNodeId: InfrastructureNodeId
        get() = closerNode.id

    /**
     * Returns the ID of the node that lies further away from the point being
     * snapped.
     */
    val furtherNodeId: InfrastructureNodeId
        get() = furtherNode.id

    override fun getInfrastructureNodeId() = closerNodeId

    fun hasNode(nodeId: InfrastructureNodeId) = startNode.id == nodeId || endNode.id == nodeId

    fun hasSharedNode(that: SnappedLinkState) = hasNode(that.startNode.id) || hasNode(that.endNode.id)

    fun hasDiscreteNodes(): Boolean = startNode.id != endNode.id

    fun isOnSameLinkAs(other: SnappedLinkState): Boolean = infrastructureLinkId == other.infrastructureLinkId
}
