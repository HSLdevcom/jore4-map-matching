package fi.hsl.jore4.mapmatching.repository.routing

import fi.hsl.jore4.mapmatching.model.PathTraversal

data class RouteLinkDTO(val routeSeqNum: Int,
                        val routeLegSeqNum: Int,
                        val startNodeId: Long,
                        val path: PathTraversal)
