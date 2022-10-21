package fi.hsl.jore4.mapmatching.service.common

import fi.hsl.jore4.mapmatching.model.InfrastructureNodeId
import fi.hsl.jore4.mapmatching.model.NodeIdSequence
import fi.hsl.jore4.mapmatching.model.VehicleType
import fi.hsl.jore4.mapmatching.repository.routing.BufferAreaRestriction
import fi.hsl.jore4.mapmatching.repository.routing.IRoutingRepository
import fi.hsl.jore4.mapmatching.repository.routing.PgRoutingPoint
import fi.hsl.jore4.mapmatching.repository.routing.RealNode
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
    override fun findRouteViaNodes(nodeIdSequence: NodeIdSequence,
                                   vehicleType: VehicleType,
                                   fractionalStartLocationOnFirstLink: Double,
                                   fractionalEndLocationOnLastLink: Double,
                                   bufferAreaRestriction: BufferAreaRestriction?)
        : List<RouteLinkDTO> {

        return routingRepository.findRouteViaNetworkNodes(nodeIdSequence,
                                                          vehicleType,
                                                          fractionalStartLocationOnFirstLink,
                                                          fractionalEndLocationOnLastLink,
                                                          bufferAreaRestriction)
            .also { routeLinks: List<RouteLinkDTO> ->
                LOGGER.debug { "Got route links for nodes $nodeIdSequence: ${joinToLogString(routeLinks)}" }
            }
    }

    @Transactional(readOnly = true)
    override fun findRouteViaPointsOnLinks(points: List<PgRoutingPoint>,
                                           vehicleType: VehicleType,
                                           simplifyConsecutiveClosedLoopTraversals: Boolean,
                                           bufferAreaRestriction: BufferAreaRestriction?)
        : List<RouteLinkDTO> {

        return when (points.all { it is RealNode }) {

            true -> {
                val nodeIdList: List<InfrastructureNodeId> =
                    points.mapNotNull { if (it is RealNode) it.nodeId else null }

                val nodeIdSequence = NodeIdSequence(nodeIdList).duplicatesRemoved()

                // Closed-loop post-processing is not relevant when find route via network nodes.

                routingRepository
                    .findRouteViaNetworkNodes(nodeIdSequence, vehicleType, bufferAreaRestriction)
                    .also { routeLinks: List<RouteLinkDTO> ->
                        LOGGER.debug {
                            "Got route links for nodes $nodeIdSequence: ${joinToLogString(routeLinks)}"
                        }
                    }
            }

            false -> {
                val routeLinks: List<RouteLinkDTO> =
                    routingRepository.findRouteViaPointsOnLinks(points, vehicleType, bufferAreaRestriction)

                val resultsRouteLinks: List<RouteLinkDTO> = if (simplifyConsecutiveClosedLoopTraversals)
                    ClosedLoopPostProcessor.simplifyConsecutiveClosedLoopTraversals(routeLinks)
                else
                    routeLinks

                return resultsRouteLinks
                    .also { routeLinks: List<RouteLinkDTO> ->
                        LOGGER.debug { "Got route links: ${joinToLogString(routeLinks)}" }
                    }
            }
        }
    }
}
