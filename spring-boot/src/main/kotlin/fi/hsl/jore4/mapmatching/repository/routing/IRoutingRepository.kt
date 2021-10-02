package fi.hsl.jore4.mapmatching.repository.routing

interface IRoutingRepository {

    fun findRouteViaNetworkNodes(nodeIds: List<Long>): List<RouteLinkDTO>
}
