package fi.hsl.jore4.mapmatching.repository.routing

import fi.hsl.jore4.mapmatching.model.InfrastructureLinkTraversal

data class RouteLinkDTO(val routeSeqNum: Int,
                        val linkTraversal: InfrastructureLinkTraversal)
