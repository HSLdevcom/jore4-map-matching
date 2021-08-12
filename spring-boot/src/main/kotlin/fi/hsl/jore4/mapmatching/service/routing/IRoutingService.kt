package fi.hsl.jore4.mapmatching.service.routing

import fi.hsl.jore4.mapmatching.model.LatLng
import fi.hsl.jore4.mapmatching.service.routing.response.RoutingResponse

interface IRoutingService {

    fun findRoute(coordinates: List<LatLng>, linkQueryDistance: Int): RoutingResponse
}
