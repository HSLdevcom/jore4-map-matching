package fi.hsl.jore4.mapmatching.service.common

import fi.hsl.jore4.mapmatching.model.NodeIdSequence
import fi.hsl.jore4.mapmatching.model.VehicleType
import fi.hsl.jore4.mapmatching.repository.routing.BufferAreaRestriction
import fi.hsl.jore4.mapmatching.repository.routing.IRoutingRepository
import fi.hsl.jore4.mapmatching.repository.routing.PgRoutingPoint
import fi.hsl.jore4.mapmatching.repository.routing.RouteDTO
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
    override fun findRouteViaNodes(nodeIdSequence: NodeIdSequence,
                                   vehicleType: VehicleType,
                                   fractionalStartLocationOnFirstLink: Double,
                                   fractionalEndLocationOnLastLink: Double,
                                   bufferAreaRestriction: BufferAreaRestriction?)
        : RouteDTO {

        return routingRepository.findRouteViaNetworkNodes(nodeIdSequence,
                                                          vehicleType,
                                                          fractionalStartLocationOnFirstLink,
                                                          fractionalEndLocationOnLastLink,
                                                          bufferAreaRestriction)
            .also { route: RouteDTO ->
                LOGGER.debug { "Got route links for nodes $nodeIdSequence: ${joinToLogString(route.routeLinks)}" }
            }
    }

    @Transactional(readOnly = true)
    override fun findRouteViaPoints(points: List<PgRoutingPoint>,
                                    vehicleType: VehicleType,
                                    bufferAreaRestriction: BufferAreaRestriction?)
        : RouteDTO {

        return routingRepository.findRouteViaPoints(points,
                                                    vehicleType,
                                                    bufferAreaRestriction)
            .also { route: RouteDTO ->
                LOGGER.debug { "Got route links: ${joinToLogString(route.routeLinks)}" }
            }
    }
}
