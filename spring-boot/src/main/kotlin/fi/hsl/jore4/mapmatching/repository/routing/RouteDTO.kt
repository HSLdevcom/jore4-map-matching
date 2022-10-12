package fi.hsl.jore4.mapmatching.repository.routing

data class RouteDTO(val routeLinks: List<RouteLinkDTO>) {

    companion object {
        val EMPTY = RouteDTO(emptyList())
    }
}
