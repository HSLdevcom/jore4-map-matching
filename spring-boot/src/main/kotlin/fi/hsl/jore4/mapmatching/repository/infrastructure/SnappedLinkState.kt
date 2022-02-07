package fi.hsl.jore4.mapmatching.repository.infrastructure

import fi.hsl.jore4.mapmatching.model.HasInfrastructureNodeId
import fi.hsl.jore4.mapmatching.model.InfrastructureLinkId
import fi.hsl.jore4.mapmatching.model.InfrastructureNodeId

/**
 * Contains information about a snap from an arbitrary point to an
 * infrastructure link that is deemed closest to the given point.
 *
 * @property infrastructureLinkId the identifier of the infrastructure link to
 * which the point is snapped
 * @property closestDistance the closest distance from the point being snapped
 * to the infrastructure link
 * @property closestPointFractionalMeasure a [Double] between 0 and 1
 * representing the location of the closest point on the infrastructure link to
 * the source point, as a fraction of the link's 2D length
 * @property infrastructureLinkLength the 2D length of the infrastructure link
 * @property startNodeId the identifier of the infrastructure node at start
 * point of the infrastructure link
 * @property endNodeId the identifier of the infrastructure node at end point of
 * the infrastructure link
 */
data class SnappedLinkState(val infrastructureLinkId: InfrastructureLinkId,
                            val closestDistance: Double,
                            val closestPointFractionalMeasure: Double,
                            val infrastructureLinkLength: Double,
                            val startNodeId: InfrastructureNodeId,
                            val endNodeId: InfrastructureNodeId)
    : HasInfrastructureNodeId {

    init {
        // check validity

        require(closestDistance >= 0.0) {
            "closestDistance must be greater than or equal to 0.0: $closestDistance"
        }
        require(closestPointFractionalMeasure in 0.0..1.0) {
            "closestPointFractionalMeasure must be in range 0.0..1.0: $closestPointFractionalMeasure"
        }
        require(infrastructureLinkLength >= 0.0) {
            "infrastructureLinkLength must be greater than or equal to 0.0: $infrastructureLinkLength"
        }
    }

    private val isEndNodeCloser: Boolean get() = closestPointFractionalMeasure > 0.5

    /**
     * The ID of the node that is closer to the point being snapped.
     */
    val closerNodeId: InfrastructureNodeId get() = if (!isEndNodeCloser) startNodeId else endNodeId

    /**
     * The ID of the node that lies further away from the point being snapped.
     */
    val furtherNodeId: InfrastructureNodeId get() = if (!isEndNodeCloser) endNodeId else startNodeId

    override fun getInfrastructureNodeId() = closerNodeId

    fun hasNode(nodeId: InfrastructureNodeId) = startNodeId == nodeId || endNodeId == nodeId

    fun hasSharedNode(that: SnappedLinkState) = hasNode(that.startNodeId) || hasNode(that.endNodeId)

    fun hasDiscreteNodes(): Boolean = startNodeId != endNodeId

    fun isOnSameLinkAs(other: SnappedLinkState): Boolean = infrastructureLinkId == other.infrastructureLinkId
}
