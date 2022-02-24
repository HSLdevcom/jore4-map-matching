package fi.hsl.jore4.mapmatching.service.node

import fi.hsl.jore4.mapmatching.model.InfrastructureNodeId
import fi.hsl.jore4.mapmatching.repository.infrastructure.SnappedLinkState

data class VisitedNodesResolverParams(val startLink: SnappedLinkState,
                                      val viaNodeIds: List<InfrastructureNodeId>,
                                      val endLink: SnappedLinkState) {

    fun withViaNodeIds(viaNodeIds: List<InfrastructureNodeId>) = VisitedNodesResolverParams(startLink,
                                                                                            viaNodeIds,
                                                                                            endLink)
}
