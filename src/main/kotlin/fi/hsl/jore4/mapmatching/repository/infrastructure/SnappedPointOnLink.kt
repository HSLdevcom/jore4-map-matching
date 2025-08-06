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
 * an infrastructure link
 * @property infrastructureLinkLength the 2D length of the infrastructure link
 * @property startNodeId the identifier of the infrastructure node at the start
 * point of the infrastructure link
 * @property endNodeId the identifier of the infrastructure node at the end
 * point of the infrastructure link
 */
@Suppress("MemberVisibilityCanBePrivate")
data class SnappedPointOnLink(
    val infrastructureLinkId: InfrastructureLinkId,
    val closestDistance: Double,
    val closestPointFractionalMeasure: Double,
    val trafficFlowDirectionType: TrafficFlowDirectionType,
    val infrastructureLinkLength: Double,
    val startNodeId: InfrastructureNodeId,
    val endNodeId: InfrastructureNodeId
) : HasInfrastructureNodeId {
    init {
        // check validity

        require(closestDistance >= 0.0) {
            "closestDistance must be greater than or equal to 0.0: $closestDistance"
        }
        require(closestPointFractionalMeasure in 0.0..1.0) {
            "closestPointFractionalMeasure must be in range 0.0..1.0: $closestPointFractionalMeasure"
        }
        require(infrastructureLinkLength > 0.0) {
            "infrastructureLinkLength must be greater than 0.0: $infrastructureLinkLength"
        }
    }

    /**
     * @property closerNodeId is the ID of the node that is closer to the
     * snapped projected point on a link.
     */
    val closerNodeId: InfrastructureNodeId
        get() = if (isStartNodeCloser()) startNodeId else endNodeId

    /**
     * @property furtherNodeId is the ID of the node that lies further away from
     * the snapped projected point on a link.
     */
    val furtherNodeId: InfrastructureNodeId get() = if (isStartNodeCloser()) endNodeId else startNodeId

    val isSnappedToStartNode: Boolean by lazy {
        closestPointFractionalMeasure.isWithinTolerance(0.0)
    }

    val isSnappedToEndNode: Boolean by lazy {
        closestPointFractionalMeasure.isWithinTolerance(1.0)
    }

    override fun getInfrastructureNodeId() = closerNodeId

    fun isStartNodeCloser(): Boolean = closestPointFractionalMeasure <= 0.5

    fun getSnappedNodeOrNull(): InfrastructureNodeId? =
        if (isSnappedToStartNode) {
            startNodeId
        } else if (isSnappedToEndNode) {
            endNodeId
        } else {
            null
        }

    fun isOnLinkTerminatedByNode(nodeId: InfrastructureNodeId) = startNodeId == nodeId || endNodeId == nodeId

    fun isOnLinkWithDiscreteNodes(): Boolean = startNodeId != endNodeId

    fun isOnSameLinkAs(other: SnappedPointOnLink): Boolean = infrastructureLinkId == other.infrastructureLinkId

    /**
     * Returns the distance to start of infrastructure link in meters.
     */
    fun getDistanceToStartOfLink(): Double = closestPointFractionalMeasure * infrastructureLinkLength

    fun withSnappedToTerminusNode(thresholdDistanceOfSnappingToLinkEndpointInMeters: Double): SnappedPointOnLink {
        val distanceToStartOfLink: Double = getDistanceToStartOfLink()

        return if (distanceToStartOfLink.isWithinTolerance(
                0.0,
                thresholdDistanceOfSnappingToLinkEndpointInMeters
            )
        ) {
            withClosestPointFractionalMeasure(0.0)
        } else if (distanceToStartOfLink.isWithinTolerance(
                infrastructureLinkLength,
                thresholdDistanceOfSnappingToLinkEndpointInMeters
            )
        ) {
            withClosestPointFractionalMeasure(1.0)
        } else {
            this
        }
    }

    /**
     * This method is used in map-matching public transport routes. If a snap
     * point lies at either endpoint of a link, then the link may not be
     * included in the route response depending on the direction of travel from
     * the snapped node. To make sure that a link associated with a terminus
     * stop point is included in a map-matched route, snap point needs to be
     * moved a bit inwards.
     */
    fun moveSnapPointInwardsIfLocatedAtEndpoint(inwardsOffsetInMeters: Double): SnappedPointOnLink {
        require(inwardsOffsetInMeters < infrastructureLinkLength) {
            "inwardsOffsetInMeters must be less than length of infrastructure link ($infrastructureLinkLength): $inwardsOffsetInMeters"
        }

        val overriddenClosestPointFractionalMeasure: Double? =
            if (isSnappedToStartNode) {
                inwardsOffsetInMeters / infrastructureLinkLength
            } else if (isSnappedToEndNode) {
                1.0 - (inwardsOffsetInMeters / infrastructureLinkLength)
            } else {
                null
            }

        return overriddenClosestPointFractionalMeasure
            ?.let(this::withClosestPointFractionalMeasure)
            ?: this
    }

    private fun withClosestPointFractionalMeasure(newClosestPointFractionalMeasure: Double) =
        SnappedPointOnLink(
            infrastructureLinkId,
            closestDistance,
            newClosestPointFractionalMeasure,
            trafficFlowDirectionType,
            infrastructureLinkLength,
            startNodeId,
            endNodeId
        )
}
