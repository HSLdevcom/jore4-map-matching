package fi.hsl.jore4.mapmatching.service.common.response

import fi.hsl.jore4.mapmatching.model.LatLng

sealed interface RoutingResponse {
    val code: ResponseCode

    data class RoutingSuccessDTO(override val code: ResponseCode, val routes: List<RouteResultDTO>) : RoutingResponse

    data class RoutingFailureDTO(override val code: ResponseCode, val message: String) : RoutingResponse

    companion object {
        fun ok(route: RouteResultDTO) = RoutingSuccessDTO(ResponseCode.Ok, listOf(route))

        fun invalidUrl(message: String) = RoutingFailureDTO(ResponseCode.InvalidUrl, message)

        fun invalidTransportationMode(transportationMode: String) =
            invalidUrl("Failed to resolve transportation mode from: '$transportationMode'")

        fun invalidTransportationProfile(transportationMode: String, vehicleType: String) =
            invalidUrl("Failed to resolve a valid combination of transportation mode and vehicle type from: '$transportationMode/$vehicleType'")

        fun invalidValue(message: String) = RoutingFailureDTO(ResponseCode.InvalidValue, message)

        fun noSegment(message: String) = RoutingFailureDTO(ResponseCode.NoSegment, message)

        fun noSegment(unmatchedCoordinates: List<LatLng>) =
            noSegment("Could not match infrastructure link for following coordinates: $unmatchedCoordinates")
    }
}
