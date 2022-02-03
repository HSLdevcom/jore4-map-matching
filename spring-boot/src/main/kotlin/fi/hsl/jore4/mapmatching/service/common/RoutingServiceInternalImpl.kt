package fi.hsl.jore4.mapmatching.service.common

import fi.hsl.jore4.mapmatching.model.InfrastructureLinkTraversal
import fi.hsl.jore4.mapmatching.model.NodeIdSequence
import fi.hsl.jore4.mapmatching.model.VehicleType
import fi.hsl.jore4.mapmatching.repository.routing.BufferAreaRestriction
import fi.hsl.jore4.mapmatching.repository.routing.IRoutingRepository
import fi.hsl.jore4.mapmatching.repository.routing.RouteLinkDTO
import fi.hsl.jore4.mapmatching.util.LogUtils.joinToLogString
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional

@Component
class RoutingServiceInternalImpl @Autowired constructor(val routingRepository: IRoutingRepository)
    : IRoutingServiceInternal {

    @Transactional(readOnly = true)
    override fun findRoute(nodeIdSequence: NodeIdSequence,
                           vehicleType: VehicleType,
                           bufferAreaRestriction: BufferAreaRestriction?)
        : List<InfrastructureLinkTraversal> {

        val routeLinks: List<RouteLinkDTO> =
            routingRepository.findRouteViaNetworkNodes(nodeIdSequence, vehicleType, bufferAreaRestriction)

        if (LOGGER.isDebugEnabled) {
            LOGGER.debug("Got route links for $nodeIdSequence: {}", joinToLogString(routeLinks))
        }

        return routeLinks.map(RouteLinkDTO::linkTraversal)
    }

    companion object {
        private val LOGGER: Logger = LoggerFactory.getLogger(RoutingServiceInternalImpl::class.java)
    }
}
