package fi.hsl.jore4.mapmatching.repository.routing

interface RoutingRepository {

    fun findRouteViaNetworkNodes(nodeIds: List<Int>): List<RouteSegmentDTO>
}
