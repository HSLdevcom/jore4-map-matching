package fi.hsl.jore4.mapmatching.repository.routing

import fi.hsl.jore4.mapmatching.model.InfrastructureLinkId

/**
 * Source point for pgRouting function: pgr_trspViaEdges
 */
data class PgRoutingPoint(val linkId: InfrastructureLinkId, val fractionalLocation: Double)
