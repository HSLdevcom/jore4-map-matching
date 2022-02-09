package fi.hsl.jore4.mapmatching.service.common

import fi.hsl.jore4.mapmatching.model.InfrastructureLinkTraversal
import fi.hsl.jore4.mapmatching.model.NodeIdSequence
import fi.hsl.jore4.mapmatching.model.VehicleType
import fi.hsl.jore4.mapmatching.repository.routing.BufferAreaRestriction
import fi.hsl.jore4.mapmatching.repository.routing.IRoutingRepository
import fi.hsl.jore4.mapmatching.repository.routing.RouteLinkDTO
import fi.hsl.jore4.mapmatching.util.LogUtils.joinToLogString
import mu.KotlinLogging
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional

private val LOGGER = KotlinLogging.logger {}

@Component
class RoutingServiceInternalImpl @Autowired constructor(val routingRepository: IRoutingRepository)
    : IRoutingServiceInternal {

    @Transactional(readOnly = true)
    override fun findRoute(nodeIdSequence: NodeIdSequence,
                           vehicleType: VehicleType,
                           bufferAreaRestriction: BufferAreaRestriction?)
        : List<InfrastructureLinkTraversal> {

        return routingRepository
            .findRouteViaNetworkNodes(nodeIdSequence, vehicleType, bufferAreaRestriction)
            .also { routeLinks: List<RouteLinkDTO> ->
                LOGGER.debug { "Got route links for nodes $nodeIdSequence: ${joinToLogString(routeLinks)}" }
            }
            .map(RouteLinkDTO::linkTraversal)
    }
}
