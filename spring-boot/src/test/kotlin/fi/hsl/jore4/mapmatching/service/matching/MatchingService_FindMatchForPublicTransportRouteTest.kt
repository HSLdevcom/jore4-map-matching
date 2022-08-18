package fi.hsl.jore4.mapmatching.service.matching

import fi.hsl.jore4.mapmatching.model.VehicleType
import fi.hsl.jore4.mapmatching.model.matching.RouteJunctionPoint
import fi.hsl.jore4.mapmatching.model.matching.RoutePoint
import fi.hsl.jore4.mapmatching.model.matching.RouteStopPoint
import fi.hsl.jore4.mapmatching.service.common.response.LinkTraversalDTO
import fi.hsl.jore4.mapmatching.service.common.response.ResponseCode
import fi.hsl.jore4.mapmatching.service.common.response.RoutingResponse
import fi.hsl.jore4.mapmatching.service.matching.PublicTransportRouteMatchingParameters.JunctionMatchingParameters
import fi.hsl.jore4.mapmatching.test.IntTest
import fi.hsl.jore4.mapmatching.test.IntegrationTest
import fi.hsl.jore4.mapmatching.util.GeolatteUtils.toPoint
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.fail
import org.geolatte.geom.G2D
import org.geolatte.geom.Geometries.mkLineString
import org.geolatte.geom.LineString
import org.geolatte.geom.PositionSequenceBuilders
import org.geolatte.geom.builder.DSL.g
import org.geolatte.geom.crs.CoordinateReferenceSystems.WGS84
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired

@IntTest
@Suppress("ClassName")
class MatchingService_FindMatchForPublicTransportRouteTest @Autowired constructor(val matchingService: IMatchingService)
    : IntegrationTest() {

    private fun matchPublicTransportRoute(routeGeometry: LineString<G2D>,
                                          routePoints: List<RoutePoint>,
                                          vehicleType: VehicleType,
                                          matchingParameters: PublicTransportRouteMatchingParameters = DEFAULT_MATCHING_PARAMS)
        : RoutingResponse = matchingService.findMatchForPublicTransportRoute(routeGeometry,
                                                                             routePoints,
                                                                             vehicleType,
                                                                             matchingParameters)

    private fun matchRouteAndCheckAssertionsOnSuccessResponse(
        routeGeometry: LineString<G2D>,
        routePoints: List<RoutePoint>,
        vehicleType: VehicleType,
        matchingParameters: PublicTransportRouteMatchingParameters = DEFAULT_MATCHING_PARAMS,
        checkAssertions: (response: RoutingResponse.RoutingSuccessDTO) -> Unit) {

        val response: RoutingResponse = matchPublicTransportRoute(routeGeometry,
                                                                  routePoints,
                                                                  vehicleType,
                                                                  matchingParameters)

        when (response) {
            is RoutingResponse.RoutingSuccessDTO -> {
                // Verify common assertions.
                assertThat(response.code).isEqualTo(ResponseCode.Ok)
                assertThat(response.routes).hasSize(1)

                // Check custom assertions provided within block parameter.
                checkAssertions(response)
            }

            else -> fail<Void>("RoutingResponse is not a success response as expected")
        }
    }

    @Nested
    @DisplayName("With smoke test")
    inner class WithSmokeTest {

        private fun createSourceGeometry(): LineString<G2D> {
            val positionSequenceBuilder = PositionSequenceBuilders.variableSized(G2D::class.java)
                .add(25.008022, 60.18773)
                .add(25.007984, 60.187677)
                .add(25.007197, 60.186677)
                .add(25.006818, 60.186097)
                .add(25.0062, 60.185922)
                .add(25.00573, 60.18589)
                .add(25.004165, 60.185955)
                .add(25.003768, 60.185967)

            return mkLineString(positionSequenceBuilder.toPositionSequence(), WGS84)
        }

        private fun createSourceRoutePoints(): List<RoutePoint> = listOf(
            RouteStopPoint(location = toPoint(g(25.007994, 60.187739)),
                           projectedLocation = toPoint(g(25.008022, 60.18773)),
                           nationalId = 240525,
                           passengerId = "H4026"),
            RouteJunctionPoint(location = toPoint(g(25.006818, 60.186097))),
            RouteStopPoint(location = toPoint(g(25.00323, 60.18605)),
                           projectedLocation = toPoint(g(25.003768, 60.185967)),
                           nationalId = 240681,
                           passengerId = "H4034")
        )

        @Test
        fun shouldReturnExpectedGeometry() {
            matchRouteAndCheckAssertionsOnSuccessResponse(createSourceGeometry(),
                                                          createSourceRoutePoints(),
                                                          VehicleType.GENERIC_BUS,
                                                          DEFAULT_MATCHING_PARAMS) { resp ->

                val positionSequenceBuilder = PositionSequenceBuilders.variableSized(G2D::class.java)
                    .add(25.00802519622538, 60.18772920125103)
                    .add(25.00797417868717, 60.187678573925)
                    .add(25.0072063517561, 60.18667322842173)
                    .add(25.00691766531791, 60.1862738972099)
                    .add(25.00683990177706, 60.18610546064316)
                    .add(25.00652058879184, 60.18600404182171)
                    .add(25.00615281454607, 60.185920512407115)
                    .add(25.005805753048907, 60.185892468525985)
                    .add(25.005417525805708, 60.18590264138491)
                    .add(25.004908706323214, 60.18592247667472)
                    .add(25.00429811282315, 60.185946490065355)
                    .add(25.00376822241676, 60.18596833797931)

                val expectedGeometry: LineString<G2D> =
                    mkLineString(positionSequenceBuilder.toPositionSequence(), WGS84)

                val actualGeometry: LineString<G2D> = resp.routes.first().geometry

                assertThat(actualGeometry).isEqualTo(expectedGeometry)
            }
        }

        @Test
        fun shouldReturnExpectedInfrastructureLinksWithTraversalDirections() {
            matchRouteAndCheckAssertionsOnSuccessResponse(createSourceGeometry(),
                                                          createSourceRoutePoints(),
                                                          VehicleType.GENERIC_BUS,
                                                          DEFAULT_MATCHING_PARAMS) { resp ->

                val actualLinkIdsAndForwardTraversals: List<Pair<String, Boolean>> = resp
                    .routes.first()
                    .paths.map { traversal: LinkTraversalDTO ->
                        traversal.externalLinkRef.externalLinkId to traversal.isTraversalForwards
                    }

                assertThat(actualLinkIdsAndForwardTraversals).isEqualTo(listOf(
                    "440250" to false,
                    "440764" to false,
                    "440767" to false,
                    "440765" to false,
                    "440750" to true
                ))
            }
        }
    }

    companion object {

        private val DEFAULT_MATCHING_PARAMS = PublicTransportRouteMatchingParameters(
            bufferRadiusInMeters = 55.0,
            terminusLinkQueryDistance = 50.0,
            terminusLinkQueryLimit = 5,
            maxStopLocationDeviation = 80.0,
            roadJunctionMatching = JunctionMatchingParameters(
                junctionNodeMatchDistance = 5.0,
                junctionNodeClearingDistance = 30.0
            )
        )
    }
}
