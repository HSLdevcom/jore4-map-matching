package fi.hsl.jore4.mapmatching.service.routing.response

import fi.hsl.jore4.mapmatching.model.LatLng
import fi.hsl.jore4.mapmatching.service.routing.response.ResponseCode.InvalidUrl
import fi.hsl.jore4.mapmatching.service.routing.response.ResponseCode.InvalidValue
import fi.hsl.jore4.mapmatching.service.routing.response.ResponseCode.NoSegment

data class RoutingFailureDTO(override val code: ResponseCode, val message: String) : RoutingResponse {

    companion object {
        fun invalidUrl(message: String) = RoutingFailureDTO(InvalidUrl, message)

        fun invalidValue(message: String) = RoutingFailureDTO(InvalidValue, message)

        fun noSegment(message: String) = RoutingFailureDTO(NoSegment, message)

        fun noSegment(unmatchedCoordinates: List<LatLng>) =
            noSegment("Could not match a link for following coordinates: $unmatchedCoordinates")
    }
}
