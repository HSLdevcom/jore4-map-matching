package fi.hsl.jore4.mapmatching.repository.infrastructure

import fi.hsl.jore4.mapmatching.model.HasInfrastructureNodeId
import fi.hsl.jore4.mapmatching.model.InfrastructureLinkId
import fi.hsl.jore4.mapmatching.model.InfrastructureNodeId
import fi.hsl.jore4.mapmatching.model.TrafficFlowDirectionType
import fi.hsl.jore4.mapmatching.util.MathUtils.isWithinTolerance

/**
 * Contains information about a snap from an arbitrary point to an
 * infrastructure link that is deemed closest to the given point.
 *
 * @property infrastructureLinkId the identifier of the infrastructure link to
 * which the point is snapped
 * @property closestDistance the closest distance from the point being snapped
 * to the infrastructure link
 * @property closestPointFractionalMeasure a [Double] between 0.0 and 1.0
 * representing the location of the closest point on the infrastructure link to
 * the source point, as a fraction of the link's 2D length
 * @property trafficFlowDirectionType the direction of traffic flow on
 * infrastructure link
 * @property infrastructureLinkLength the 2D length of the infrastructure link
 * @property startNodeId the identifier of the infrastructure node at start
 * point of the infrastructure link
 * @property endNodeId the identifier of the infrastructure node at end point of
 * the infrastructure link
 */
@Suppress("MemberVisibilityCanBePrivate")
data class SnappedLinkState(val infrastructureLinkId: InfrastructureLinkId,
                            val closestDistance: Double,
                            val closestPointFractionalMeasure: Double,
                            val trafficFlowDirectionType: TrafficFlowDirectionType,
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

    /**
     * @property closerNodeId is the ID of the node that is closer to the snapped projected point on link.
     */
    val closerNodeId: InfrastructureNodeId
        get() = if (closestPointFractionalMeasure <= 0.5) startNodeId else endNodeId

    /**
     * @property furtherNodeId is the ID of the node that lies further away from the snapped projected point on link.
     */
    val furtherNodeId: InfrastructureNodeId get() = if (startNodeId == closerNodeId) endNodeId else startNodeId

    val isSnappedToStartNode: Boolean by lazy {
        closestPointFractionalMeasure.isWithinTolerance(0.0)
    }

    val isSnappedToEndNode: Boolean by lazy {
        closestPointFractionalMeasure.isWithinTolerance(1.0)
    }

    override fun getInfrastructureNodeId() = closerNodeId

    fun findSnappedNode(): InfrastructureNodeId? {
        return if (isSnappedToStartNode)
            startNodeId
        else if (isSnappedToEndNode)
            endNodeId
        else
            null
    }

    fun hasNode(nodeId: InfrastructureNodeId) = startNodeId == nodeId || endNodeId == nodeId

    fun hasSharedNode(that: SnappedLinkState) = hasNode(that.startNodeId) || hasNode(that.endNodeId)

    fun hasDiscreteNodes(): Boolean = startNodeId != endNodeId

    fun isOnSameLinkAs(other: SnappedLinkState): Boolean = infrastructureLinkId == other.infrastructureLinkId

    fun withSnappedToTerminusNode(thresholdDistanceOfSnappingToLinkEndpointInMeters: Double): SnappedLinkState {
        val distanceFromStartOfLink: Double = closestPointFractionalMeasure * infrastructureLinkLength

        val closestPointFractionalMeasurePossiblySnappedToEndpoint: Double =
            if (distanceFromStartOfLink.isWithinTolerance(0.0, thresholdDistanceOfSnappingToLinkEndpointInMeters))
                0.0
            else if (distanceFromStartOfLink.isWithinTolerance(infrastructureLinkLength,
                                                               thresholdDistanceOfSnappingToLinkEndpointInMeters)
            )
                1.0
            else
                closestPointFractionalMeasure

        return SnappedLinkState(infrastructureLinkId,
                                closestDistance,
                                closestPointFractionalMeasurePossiblySnappedToEndpoint,
                                trafficFlowDirectionType,
                                infrastructureLinkLength,
                                startNodeId,
                                endNodeId)
    }
}
