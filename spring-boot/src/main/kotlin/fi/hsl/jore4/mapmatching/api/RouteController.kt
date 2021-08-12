package fi.hsl.jore4.mapmatching.api

import fi.hsl.jore4.mapmatching.model.LatLng
import fi.hsl.jore4.mapmatching.service.routing.IRoutingService
import fi.hsl.jore4.mapmatching.service.routing.response.RoutingFailureDTO
import fi.hsl.jore4.mapmatching.service.routing.response.RoutingResponse
import fi.hsl.jore4.mapmatching.web.util.ParameterUtils.parseCoordinates
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

    @GetMapping("/bus/{coords}")
    fun findRoute(@PathVariable coords: String,
                  @RequestParam(required = false) linkSearchRadius: Int?
    ): RoutingResponse {

        if (LOGGER.isDebugEnabled) {
            LOGGER.debug("Given coordinate sequence: $coords")
        }

        val parsedCoordinates: List<LatLng>

        try {
            parsedCoordinates = parseCoordinates(coords)
        } catch (ex: RuntimeException) {
            return RoutingFailureDTO.invalidUrl(ex.message ?: "Failed to parse coordinates")
        }

        return routingService.findRoute(parsedCoordinates,
                                        linkSearchRadius ?: DEFAULT_LINK_SEARCH_RADIUS)
    }

    companion object {
        const val URL_PREFIX = "/api/route/v1"

        private const val DEFAULT_LINK_SEARCH_RADIUS = 150

        private val LOGGER: Logger = LoggerFactory.getLogger(RouteController::class.java)
    }
}
