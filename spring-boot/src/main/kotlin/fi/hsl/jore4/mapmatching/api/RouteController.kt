package fi.hsl.jore4.mapmatching.api

import fi.hsl.jore4.mapmatching.model.LatLng
import fi.hsl.jore4.mapmatching.model.VehicleMode
import fi.hsl.jore4.mapmatching.model.VehicleType
import fi.hsl.jore4.mapmatching.service.common.response.RoutingResponse
import fi.hsl.jore4.mapmatching.service.routing.IRoutingService
import fi.hsl.jore4.mapmatching.web.util.ParameterUtils
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.MediaType
import org.springframework.validation.annotation.Validated
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController

@RestController
@Validated
@RequestMapping(value = [RouteController.URL_PREFIX], produces = [MediaType.APPLICATION_JSON_VALUE])
class RouteController @Autowired constructor(val routingService: IRoutingService) {

    @GetMapping("/$MODE_PARAM/$COORDS_PARAM", "/$MODE_PARAM/$COORDS_PARAM.json")
    fun findRoute(@PathVariable transportationMode: String,
                  @PathVariable coords: String,
                  @RequestParam(required = false) linkSearchRadius: Int?
    ): RoutingResponse {

        if (LOGGER.isDebugEnabled) {
            LOGGER.debug("Given transportation mode: $transportationMode")
            LOGGER.debug("Given coordinate sequence: $coords")
        }

        val vehicleType: VehicleType = findVehicleType(transportationMode, null)
            ?: return RoutingResponse.invalidTransportationMode(transportationMode)

        return findRoute(vehicleType, coords, linkSearchRadius)
    }

    @GetMapping("/$MODE_PARAM/$VEHICLE_TYPE_PARAM/$COORDS_PARAM", "/$MODE_PARAM/$VEHICLE_TYPE_PARAM/$COORDS_PARAM.json")
    fun findRoute(@PathVariable transportationMode: String,
                  @PathVariable vehicleTypeParam: String,
                  @PathVariable coords: String,
                  @RequestParam(required = false) linkSearchRadius: Int?
    ): RoutingResponse {

        if (LOGGER.isDebugEnabled) {
            LOGGER.debug("Given profile: $transportationMode/$vehicleTypeParam")
            LOGGER.debug("Given coordinate sequence: $coords")
        }

        val vehicleType: VehicleType = findVehicleType(transportationMode, vehicleTypeParam)
            ?: return RoutingResponse.invalidTransportationProfile(transportationMode, vehicleTypeParam)

        return findRoute(vehicleType, coords, linkSearchRadius)
    }

    private fun findRoute(vehicleType: VehicleType, coords: String, linkSearchRadius: Int?): RoutingResponse {

        val parsedCoordinates: List<LatLng>

        try {
            parsedCoordinates = ParameterUtils.parseCoordinates(coords)
        } catch (ex: RuntimeException) {
            return RoutingResponse.invalidUrl(ex.message ?: "Failed to parse coordinates")
        }

        return routingService.findRoute(parsedCoordinates,
                                        vehicleType,
                                        linkSearchRadius ?: DEFAULT_LINK_SEARCH_RADIUS)
    }

    companion object {
        const val URL_PREFIX = "/api/route/v1"

        private const val MODE_PARAM = "{transportationMode:[a-zA-Z-_]+}"
        private const val VEHICLE_TYPE_PARAM = "{vehicleTypeParam:[a-zA-Z-_]+}"
        private const val COORDS_PARAM = "{coords:${ParameterUtils.COORDINATE_LIST}}"

        private const val DEFAULT_LINK_SEARCH_RADIUS = 150

        private val LOGGER: Logger = LoggerFactory.getLogger(RouteController::class.java)

        private fun findVehicleType(transportationModeParam: String, vehicleTypeParam: String?): VehicleType? {
            return VehicleMode.from(transportationModeParam)?.let { vehicleMode: VehicleMode ->
                findVehicleType(vehicleMode, vehicleTypeParam)
            }
        }

        private fun findVehicleType(vehicleMode: VehicleMode, vehicleTypeParam: String?): VehicleType? {
            if (vehicleTypeParam != null) {
                // Vehicle type must match with its vehicle mode.
                return VehicleType.from(vehicleTypeParam)?.takeIf { it.vehicleMode == vehicleMode }
            }

            // When given vehicleType is null, resolve default deduced from vehicleMode.
            return when (vehicleMode) {
                VehicleMode.BUS -> VehicleType.GENERIC_BUS
                VehicleMode.FERRY -> VehicleType.GENERIC_FERRY
                VehicleMode.METRO -> VehicleType.GENERIC_METRO
                VehicleMode.TRAIN -> VehicleType.GENERIC_TRAIN
                VehicleMode.TRAM -> VehicleType.GENERIC_TRAM
            }
        }
    }
}
