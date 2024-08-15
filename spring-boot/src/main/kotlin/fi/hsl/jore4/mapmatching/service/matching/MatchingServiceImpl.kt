package fi.hsl.jore4.mapmatching.service.matching

import fi.hsl.jore4.mapmatching.model.VehicleType
import fi.hsl.jore4.mapmatching.model.matching.RoutePoint
import fi.hsl.jore4.mapmatching.service.common.response.RoutingResponse
import fi.hsl.jore4.mapmatching.service.matching.MatchingServiceHelper.validateInputForRouteMatching
import io.github.oshai.kotlinlogging.KotlinLogging
import org.geolatte.geom.G2D
import org.geolatte.geom.LineString
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

private val LOGGER = KotlinLogging.logger {}

@Service
class MatchingServiceImpl
    @Autowired
    constructor(
        val viaPointsOnLinksService: IMatchRouteViaPointsOnLinksService,
        val viaNodesService: IMatchRouteViaNetworkNodesService,
    ) : IMatchingService {
        @Transactional(readOnly = true)
        override fun findMatchForPublicTransportRoute(
            sourceRouteId: String?,
            sourceRouteGeometry: LineString<G2D>,
            sourceRoutePoints: List<RoutePoint>,
            vehicleType: VehicleType,
            matchingParameters: PublicTransportRouteMatchingParameters,
        ): RoutingResponse {
            validateInputForRouteMatching(sourceRoutePoints, vehicleType)?.let { validationError ->
                return RoutingResponse.invalidValue(validationError)
            }

            val viaPointsOnLinksResponse: RoutingResponse =
                viaPointsOnLinksService
                    .findMatchForPublicTransportRoute(
                        sourceRouteGeometry,
                        sourceRoutePoints,
                        vehicleType,
                        matchingParameters,
                    )

            val routeName: String = sourceRouteId?.let { "route $it" } ?: "route"

            return when (viaPointsOnLinksResponse) {
                is RoutingResponse.RoutingSuccessDTO -> {
                    LOGGER.info { "Matching $routeName succeeded" }
                    viaPointsOnLinksResponse
                }

                is RoutingResponse.RoutingFailureDTO -> {
                    LOGGER.info {
                        "Matching $routeName using via-graph-edges algorithm failed: ${viaPointsOnLinksResponse.message}"
                    }

                    when (matchingParameters.fallbackToViaNodesAlgorithm) {
                        false -> viaPointsOnLinksResponse
                        else -> {
                            LOGGER.info { "Trying to match $routeName using via-graph-vertices algorithm..." }

                            val viaNodesResponse: RoutingResponse =
                                viaNodesService
                                    .findMatchForPublicTransportRoute(
                                        sourceRouteGeometry,
                                        sourceRoutePoints,
                                        vehicleType,
                                        matchingParameters,
                                    )

                            LOGGER.info {
                                if (viaNodesResponse is RoutingResponse.RoutingSuccessDTO) {
                                    "Matching $routeName using via-graph-vertices algorithm succeeded"
                                } else {
                                    "Matching $routeName using via-graph-vertices algorithm failed: ${viaPointsOnLinksResponse.message}"
                                }
                            }

                            viaNodesResponse
                        }
                    }
                }
            }
        }
    }
