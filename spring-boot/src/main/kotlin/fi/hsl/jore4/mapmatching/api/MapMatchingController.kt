package fi.hsl.jore4.mapmatching.api

import fi.hsl.jore4.mapmatching.api.PublicTransportRouteMatchRequestDTO.MapMatchingParametersDTO
import fi.hsl.jore4.mapmatching.model.VehicleType
import fi.hsl.jore4.mapmatching.service.common.response.RoutingResponse
import fi.hsl.jore4.mapmatching.service.matching.IMatchingService
import fi.hsl.jore4.mapmatching.service.matching.PublicTransportRouteMatchingParameters
import fi.hsl.jore4.mapmatching.service.matching.PublicTransportRouteMatchingParameters.JunctionMatchingParameters
import fi.hsl.jore4.mapmatching.util.LogUtils.joinToLogString
import fi.hsl.jore4.mapmatching.web.util.ParameterUtils.findVehicleType
import io.github.oshai.kotlinlogging.KotlinLogging
import jakarta.validation.Valid
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.MediaType
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

private val LOGGER = KotlinLogging.logger {}

@RestController
@RequestMapping(value = [MapMatchingController.URL_PREFIX], produces = [MediaType.APPLICATION_JSON_VALUE])
class MapMatchingController
    @Autowired
    constructor(
        val matchingService: IMatchingService
    ) {
        @PostMapping(
            "/$TRANSPORTATION_MODE_PARAM",
            "/$TRANSPORTATION_MODE_PARAM.json"
        )
        fun findMatchForPublicTransportRoute(
            @PathVariable transportationMode: String,
            @Valid @RequestBody request: PublicTransportRouteMatchRequestDTO
        ): RoutingResponse {
            LOGGER.debug { "Given transportation mode: $transportationMode" }

            val vehicleType: VehicleType =
                findVehicleType(transportationMode, null)
                    ?: return RoutingResponse.invalidTransportationMode(transportationMode)

            return findMatch(request, vehicleType)
        }

        @PostMapping(
            "/$TRANSPORTATION_MODE_PARAM/$VEHICLE_TYPE_PARAM",
            "/$TRANSPORTATION_MODE_PARAM/$VEHICLE_TYPE_PARAM.json"
        )
        fun findMatchForPublicTransportRoute(
            @PathVariable transportationMode: String,
            @PathVariable vehicleTypeParam: String,
            @Valid @RequestBody request: PublicTransportRouteMatchRequestDTO
        ): RoutingResponse {
            LOGGER.debug { "Given profile: $transportationMode/$vehicleTypeParam" }

            val vehicleType: VehicleType =
                findVehicleType(transportationMode, vehicleTypeParam)
                    ?: return RoutingResponse.invalidTransportationProfile(transportationMode, vehicleTypeParam)

            return findMatch(request, vehicleType)
        }

        private fun findMatch(
            request: PublicTransportRouteMatchRequestDTO,
            vehicleType: VehicleType
        ): RoutingResponse {
            LOGGER.debug { "Given route geometry: ${request.routeGeometry}" }
            LOGGER.debug {
                "Given route points: ${
                    joinToLogString(request.routePoints.withIndex()) { (index, rp) ->
                        "#${index + 1}: $rp"
                    }
                }"
            }

            return try {
                matchingService.findMatchForPublicTransportRoute(
                    request.routeId,
                    request.routeGeometry,
                    request.routePoints,
                    vehicleType,
                    getMatchingParameters(request)
                )
            } catch (ex: Exception) {
                RoutingResponse.invalidUrl(ex.message ?: "Map-matching failed")
            }
        }

        companion object {
            const val URL_PREFIX = "/api/match/public-transport-route/v1"

            private const val TRANSPORTATION_MODE_PARAM = "{transportationMode:[a-zA-Z-_]+}"
            private const val VEHICLE_TYPE_PARAM = "{vehicleTypeParam:[a-zA-Z-_]+}"

            private const val DEFAULT_BUFFER_RADIUS_IN_METERS: Double = 55.0
            private const val DEFAULT_TERMINUS_LINK_QUERY_DISTANCE: Double = 50.0
            private const val DEFAULT_TERMINUS_LINK_QUERY_LIMIT: Int = 5
            private const val DEFAULT_MAX_STOP_LOCATION_DEVIATION: Double = 80.0
            private const val DEFAULT_FALLBACK_TO_VIA_NODES_ALGO: Boolean = true
            private const val DEFAULT_ROAD_JUNCTION_MATCHING_ENABLED: Boolean = true
            private const val DEFAULT_JUNCTION_NODE_MATCH_DISTANCE: Double = 5.0
            private const val DEFAULT_JUNCTION_NODE_CLEARING_DISTANCE: Double = 30.0

            fun getMatchingParameters(
                request: PublicTransportRouteMatchRequestDTO
            ): PublicTransportRouteMatchingParameters {
                val parameters: MapMatchingParametersDTO? = request.matchingParameters

                val bufferRadiusInMeters: Double = parameters?.bufferRadiusInMeters ?: DEFAULT_BUFFER_RADIUS_IN_METERS
                val terminusLinkQueryDistance: Double =
                    parameters?.terminusLinkQueryDistance ?: DEFAULT_TERMINUS_LINK_QUERY_DISTANCE
                val terminusLinkQueryLimit: Int =
                    parameters?.terminusLinkQueryLimit ?: DEFAULT_TERMINUS_LINK_QUERY_LIMIT
                val maxStopLocationDeviation: Double =
                    parameters?.maxStopLocationDeviation ?: DEFAULT_MAX_STOP_LOCATION_DEVIATION
                val fallbackToViaNodesAlgorithm: Boolean =
                    parameters?.fallbackToViaNodesAlgorithm ?: DEFAULT_FALLBACK_TO_VIA_NODES_ALGO

                val roadJunctionMatchingParameters: JunctionMatchingParameters? =
                    if (parameters?.roadJunctionMatchingEnabled ?: DEFAULT_ROAD_JUNCTION_MATCHING_ENABLED) {
                        val junctionNodeMatchDistance: Double =
                            parameters?.junctionNodeMatchDistance ?: DEFAULT_JUNCTION_NODE_MATCH_DISTANCE
                        val junctionNodeClearingDistance: Double =
                            parameters?.junctionNodeClearingDistance ?: DEFAULT_JUNCTION_NODE_CLEARING_DISTANCE

                        JunctionMatchingParameters(junctionNodeMatchDistance, junctionNodeClearingDistance)
                    } else {
                        null
                    }

                return PublicTransportRouteMatchingParameters(
                    bufferRadiusInMeters,
                    terminusLinkQueryDistance,
                    terminusLinkQueryLimit,
                    maxStopLocationDeviation,
                    fallbackToViaNodesAlgorithm,
                    roadJunctionMatchingParameters
                )
            }
        }
    }
