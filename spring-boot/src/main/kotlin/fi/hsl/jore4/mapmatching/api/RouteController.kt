package fi.hsl.jore4.mapmatching.api

import fi.hsl.jore4.mapmatching.model.LatLng
import fi.hsl.jore4.mapmatching.model.VehicleType
import fi.hsl.jore4.mapmatching.service.common.response.RoutingResponse
import fi.hsl.jore4.mapmatching.service.routing.IRoutingService
import fi.hsl.jore4.mapmatching.service.routing.RoutingExtraParameters
import fi.hsl.jore4.mapmatching.util.GeolatteUtils.toPoints
import fi.hsl.jore4.mapmatching.web.util.ParameterUtils
import fi.hsl.jore4.mapmatching.web.util.ParameterUtils.findVehicleType
import io.github.oshai.kotlinlogging.KotlinLogging
import jakarta.validation.Valid
import jakarta.validation.constraints.Pattern
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.MediaType
import org.springframework.validation.annotation.Validated
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController

private val LOGGER = KotlinLogging.logger {}

@RestController
@Validated
@RequestMapping(value = [RouteController.URL_PREFIX], produces = [MediaType.APPLICATION_JSON_VALUE])
class RouteController
    @Autowired
    constructor(
        val routingService: IRoutingService
    ) {
        @Deprecated("GET request should be replaced with POST")
        @GetMapping(
            "/$TRANSPORTATION_MODE_PARAM/{coords}",
            "/$TRANSPORTATION_MODE_PARAM/{coords}.json"
        )
        fun findRoute(
            @PathVariable transportationMode: String,
            @Pattern(regexp = ParameterUtils.COORDINATE_LIST) @PathVariable coords: String,
            @RequestParam(required = false) linkSearchRadius: Int?,
            @RequestParam(required = false) simplifyClosedLoopTraversals: Boolean?
        ): RoutingResponse {
            LOGGER.debug { "Given transportation mode: $transportationMode" }
            LOGGER.debug { "Given coordinate sequence: $coords" }

            val vehicleType: VehicleType =
                findVehicleType(transportationMode, null)
                    ?: return RoutingResponse.invalidTransportationMode(transportationMode)

            return findRouteInternal(coords, vehicleType, linkSearchRadius, simplifyClosedLoopTraversals)
        }

        @PostMapping("/$TRANSPORTATION_MODE_PARAM", consumes = [MediaType.APPLICATION_JSON_VALUE])
        fun findRoute(
            @PathVariable transportationMode: String,
            @Valid @RequestBody request: PublicTransportRouteFindRequestDTO
        ): RoutingResponse {
            LOGGER.debug { "Given transportation mode: $transportationMode" }
            LOGGER.debug { "Given coordinate points: ${request.routePoints}" }

            val vehicleType: VehicleType =
                findVehicleType(transportationMode, null)
                    ?: return RoutingResponse.invalidTransportationMode(transportationMode)

            return findRouteInternal(
                request.routePoints,
                vehicleType,
                request.linkSearchRadius,
                request.simplifyClosedLoopTraversals
            )
        }

        @Deprecated("GET request should be replaced with POST")
        @GetMapping(
            "/$TRANSPORTATION_MODE_PARAM/$VEHICLE_TYPE_PARAM/{coords}",
            "/$TRANSPORTATION_MODE_PARAM/$VEHICLE_TYPE_PARAM/{coords}.json"
        )
        fun findRoute(
            @PathVariable transportationMode: String,
            @PathVariable vehicleTypeParam: String,
            @Pattern(regexp = ParameterUtils.COORDINATE_LIST) @PathVariable coords: String,
            @RequestParam(required = false) linkSearchRadius: Int?,
            @RequestParam(required = false) simplifyClosedLoopTraversals: Boolean?
        ): RoutingResponse {
            LOGGER.debug { "Given profile: $transportationMode/$vehicleTypeParam" }
            LOGGER.debug { "Given coordinate sequence: $coords" }

            val vehicleType: VehicleType =
                findVehicleType(transportationMode, vehicleTypeParam)
                    ?: return RoutingResponse.invalidTransportationProfile(transportationMode, vehicleTypeParam)

            return findRouteInternal(coords, vehicleType, linkSearchRadius, simplifyClosedLoopTraversals)
        }

        @PostMapping("/$TRANSPORTATION_MODE_PARAM/$VEHICLE_TYPE_PARAM", consumes = [MediaType.APPLICATION_JSON_VALUE])
        fun findRoute(
            @PathVariable transportationMode: String,
            @PathVariable vehicleTypeParam: String,
            @Valid @RequestBody request: PublicTransportRouteFindRequestDTO
        ): RoutingResponse {
            LOGGER.debug { "Given profile: $transportationMode/$vehicleTypeParam" }
            LOGGER.debug { "Given coordinate points: ${request.routePoints}" }

            val vehicleType: VehicleType =
                findVehicleType(transportationMode, vehicleTypeParam)
                    ?: return RoutingResponse.invalidTransportationProfile(transportationMode, vehicleTypeParam)

            return findRouteInternal(
                request.routePoints,
                vehicleType,
                request.linkSearchRadius,
                request.simplifyClosedLoopTraversals
            )
        }

        private fun findRouteInternal(
            coords: String,
            vehicleType: VehicleType,
            linkSearchRadius: Int?,
            simplifyClosedLoopTraversals: Boolean?
        ): RoutingResponse {
            val parsedCoordinates: List<LatLng>

            try {
                parsedCoordinates = ParameterUtils.parseCoordinates(coords)
            } catch (ex: RuntimeException) {
                return RoutingResponse.invalidUrl(ex.message ?: "Failed to parse coordinates")
            }

            return findRouteInternal(
                parsedCoordinates,
                vehicleType,
                linkSearchRadius,
                simplifyClosedLoopTraversals
            )
        }

        private fun findRouteInternal(
            coords: List<LatLng>,
            vehicleType: VehicleType,
            linkSearchRadius: Int?,
            simplifyClosedLoopTraversals: Boolean?
        ): RoutingResponse =
            routingService.findRoute(
                toPoints(coords),
                vehicleType,
                RoutingExtraParameters(
                    linkSearchRadius ?: DEFAULT_LINK_SEARCH_RADIUS,
                    simplifyClosedLoopTraversals ?: DEFAULT_SIMPLIFY_CLOSED_LOOP_TRAVERSALS
                )
            )

        companion object {
            const val URL_PREFIX = "/api/route/v1"

            private const val TRANSPORTATION_MODE_PARAM = "{transportationMode:[a-zA-Z-_]+}"
            private const val VEHICLE_TYPE_PARAM = "{vehicleTypeParam:[a-zA-Z-_]+}"

            private const val DEFAULT_LINK_SEARCH_RADIUS = 150
            private const val DEFAULT_SIMPLIFY_CLOSED_LOOP_TRAVERSALS = true
        }
    }
