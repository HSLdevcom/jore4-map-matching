package fi.hsl.jore4.mapmatching.service.common.response

data class RoutingSuccessDTO(override val code: ResponseCode, val routes: List<RouteResultDTO>) : RoutingResponse
