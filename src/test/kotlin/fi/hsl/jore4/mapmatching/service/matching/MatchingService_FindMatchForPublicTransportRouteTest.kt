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
import fi.hsl.jore4.mapmatching.util.GeolatteUtils.roundCoordinates
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
class MatchingService_FindMatchForPublicTransportRouteTest
    @Autowired
    constructor(
        val matchingService: IMatchingService
    ) : IntegrationTest() {
        private fun matchPublicTransportRoute(
            routeGeometry: LineString<G2D>,
            routePoints: List<RoutePoint>,
            vehicleType: VehicleType,
            matchingParameters: PublicTransportRouteMatchingParameters = DEFAULT_MATCHING_PARAMS
        ): RoutingResponse =
            matchingService.findMatchForPublicTransportRoute(
                "integration test", // route ID
                routeGeometry,
                routePoints,
                vehicleType,
                matchingParameters
            )

        private fun matchRouteAndCheckAssertionsOnSuccessResponse(
            routeGeometry: LineString<G2D>,
            routePoints: List<RoutePoint>,
            vehicleType: VehicleType,
            matchingParameters: PublicTransportRouteMatchingParameters = DEFAULT_MATCHING_PARAMS,
            checkAssertions: (response: RoutingResponse.RoutingSuccessDTO) -> Unit
        ) {
            val response: RoutingResponse =
                matchPublicTransportRoute(
                    routeGeometry,
                    routePoints,
                    vehicleType,
                    matchingParameters
                )

            when (response) {
                is RoutingResponse.RoutingSuccessDTO -> {
                    // Verify common assertions.
                    assertThat(response.code).isEqualTo(ResponseCode.Ok)
                    assertThat(response.routes).hasSize(1)

                    // Check custom assertions provided within block parameter.
                    checkAssertions(response)
                }

                else -> fail<Void>("RoutingResponse is not a success response as expected: $response")
            }
        }

        @Nested
        @DisplayName("With smoke test")
        inner class WithSmokeTest {
            private fun createSourceGeometry(): LineString<G2D> {
                val positionSequenceBuilder =
                    PositionSequenceBuilders
                        .variableSized(G2D::class.java)
                        .add(25.00802, 60.18773)
                        .add(25.00798, 60.18768)
                        .add(25.00720, 60.18668)
                        .add(25.00682, 60.18610)
                        .add(25.00620, 60.18592)
                        .add(25.00573, 60.18589)
                        .add(25.00417, 60.18596)
                        .add(25.00377, 60.18597)

                return mkLineString(positionSequenceBuilder.toPositionSequence(), WGS84)
            }

            private fun createSourceRoutePoints(): List<RoutePoint> =
                listOf(
                    RouteStopPoint(
                        location = toPoint(g(25.00800, 60.18774)),
                        projectedLocation = toPoint(g(25.00802, 60.18773)),
                        nationalId = 240525,
                        passengerId = "H4026"
                    ),
                    RouteJunctionPoint(location = toPoint(g(25.00682, 60.18610))),
                    RouteStopPoint(
                        location = toPoint(g(25.00323, 60.18605)),
                        projectedLocation = toPoint(g(25.00377, 60.18597)),
                        nationalId = 240681,
                        passengerId = "H4034"
                    )
                )

            @Test
            fun shouldReturnExpectedGeometry() {
                matchRouteAndCheckAssertionsOnSuccessResponse(
                    createSourceGeometry(),
                    createSourceRoutePoints(),
                    VehicleType.GENERIC_BUS,
                    DEFAULT_MATCHING_PARAMS
                ) { resp ->

                    val positionSequenceBuilder =
                        PositionSequenceBuilders
                            .variableSized(G2D::class.java)
                            .add(25.00802, 60.18773)
                            .add(25.00797, 60.18768)
                            .add(25.00721, 60.18667)
                            .add(25.00692, 60.18627)
                            .add(25.00684, 60.18611)
                            .add(25.00652, 60.18600)
                            .add(25.00615, 60.18592)
                            .add(25.00581, 60.18589)
                            .add(25.00542, 60.18590)
                            .add(25.00491, 60.18592)
                            .add(25.00430, 60.18595)
                            .add(25.00377, 60.18597)

                    val expectedGeometry: LineString<G2D> =
                        mkLineString(positionSequenceBuilder.toPositionSequence(), WGS84)

                    val actualGeometry: LineString<G2D> = resp.routes.first().geometry

                    assertThat(roundCoordinates(actualGeometry, 5)).isEqualTo(expectedGeometry)
                }
            }

            @Test
            fun shouldReturnExpectedInfrastructureLinksWithTraversalDirections() {
                matchRouteAndCheckAssertionsOnSuccessResponse(
                    createSourceGeometry(),
                    createSourceRoutePoints(),
                    VehicleType.GENERIC_BUS,
                    DEFAULT_MATCHING_PARAMS
                ) { resp ->

                    val actualLinkIdsAndForwardTraversals: List<Pair<String, Boolean>> =
                        resp
                            .routes
                            .first()
                            .paths
                            .map { traversal: LinkTraversalDTO ->
                                traversal.externalLinkRef.externalLinkId to traversal.isTraversalForwards
                            }

                    assertThat(actualLinkIdsAndForwardTraversals).isEqualTo(
                        listOf(
                            "90c18933-06f4-4f7b-9df2-9c82b59ecd35:2" to false,
                            "315a1fa3-fdfa-4340-b581-2a83bb8abfa9:2" to false,
                            "80f7650b-e63b-4f0b-8159-b04936ce7cdb:2" to false,
                            "9fbfa4c7-0bc7-490c-99a2-c8524225ba40:2" to false,
                            "e30555b1-d6bd-43d5-ae94-fc3834d0abef:2" to true
                        )
                    )
                }
            }
        }

        companion object {
            private val DEFAULT_MATCHING_PARAMS =
                PublicTransportRouteMatchingParameters(
                    bufferRadiusInMeters = 55.0,
                    terminusLinkQueryDistance = 50.0,
                    terminusLinkQueryLimit = 5,
                    maxStopLocationDeviation = 80.0,
                    fallbackToViaNodesAlgorithm = true,
                    roadJunctionMatching =
                        JunctionMatchingParameters(
                            matchDistance = 5.0,
                            clearingDistance = 30.0
                        )
                )
        }
    }
