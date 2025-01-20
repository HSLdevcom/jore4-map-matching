package fi.hsl.jore4.mapmatching.repository.routing

import fi.hsl.jore4.mapmatching.model.InfrastructureLinkId
import fi.hsl.jore4.mapmatching.model.InfrastructureNodeId
import fi.hsl.jore4.mapmatching.model.LinkSide
import fi.hsl.jore4.mapmatching.repository.infrastructure.SnappedPointOnLink

/**
 * Source point for pgRouting functions: pgr_withPointsVia, pgr_trspVia_withPoints
 */
sealed interface PgRoutingPoint {
    companion object {
        fun fromSnappedPointOnLink(
            pointOnLink: SnappedPointOnLink,
            linkSideIfVirtualNode: LinkSide = LinkSide.BOTH
        ): PgRoutingPoint {
            return if (pointOnLink.isSnappedToStartNode) {
                RealNode(pointOnLink.startNodeId)
            } else if (pointOnLink.isSnappedToEndNode) {
                RealNode(pointOnLink.endNodeId)
            } else {
                VirtualNode(
                    pointOnLink.infrastructureLinkId,
                    pointOnLink.closestPointFractionalMeasure,
                    linkSideIfVirtualNode,
                    pointOnLink.closerNodeId
                )
            }
        }
    }
}

/**
 * A real infrastructure node as routing point.
 */
data class RealNode(val nodeId: InfrastructureNodeId) : PgRoutingPoint

/**
 * A point along infrastructure link as virtual node.
 */
data class VirtualNode(
    val linkId: InfrastructureLinkId,
    val fractionalLocation: Double,
    val side: LinkSide,
    val closerRealNodeId: InfrastructureNodeId
) : PgRoutingPoint
