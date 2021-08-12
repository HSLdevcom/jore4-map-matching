package fi.hsl.jore4.mapmatching.service.routing.response

data class RoutingSuccessDTO(override val code: ResponseCode, val routes: List<RouteResultDTO>) : RoutingResponse
