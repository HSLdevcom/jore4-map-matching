package fi.hsl.jore4.mapmatching.service.matching.test

import fi.hsl.jore4.mapmatching.model.VehicleType.GENERIC_BUS
import fi.hsl.jore4.mapmatching.model.matching.RoutePoint
import fi.hsl.jore4.mapmatching.service.common.response.LinkTraversalDTO
import fi.hsl.jore4.mapmatching.service.common.response.RouteResultDTO
import fi.hsl.jore4.mapmatching.service.common.response.RoutingResponse
import fi.hsl.jore4.mapmatching.service.matching.IMatchingService
import fi.hsl.jore4.mapmatching.service.matching.PublicTransportRouteMatchingParameters
import fi.hsl.jore4.mapmatching.service.matching.PublicTransportRouteMatchingParameters.JunctionMatchingParameters
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
            val (routeMatchResults, stopToStopSegmentMatchResults) = processFile()

            resultsPublisher.publishMatchResultsForRoutesAndStopToStopSegments(routeMatchResults,
                                                                               stopToStopSegmentMatchResults)
        }

        LOGGER.info("Finished map-matching routes in {}", duration)
    }

    fun processFile(): Pair<List<MatchResult>, List<SegmentMatchResult>> {
        LOGGER.info("Loading public transport routes from file: {}", csvFile)

        val sourceRoutes: List<PublicTransportRoute> = csvParser.parsePublicTransportRoutes(csvFile)

        LOGGER.info("Number of source routes: {}", sourceRoutes.size)

        val (stopToStopSegments: List<StopToStopSegment>, discardedRoutes: List<String>) =
            ExtractStopToStopSegments.extractStopToStopSegments(sourceRoutes)

        LOGGER.info("Number of stop-to-stop segments: {}", stopToStopSegments.size)
        LOGGER.info("Number of discarded routes within resolution of stop-to-stop segments: {}", discardedRoutes.size)

        val routeMatchResults: List<MatchResult> = matchRoutes(sourceRoutes, listOf(55.0))
        val segmentMatchResults: List<SegmentMatchResult> = matchStopToStopSegments(stopToStopSegments, listOf(55.0))

        return routeMatchResults to segmentMatchResults
    }

    private fun matchRoutes(routes: List<PublicTransportRoute>, bufferRadiuses: List<Double>): List<MatchResult> {
        return routes.map { (routeId, routeGeometry, routePoints) ->
            LOGGER.info("Starting to match route:    {}", routeId)

            val result: MatchResult = matchRoute(routeId, routeGeometry, routePoints, bufferRadiuses)

            if (result is SuccessfulMatchResult)
                LOGGER.info("Successfully matched route: {} (with bufferRadius={})",
                            routeId, result.getLowestBufferRadius())
            else
                LOGGER.info("Failed to match route:      {}", routeId)

            result
        }
    }

    private fun matchStopToStopSegments(segments: List<StopToStopSegment>, bufferRadiuses: List<Double>)
        : List<SegmentMatchResult> {

        return segments.map { segment ->
            val (segmentId, geometry, routePoints, referencingRoutes) = segment

            LOGGER.info("Starting to match stop-to-stop segment:    {}", segmentId)

            val result: MatchResult = matchRoute(segmentId, geometry, routePoints, bufferRadiuses)

            if (result is SuccessfulMatchResult)
                LOGGER.info("Successfully matched stop-to-stop segment: {} (with bufferRadius={})",
                            segmentId, result.getLowestBufferRadius())
            else
                LOGGER.info("Failed to match stop-to-stop segment:      {}", segmentId)

            val numRoutePoints = routePoints.size

            when (result) {
                is SuccessfulRouteMatchResult -> SuccessfulSegmentMatchResult(segmentId,
                                                                              geometry,
                                                                              result.sourceRouteLength,
                                                                              result.details,
                                                                              segment.startStopId,
                                                                              segment.endStopId,
                                                                              numRoutePoints,
                                                                              referencingRoutes)

                is RouteMatchFailure -> SegmentMatchFailure(segmentId,
                                                            geometry,
                                                            result.sourceRouteLength,
                                                            result.bufferRadius,
                                                            segment.startStopId,
                                                            segment.endStopId,
                                                            numRoutePoints,
                                                            referencingRoutes)

                else -> throw IllegalStateException("Unknown route match result type")
            }
        }
    }

    private fun matchRoute(routeId: String,
                           geometry: LineString<G2D>,
                           routePoints: List<RoutePoint>,
                           bufferRadiuses: List<Double>)
        : MatchResult {

        val sortedBufferRadiuses: List<Double> = bufferRadiuses.sorted()

        val lengthOfSourceRoute: Double = length(geometry)

        val lengthsOfMatchedRoutes: MutableList<Pair<BufferRadius, Double>> = mutableListOf()
        val unsuccessfulBufferRadiuses: MutableSet<BufferRadius> = mutableSetOf()

        sortedBufferRadiuses.forEach { radius ->
            val matchingParams: PublicTransportRouteMatchingParameters = getMatchingParameters(radius)

            val response: RoutingResponse = matchingService.findMatchForPublicTransportRoute(routeId,
                                                                                             geometry,
                                                                                             routePoints,
                                                                                             GENERIC_BUS,
                                                                                             matchingParams)

            val bufferRadius = BufferRadius(radius)

            if (response is RoutingResponse.RoutingSuccessDTO) {
                val route: RouteResultDTO = response.routes[0]
                val lengthOfMatchedRoute: Double = calculateLengthOfRoute(route)
                //val lengthOfMatchedRoute: Double = length(route.geometry)

                lengthsOfMatchedRoutes.add(bufferRadius to lengthOfMatchedRoute)
            } else {
                unsuccessfulBufferRadiuses.add(bufferRadius)
            }
        }

        return when (lengthsOfMatchedRoutes.size) {
            0 -> RouteMatchFailure(routeId,
                                   geometry,
                                   lengthOfSourceRoute,
                                   BufferRadius(sortedBufferRadiuses.last()))
            else -> SuccessfulRouteMatchResult(routeId,
                                               geometry,
                                               lengthOfSourceRoute,
                                               MatchDetails(
                                                   lengthsOfMatchedRoutes.toMap().toSortedMap(),
                                                   unsuccessfulBufferRadiuses
                                               ))
        }
    }

    companion object {

        private fun getMatchingParameters(bufferRadius: Double): PublicTransportRouteMatchingParameters {
            val roadJunctionMatchingParams = JunctionMatchingParameters(matchDistance = 5.0, clearingDistance = 30.0)

            return PublicTransportRouteMatchingParameters(bufferRadiusInMeters = bufferRadius,
                                                          terminusLinkQueryDistance = 100.0,
                                                          terminusLinkQueryLimit = 5,
                                                          maxStopLocationDeviation = 80.0,
                                                          fallbackToViaNodesAlgorithm = true,
                                                          roadJunctionMatching = roadJunctionMatchingParams)
        }

        private fun calculateLengthOfRoute(route: RouteResultDTO): Double {
            val linkTraversals: List<LinkTraversalDTO> = route.paths

            return when (linkTraversals.size) {
                0 -> 0.0
                1, 2 -> route.weight
                else -> {
                    val sumOfInterimLinkLengths: Double =
                        linkTraversals.drop(1).dropLast(1).map { it.distance }.fold(0.0) { acc, distance ->
                            acc + distance
                        }

                    linkTraversals.first().weight + sumOfInterimLinkLengths + linkTraversals.last().weight
                }
            }
        }
    }
}
