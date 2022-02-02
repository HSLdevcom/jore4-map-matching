package fi.hsl.jore4.mapmatching.service.matching.test

import fi.hsl.jore4.mapmatching.model.VehicleType.GENERIC_BUS
import fi.hsl.jore4.mapmatching.model.matching.RoutePoint
import fi.hsl.jore4.mapmatching.service.common.response.RoutingResponse
import fi.hsl.jore4.mapmatching.service.matching.IMatchingService
import fi.hsl.jore4.mapmatching.service.matching.PublicTransportRouteMatchingParameters
import fi.hsl.jore4.mapmatching.service.matching.PublicTransportRouteMatchingParameters.JunctionMatchingParameters
import fi.hsl.jore4.mapmatching.service.matching.test.SuccessfulMatchResult.BufferRadius
import fi.hsl.jore4.mapmatching.service.matching.test.SuccessfulMatchResult.MatchDetails
import fi.hsl.jore4.mapmatching.util.GeolatteUtils.length
import mu.KotlinLogging
import org.geolatte.geom.G2D
import org.geolatte.geom.LineString
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service
import javax.annotation.PostConstruct
import kotlin.time.Duration
import kotlin.time.ExperimentalTime
import kotlin.time.measureTime

private val LOGGER = KotlinLogging.logger {}

@Service
class MapMatchingBulkTester @Autowired constructor(val csvParser: IPublicTransportRouteCsvParser,
                                                   val matchingService: IMatchingService,
                                                   val resultsPublisher: IMapMatchingBulkTestResultsPublisher,
                                                   @Value("\${test.routes.csvfile}") val csvFile: String) {

    @PostConstruct
    @OptIn(ExperimentalTime::class)
    fun launchRouteTesting() {
        LOGGER.info("Starting to map-match routes from file...")

        val duration: Duration = measureTime {
            val matchResults: List<MatchResult> = processRoutes()

            resultsPublisher.publishResults(matchResults)
        }

        LOGGER.info("Finished map-matching routes in {}", duration)
    }

    fun processRoutes(): List<MatchResult> {
        LOGGER.info("Loading public transport routes from file: {}", csvFile)

        val sourceRoutes: List<PublicTransportRoute> = csvParser.parsePublicTransportRoutes(csvFile)

        val matchResults: List<MatchResult> = sourceRoutes.map { (routeId, routeGeometry, routePoints) ->
            LOGGER.info("Starting to match route:    {}", routeId)

            val result: MatchResult = matchRoute(routeId, routeGeometry, routePoints)

            if (result.matchFound)
                LOGGER.info("Successfully matched route: {}", routeId)
            else
                LOGGER.info("Failed to match route:      {}", routeId)

            result
        }

        return matchResults
    }

    private fun matchRoute(routeId: String,
                           geometry: LineString<G2D>,
                           routePoints: List<RoutePoint>): MatchResult {

        val matchingParams: PublicTransportRouteMatchingParameters = getMatchingParameters(50.0)

        val response: RoutingResponse = matchingService.findMatchForPublicTransportRoute(routeId,
                                                                                         geometry,
                                                                                         routePoints,
                                                                                         GENERIC_BUS,
                                                                                         matchingParams)

        val lengthOfSourceRoute: Double = length(geometry)

        return when (response) {
            is RoutingResponse.RoutingSuccessDTO -> {
                SuccessfulMatchResult(routeId,
                                      geometry,
                                      lengthOfSourceRoute,
                                      createMatchDetails(response,
                                                         geometry,
                                                         matchingParams.bufferRadiusInMeters))
            }

            else -> MatchFailure(routeId, geometry, lengthOfSourceRoute)
        }
    }

    companion object {

        private fun getMatchingParameters(bufferRadius: Double): PublicTransportRouteMatchingParameters {
            val roadJunctionMatchingParams = JunctionMatchingParameters(matchDistance = 5.0, clearingDistance = 30.0)

            return PublicTransportRouteMatchingParameters(bufferRadiusInMeters = bufferRadius,
                                                          terminusLinkQueryDistance = 100.0,
                                                          terminusLinkQueryLimit = 5,
                                                          maxStopLocationDeviation = 80.0,
                                                          roadJunctionMatching = roadJunctionMatchingParams)
        }

        private fun createMatchDetails(routingResponse: RoutingResponse.RoutingSuccessDTO,
                                       sourceGeometry: LineString<G2D>,
                                       bufferRadius: Double): MatchDetails {

            val resultGeometry: LineString<G2D> = routingResponse.routes[0].geometry

            return MatchDetails(
                mapOf(
                    BufferRadius(bufferRadius) to length(resultGeometry)
                ).toSortedMap()
            )
        }
    }
}
