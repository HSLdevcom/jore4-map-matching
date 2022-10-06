package fi.hsl.jore4.mapmatching.service.node

import fi.hsl.jore4.mapmatching.model.InfrastructureNodeId
import fi.hsl.jore4.mapmatching.repository.infrastructure.SnappedPointOnLink

data class VisitedNodesResolverParams(val pointOnStartLink: SnappedPointOnLink,
                                      val viaNodeIds: List<InfrastructureNodeId>,
                                      val pointOnEndLink: SnappedPointOnLink) {

    fun withViaNodeIds(viaNodeIds: List<InfrastructureNodeId>) = VisitedNodesResolverParams(pointOnStartLink,
                                                                                            viaNodeIds,
                                                                                            pointOnEndLink)
}
