package fi.hsl.jore4.mapmatching.service.common

import fi.hsl.jore4.mapmatching.model.InfrastructureNodeId
import fi.hsl.jore4.mapmatching.model.NodeIdSequence
import fi.hsl.jore4.mapmatching.model.VehicleType
import fi.hsl.jore4.mapmatching.repository.routing.BufferAreaRestriction
import fi.hsl.jore4.mapmatching.repository.routing.IRoutingRepository
import fi.hsl.jore4.mapmatching.repository.routing.PgRoutingPoint
import fi.hsl.jore4.mapmatching.repository.routing.RealNode
import fi.hsl.jore4.mapmatching.repository.routing.RouteLink
import fi.hsl.jore4.mapmatching.util.InternalService
import org.springframework.transaction.annotation.Transactional

@InternalService
class RoutingServiceInternalImpl(
    val routingRepository: IRoutingRepository
) : IRoutingServiceInternal {
    @Transactional(readOnly = true, noRollbackFor = [RuntimeException::class])
    override fun findRouteViaNodes(
        nodeIdSequence: NodeIdSequence,
        vehicleType: VehicleType,
        fractionalStartLocationOnFirstLink: Double,
        fractionalEndLocationOnLastLink: Double,
        bufferAreaRestriction: BufferAreaRestriction?
    ): List<RouteLink> =
        routingRepository.findRouteViaNetworkNodes(
            nodeIdSequence,
            vehicleType,
            fractionalStartLocationOnFirstLink,
            fractionalEndLocationOnLastLink,
            bufferAreaRestriction
        )

    @Transactional(readOnly = true, noRollbackFor = [RuntimeException::class])
    override fun findRouteViaPointsOnLinks(
        points: List<PgRoutingPoint>,
        vehicleType: VehicleType,
        simplifyConsecutiveClosedLoopTraversals: Boolean,
        bufferAreaRestriction: BufferAreaRestriction?
    ): List<RouteLink> {
        return when (points.all { it is RealNode }) {
            true -> {
                val nodeIdList: List<InfrastructureNodeId> =
                    points.mapNotNull { if (it is RealNode) it.nodeId else null }

                val nodeIdSequence = NodeIdSequence(nodeIdList).duplicatesRemoved()

                // Closed-loop post-processing is not relevant when find route via network nodes.

                routingRepository.findRouteViaNetworkNodes(nodeIdSequence, vehicleType, bufferAreaRestriction)
            }

            false -> {
                val routeLinks: List<RouteLink> =
                    routingRepository.findRouteViaPointsOnLinks(points, vehicleType, bufferAreaRestriction)

                return if (simplifyConsecutiveClosedLoopTraversals) {
                    ClosedLoopPostProcessor.simplifyConsecutiveClosedLoopTraversals(routeLinks)
                } else {
                    routeLinks
                }
            }
        }
    }
}
