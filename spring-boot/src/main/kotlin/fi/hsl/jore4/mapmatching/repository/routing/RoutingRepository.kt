package fi.hsl.jore4.mapmatching.repository.routing

interface RoutingRepository {

    fun findRouteViaNetworkNodes(params: NetworkNodeParams): List<RouteSegmentDTO>
}
