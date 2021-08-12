package fi.hsl.jore4.mapmatching.repository.routing

interface IRoutingRepository {

    fun findRouteViaNetworkNodes(params: NetworkNodeParams): List<RouteLinkDTO>
}
