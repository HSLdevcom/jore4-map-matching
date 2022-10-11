package fi.hsl.jore4.mapmatching.repository.routing

import fi.hsl.jore4.mapmatching.model.InfrastructureLinkId
import fi.hsl.jore4.mapmatching.model.InfrastructureNodeId
import fi.hsl.jore4.mapmatching.model.LinkSide
import fi.hsl.jore4.mapmatching.repository.infrastructure.SnappedLinkState

/**
 * Source point for pgRouting functions: pgr_withPointsVia, pgr_trspVia_withPoints
 */
sealed interface PgRoutingPoint {

    companion object {

        fun fromSnappedPointOnLink(pointOnLink: SnappedLinkState): PgRoutingPoint {
            return if (pointOnLink.isSnappedToStartNode)
                NetworkNode(pointOnLink.startNodeId)
            else if (pointOnLink.isSnappedToEndNode)
                NetworkNode(pointOnLink.endNodeId)
            else
                FractionalLocationAlongLink(pointOnLink.infrastructureLinkId,
                                            pointOnLink.closestPointFractionalMeasure,
                                            LinkSide.BOTH,
                                            pointOnLink.closerNodeId)
        }
    }
}

data class NetworkNode(val nodeId: InfrastructureNodeId) : PgRoutingPoint

data class FractionalLocationAlongLink(val linkId: InfrastructureLinkId,
                                       val fractionalLocation: Double,
                                       val side: LinkSide,
                                       val closerNodeId: InfrastructureNodeId) : PgRoutingPoint
