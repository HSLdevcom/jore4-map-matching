package fi.hsl.jore4.mapmatching.repository.routing

import fi.hsl.jore4.mapmatching.model.InfrastructureLinkId
import fi.hsl.jore4.mapmatching.model.InfrastructureNodeId
import fi.hsl.jore4.mapmatching.model.LinkSide

/**
 * Source point for pgRouting functions: pgr_withPointsVia, pgr_trspVia_withPoints
 */
sealed interface PgRoutingPoint

data class NetworkNode(val nodeId: InfrastructureNodeId) : PgRoutingPoint

data class FractionalLocationAlongLink(val linkId: InfrastructureLinkId,
                                       val fractionalLocation: Double,
                                       val side: LinkSide,
                                       val closerNodeId: InfrastructureNodeId) : PgRoutingPoint