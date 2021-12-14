package fi.hsl.jore4.mapmatching.service.node

import fi.hsl.jore4.mapmatching.model.HasInfrastructureNodeId
import fi.hsl.jore4.mapmatching.repository.infrastructure.SnappedLinkState

data class NodeResolutionParams(val startLink: SnappedLinkState,
                                val viaNodeResolvers: List<HasInfrastructureNodeId>,
                                val endLink: SnappedLinkState)
