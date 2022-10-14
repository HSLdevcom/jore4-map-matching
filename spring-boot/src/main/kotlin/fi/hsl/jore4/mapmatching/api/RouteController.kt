package fi.hsl.jore4.mapmatching.api

import fi.hsl.jore4.mapmatching.model.LatLng
import fi.hsl.jore4.mapmatching.model.VehicleType
import fi.hsl.jore4.mapmatching.service.common.response.RoutingResponse
import fi.hsl.jore4.mapmatching.service.routing.IRoutingService
import fi.hsl.jore4.mapmatching.util.GeolatteUtils.toPoints
import fi.hsl.jore4.mapmatching.web.util.ParameterUtils
import fi.hsl.jore4.mapmatching.web.util.ParameterUtils.findVehicleType
import mu.KotlinLogging
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.MediaType
import org.springframework.validation.annotation.Validated
import org.springframework.web.bind.annotation.*
import javax.validation.Valid
import javax.validation.constraints.Pattern

private val LOGGER = KotlinLogging.logger {}

@RestController
@Validated
@RequestMapping(value = [RouteController.URL_PREFIX], produces = [MediaType.APPLICATION_JSON_VALUE])
class RouteController @Autowired constructor(val routingService: IRoutingService) {

    @Deprecated("GET request should be replaced with POST")
    @GetMapping("/$TRANSPORTATION_MODE_PARAM/{coords}",
                "/$TRANSPORTATION_MODE_PARAM/{coords}.json")
    fun findRoute(@PathVariable transportationMode: String,
                  @Pattern(regexp = ParameterUtils.COORDINATE_LIST) @PathVariable coords: String,
                  @RequestParam(required = false) linkSearchRadius: Int?
    ): RoutingResponse {

        LOGGER.debug { "Given transportation mode: $transportationMode" }
        LOGGER.debug { "Given coordinate sequence: $coords" }

        val vehicleType: VehicleType = findVehicleType(transportationMode, null)
            ?: return RoutingResponse.invalidTransportationMode(transportationMode)

        return findRouteInternal(coords, vehicleType, linkSearchRadius)
    }

    @PostMapping("/$TRANSPORTATION_MODE_PARAM", consumes = [MediaType.APPLICATION_JSON_VALUE])
    fun findRoute(@PathVariable transportationMode: String,
                  @Valid @RequestBody request: PublicTransportRouteFindRequestDTO)
        : RoutingResponse {

        LOGGER.debug { "Given transportation mode: $transportationMode" }
        LOGGER.debug { "Given coordinate points: ${request.routePoints}" }

        val vehicleType: VehicleType = findVehicleType(transportationMode, null)
            ?: return RoutingResponse.invalidTransportationMode(transportationMode)

        return findRouteInternal(request.routePoints,
                                 vehicleType,
                                 request.linkSearchRadius)
    }

    @Deprecated("GET request should be replaced with POST")
    @GetMapping("/$TRANSPORTATION_MODE_PARAM/$VEHICLE_TYPE_PARAM/{coords}",
                "/$TRANSPORTATION_MODE_PARAM/$VEHICLE_TYPE_PARAM/{coords}.json")
    fun findRoute(@PathVariable transportationMode: String,
                  @PathVariable vehicleTypeParam: String,
                  @Pattern(regexp = ParameterUtils.COORDINATE_LIST) @PathVariable coords: String,
                  @RequestParam(required = false) linkSearchRadius: Int?)
        : RoutingResponse {

        LOGGER.debug { "Given profile: $transportationMode/$vehicleTypeParam" }
        LOGGER.debug { "Given coordinate sequence: $coords" }

        val vehicleType: VehicleType = findVehicleType(transportationMode, vehicleTypeParam)
            ?: return RoutingResponse.invalidTransportationProfile(transportationMode, vehicleTypeParam)

        return findRouteInternal(coords, vehicleType, linkSearchRadius)
    }

    @PostMapping("/$TRANSPORTATION_MODE_PARAM/$VEHICLE_TYPE_PARAM", consumes = [MediaType.APPLICATION_JSON_VALUE])
    fun findRoute(@PathVariable transportationMode: String,
                  @PathVariable vehicleTypeParam: String,
                  @Valid @RequestBody request: PublicTransportRouteFindRequestDTO)
        : RoutingResponse {

        LOGGER.debug { "Given profile: $transportationMode/$vehicleTypeParam" }
        LOGGER.debug { "Given coordinate points: ${request.routePoints}" }

        val vehicleType: VehicleType = findVehicleType(transportationMode, vehicleTypeParam)
            ?: return RoutingResponse.invalidTransportationProfile(transportationMode, vehicleTypeParam)

        return findRouteInternal(request.routePoints,
                                 vehicleType,
                                 request.linkSearchRadius)
    }

    private fun findRouteInternal(coords: String, vehicleType: VehicleType, linkSearchRadius: Int?)
        : RoutingResponse {

        val parsedCoordinates: List<LatLng>

        try {
            parsedCoordinates = ParameterUtils.parseCoordinates(coords)
        } catch (ex: RuntimeException) {
            return RoutingResponse.invalidUrl(ex.message ?: "Failed to parse coordinates")
        }

        return findRouteInternal(parsedCoordinates, vehicleType, linkSearchRadius)
    }

    private fun findRouteInternal(coords: List<LatLng>, vehicleType: VehicleType, linkSearchRadius: Int?)
        : RoutingResponse {

        return routingService.findRoute(toPoints(coords),
                                        vehicleType,
                                        linkSearchRadius ?: DEFAULT_LINK_SEARCH_RADIUS)
    }

    companion object {
        const val URL_PREFIX = "/api/route/v1"

        private const val TRANSPORTATION_MODE_PARAM = "{transportationMode:[a-zA-Z-_]+}"
        private const val VEHICLE_TYPE_PARAM = "{vehicleTypeParam:[a-zA-Z-_]+}"

        private const val DEFAULT_LINK_SEARCH_RADIUS = 150
    }
}
