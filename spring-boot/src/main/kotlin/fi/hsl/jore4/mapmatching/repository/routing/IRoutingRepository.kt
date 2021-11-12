package fi.hsl.jore4.mapmatching.repository.routing

import fi.hsl.jore4.mapmatching.model.VehicleType

interface IRoutingRepository {

    fun findRouteViaNetworkNodes(vehicleType: VehicleType, nodeIds: List<Long>): List<RouteLinkDTO>
}
