package fi.hsl.jore4.mapmatching.service.routing

import fi.hsl.jore4.mapmatching.model.VehicleType
import fi.hsl.jore4.mapmatching.service.common.response.LinkTraversalDTO
import fi.hsl.jore4.mapmatching.service.common.response.ResponseCode
import fi.hsl.jore4.mapmatching.service.common.response.RoutingResponse
import fi.hsl.jore4.mapmatching.test.IntTest
import fi.hsl.jore4.mapmatching.test.IntegrationTest
import fi.hsl.jore4.mapmatching.util.GeolatteUtils.roundCoordinates
import fi.hsl.jore4.mapmatching.util.GeolatteUtils.toPoint
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.fail
import org.geolatte.geom.G2D
import org.geolatte.geom.Geometries.mkLineString
import org.geolatte.geom.LineString
import org.geolatte.geom.Point
import org.geolatte.geom.PositionSequenceBuilders
import org.geolatte.geom.builder.DSL.g
import org.geolatte.geom.crs.CoordinateReferenceSystems.WGS84
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired

@IntTest
@Suppress("ClassName")
class RoutingService_FindRouteTest
    @Autowired
    constructor(
        val routingService: IRoutingService
    ) : IntegrationTest() {
        private fun findRoute(
            routeViaPoints: List<Point<G2D>>,
            vehicleType: VehicleType = VehicleType.GENERIC_BUS,
            routingParams: RoutingExtraParameters = DEFAULT_ROUTING_EXTRA_PARAMETERS
        ): RoutingResponse = routingService.findRoute(routeViaPoints, vehicleType, routingParams)

        private fun findRouteAndCheckAssertionsOnSuccessResponse(
            routeViaPoints: List<Point<G2D>>,
            vehicleType: VehicleType = VehicleType.GENERIC_BUS,
            routingParams: RoutingExtraParameters = DEFAULT_ROUTING_EXTRA_PARAMETERS,
            checkAssertions: (
                response: RoutingResponse.RoutingSuccessDTO
            ) -> Unit
        ) {
            when (val response: RoutingResponse = findRoute(routeViaPoints, vehicleType, routingParams)) {
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

        private fun findRouteAndCheckAssertionsOnFailureResponse(
            routeViaPoints: List<Point<G2D>>,
            vehicleType: VehicleType = VehicleType.GENERIC_BUS,
            routingParams: RoutingExtraParameters = DEFAULT_ROUTING_EXTRA_PARAMETERS,
            checkAssertions: (
                response: RoutingResponse.RoutingFailureDTO
            ) -> Unit
        ) {
            when (val response: RoutingResponse = findRoute(routeViaPoints, vehicleType, routingParams)) {
                is RoutingResponse.RoutingFailureDTO -> {
                    // Verify common assertion(s).
                    assertThat(response.code).isNotEqualTo(ResponseCode.Ok)

                    // Check custom assertions provided within block parameter.
                    checkAssertions(response)
                }

                else -> fail<Void>("RoutingResponse is not a failure response as expected: $response")
            }
        }

        @Nested
        @DisplayName("When not given two or more distinct route points")
        inner class WhenNotGivenTwoOrMoreDistinctRoutePoints {
            private val sampleRoutePoint: Point<G2D> = toPoint(g(24.95707730, 60.16800990))

            private fun testWithPoints(routeViaPoints: List<Point<G2D>>) {
                findRouteAndCheckAssertionsOnFailureResponse(routeViaPoints) { resp ->
                    assertThat(resp.code).isEqualTo(ResponseCode.InvalidValue)
                    assertThat(resp.message).isEqualTo("At least 2 distinct points must be given")
                }
            }

            @Test
            @DisplayName("When no route points given")
            fun whenNoRoutePointsGiven() {
                testWithPoints(listOf())
            }

            @Test
            @DisplayName("When only single route point given")
            fun whenOnlySingleRoutePointGiven() {
                testWithPoints(listOf(sampleRoutePoint))
            }

            @Test
            @DisplayName("With single route point duplicated")
            fun withSingleRoutePointDuplicated() {
                testWithPoints(listOf(sampleRoutePoint, sampleRoutePoint, sampleRoutePoint))
            }
        }

        @Nested
        @DisplayName("When not given two or more distinct route points")
        inner class WhenNoInfrastructureLinkFoundForSomeGivenRoutePoint {
            private fun testWithPoints(routeViaPoints: List<Point<G2D>>) {
                findRouteAndCheckAssertionsOnFailureResponse(routeViaPoints) { resp ->
                    assertThat(resp.code).isEqualTo(ResponseCode.NoSegment)
                    assertThat(resp.message).startsWith("Could not match infrastructure link for following points:")
                }
            }

            @Test
            @DisplayName("When infrastructure link not found for any given route point")
            fun whenInfraLinkNotFoundForAnyGivenRoutePoint() {
                testWithPoints(
                    listOf(
                        toPoint(g(1.0, 2.0)),
                        toPoint(g(1.1, 2.2))
                    )
                )
            }

            @Test
            @DisplayName("When infrastructure link found for some but not all given route points")
            fun whenInfraLinkFoundForSomeButNotAllGivenRoutePoints() {
                testWithPoints(
                    listOf(
                        toPoint(g(1.0, 2.0)),
                        toPoint(g(24.95707730, 60.16800990))
                    )
                )
            }
        }

        @Nested
        @DisplayName("When result route via request points span multiple infrastructure links")
        inner class WhenResultRouteViaRequestPointsSpanMultipleInfrastructureLinks {
            private val requestRoutePoints: List<Point<G2D>> =
                listOf(
                    toPoint(g(24.95708, 60.16801)),
                    toPoint(g(24.95851, 60.16911))
                )

            @Test
            fun shouldReturnExpectedGeometry() {
                findRouteAndCheckAssertionsOnSuccessResponse(requestRoutePoints) { resp ->

                    val expectedCoordinates =
                        PositionSequenceBuilders
                            .variableSized(G2D::class.java)
                            .add(24.95708, 60.16801)
                            .add(24.95709, 60.16801)
                            .add(24.95712, 60.16801)
                            .add(24.95715, 60.16802)
                            .add(24.95718, 60.16804)
                            .add(24.95724, 60.16806)
                            .add(24.95724, 60.16806)
                            .add(24.95732, 60.16812)
                            .add(24.95734, 60.16814)
                            .add(24.95758, 60.16832)
                            .add(24.95776, 60.16843)
                            .add(24.95795, 60.16856)
                            .add(24.95797, 60.16858)
                            .add(24.95821, 60.16873)
                            .add(24.95826, 60.16876)
                            .add(24.95826, 60.16876)
                            .add(24.95836, 60.16882)
                            .add(24.95838, 60.16883)
                            .add(24.95839, 60.16885)
                            .add(24.95841, 60.16886)
                            .add(24.95843, 60.16888)
                            .add(24.95845, 60.16889)
                            .add(24.95847, 60.16891)
                            .add(24.95848, 60.16892)
                            .add(24.95849, 60.16894)
                            .add(24.95850, 60.16896)
                            .add(24.95850, 60.16898)
                            .add(24.95851, 60.16901)
                            .add(24.95851, 60.16905)
                            .add(24.95851, 60.16906)
                            .add(24.95851, 60.16911)

                    val expectedGeometry: LineString<G2D> =
                        mkLineString(expectedCoordinates.toPositionSequence(), WGS84)

                    val actualGeometry: LineString<G2D> = resp.routes.first().geometry

                    assertThat(roundCoordinates(actualGeometry, 5)).isEqualTo(expectedGeometry)
                }
            }

            @Test
            fun shouldReturnExpectedInfrastructureLinksWithTraversalDirections() {
                findRouteAndCheckAssertionsOnSuccessResponse(requestRoutePoints) { resp ->

                    val actualLinkIdsAndForwardTraversals: List<Pair<String, Boolean>> =
                        getExternalLinkIdsAndTraversalDirections(resp)

                    assertThat(actualLinkIdsAndForwardTraversals).isEqualTo(
                        listOf(
                            "d561a40e-1b2b-45aa-bb91-fcb5ba82e136:1" to true,
                            "3113baf5-2120-45d3-8f16-0d94e63644fd:2" to true,
                            "9253d310-7825-4323-84ee-1575fa382800:1" to true
                        )
                    )
                }
            }
        }

        @Nested
        @DisplayName("When request route points coincide single one-way link in the forwards direction")
        inner class WhenRequestRoutePointsCoincideSingleOneWayLinkInForwardsDirection {
            private val requestRoutePoints: List<Point<G2D>> =
                listOf(
                    toPoint(g(24.95735, 60.16813)),
                    toPoint(g(24.95762, 60.16834))
                )

            @Test
            fun shouldReturnExpectedGeometry() {
                findRouteAndCheckAssertionsOnSuccessResponse(requestRoutePoints) { resp ->

                    val expectedCoordinates =
                        PositionSequenceBuilders
                            .variableSized(G2D::class.java)
                            .add(24.95734, 60.16813)
                            .add(24.95734, 60.16814)
                            .add(24.95758, 60.16832)
                            .add(24.95762, 60.16834)

                    val expectedGeometry: LineString<G2D> =
                        mkLineString(expectedCoordinates.toPositionSequence(), WGS84)

                    val actualGeometry: LineString<G2D> = resp.routes.first().geometry

                    assertThat(roundCoordinates(actualGeometry, 5)).isEqualTo(expectedGeometry)
                }
            }

            @Test
            fun shouldReturnExpectedInfrastructureLinksWithTraversalDirections() {
                findRouteAndCheckAssertionsOnSuccessResponse(requestRoutePoints) { resp ->

                    val actualLinkIdsAndForwardTraversals: List<Pair<String, Boolean>> =
                        getExternalLinkIdsAndTraversalDirections(resp)

                    assertThat(actualLinkIdsAndForwardTraversals).isEqualTo(
                        listOf(
                            "3113baf5-2120-45d3-8f16-0d94e63644fd:2" to true
                        )
                    )
                }
            }
        }

        @Nested
        @DisplayName("When request route points coincide single one-way link in the backwards direction")
        inner class WhenRequestRoutePointsCoincideSingleOneWayLinkInBackwardsDirection {
            private val requestRoutePoints: List<Point<G2D>> =
                listOf(
                    toPoint(g(24.95762, 60.16834)),
                    toPoint(g(24.95735, 60.16813))
                )

            @Test
            fun shouldReturnExpectedGeometry() {
                findRouteAndCheckAssertionsOnSuccessResponse(requestRoutePoints) { resp ->

                    val expectedCoordinates =
                        PositionSequenceBuilders
                            .variableSized(G2D::class.java)
                            .add(24.95762, 60.16834)
                            .add(24.95776, 60.16843)
                            .add(24.95763, 60.16846)
                            .add(24.95748, 60.16836)
                            .add(24.95746, 60.16834)
                            .add(24.95734, 60.16825)
                            .add(24.95734, 60.16825)
                            .add(24.95726, 60.16819)
                            .add(24.95724, 60.16817)
                            .add(24.95721, 60.16816)
                            .add(24.95719, 60.16814)
                            .add(24.95718, 60.16814)
                            .add(24.95715, 60.16812)
                            .add(24.95712, 60.16811)
                            .add(24.95711, 60.16811)
                            .add(24.95724, 60.16806)
                            .add(24.95724, 60.16806)
                            .add(24.95732, 60.16812)
                            .add(24.95734, 60.16813)

                    val expectedGeometry: LineString<G2D> =
                        mkLineString(expectedCoordinates.toPositionSequence(), WGS84)

                    val actualGeometry: LineString<G2D> = resp.routes.first().geometry

                    assertThat(roundCoordinates(actualGeometry, 5)).isEqualTo(expectedGeometry)
                }
            }

            @Test
            fun shouldReturnExpectedInfrastructureLinksWithTraversalDirections() {
                findRouteAndCheckAssertionsOnSuccessResponse(requestRoutePoints) { resp ->

                    val actualLinkIdsAndForwardTraversals: List<Pair<String, Boolean>> =
                        getExternalLinkIdsAndTraversalDirections(resp)

                    assertThat(actualLinkIdsAndForwardTraversals).isEqualTo(
                        listOf(
                            "3113baf5-2120-45d3-8f16-0d94e63644fd:2" to true,
                            "134baafa-fbc0-47e7-8be1-6f25cbd37eff:2" to true,
                            "e8ad869e-5b60-43d1-b30d-345fd4c5ae92:1" to false,
                            "bd198e25-1902-44fd-ac89-da19c5115eee:2" to false,
                            "3113baf5-2120-45d3-8f16-0d94e63644fd:2" to true
                        )
                    )
                }
            }
        }

        @Nested
        @DisplayName("When traversing back and forth a bi-directional link")
        inner class WhenTraversingBackAndForthABirectionalLink {
            private val requestRoutePoints: List<Point<G2D>> =
                listOf(
                    toPoint(g(24.98954, 60.27721)),
                    toPoint(g(24.98891, 60.27711)),
                    toPoint(g(24.98803, 60.27699)),
                    toPoint(g(24.99012, 60.27728))
                )

            @Test
            fun shouldReturnExpectedGeometry() {
                findRouteAndCheckAssertionsOnSuccessResponse(requestRoutePoints) { resp ->

                    val expectedCoordinates =
                        PositionSequenceBuilders
                            .variableSized(G2D::class.java)
                            .add(24.98955, 60.2772)
                            .add(24.98884, 60.27709)
                            .add(24.98804, 60.27698)
                            .add(24.98884, 60.27709)
                            .add(24.98964, 60.27721)
                            .add(24.98979, 60.27724)
                            .add(24.98988, 60.27725)
                            .add(24.99012, 60.27728)

                    val expectedGeometry: LineString<G2D> =
                        mkLineString(expectedCoordinates.toPositionSequence(), WGS84)

                    val actualGeometry: LineString<G2D> = resp.routes.first().geometry

                    assertThat(roundCoordinates(actualGeometry, 5)).isEqualTo(expectedGeometry)
                }
            }

            @Test
            fun shouldReturnExpectedInfrastructureLinksWithTraversalDirections() {
                findRouteAndCheckAssertionsOnSuccessResponse(requestRoutePoints) { resp ->

                    val actualLinkIdsAndForwardTraversals: List<Pair<String, Boolean>> =
                        getExternalLinkIdsAndTraversalDirections(resp)

                    assertThat(actualLinkIdsAndForwardTraversals).isEqualTo(
                        listOf(
                            "5e070b32-d8f9-4096-8957-7303f8affe8b:3" to false,
                            "5e070b32-d8f9-4096-8957-7303f8affe8b:3" to true,
                            "28c73f2e-0d5e-40e1-9756-5171bb65f39d:3" to true,
                            "9e19e8ac-55dc-4cfb-b262-9206a957d084:3" to true
                        )
                    )
                }
            }
        }

        @Nested
        @DisplayName("When all given points coincide with infrastructure network nodes")
        inner class WhenAllGivenPointsCoincideWithInfrastructureNetworkNodes {
            private val requestRoutePoints: List<Point<G2D>> =
                listOf(
                    toPoint(g(24.95724, 60.16806)),
                    toPoint(g(24.95776, 60.16843))
                )

            @Test
            fun shouldReturnExpectedGeometry() {
                findRouteAndCheckAssertionsOnSuccessResponse(requestRoutePoints) { resp ->

                    val expectedCoordinates =
                        PositionSequenceBuilders
                            .variableSized(G2D::class.java)
                            .add(24.95724, 60.16806)
                            .add(24.95724, 60.16806)
                            .add(24.95732, 60.16812)
                            .add(24.95734, 60.16814)
                            .add(24.95758, 60.16832)
                            .add(24.95776, 60.16843)

                    val expectedGeometry: LineString<G2D> =
                        mkLineString(expectedCoordinates.toPositionSequence(), WGS84)

                    val actualGeometry: LineString<G2D> = resp.routes.first().geometry

                    assertThat(roundCoordinates(actualGeometry, 5)).isEqualTo(expectedGeometry)
                }
            }

            @Test
            fun shouldReturnExpectedInfrastructureLinksWithTraversalDirections() {
                findRouteAndCheckAssertionsOnSuccessResponse(requestRoutePoints) { resp ->

                    val actualLinkIdsAndForwardTraversals: List<Pair<String, Boolean>> =
                        getExternalLinkIdsAndTraversalDirections(resp)

                    assertThat(actualLinkIdsAndForwardTraversals).isEqualTo(
                        listOf(
                            "3113baf5-2120-45d3-8f16-0d94e63644fd:2" to true
                        )
                    )
                }
            }
        }

        @Nested
        @DisplayName("When traversing closed-loop links")
        inner class WhenTraversingClosedLoopLinks {
            @Nested
            @DisplayName("When traversing back and forth a bi-directional closed-loop link")
            inner class WhenTraversingBackAndForthABirectionalClosedLoopLink {
                private val requestRoutePoints: List<Point<G2D>> =
                    listOf(
                        toPoint(g(24.56306, 60.16016)),
                        toPoint(g(24.56333, 60.16021)),
                        toPoint(g(24.56451, 60.16029)),
                        toPoint(g(24.56216, 60.16041)),
                        toPoint(g(24.56220, 60.16039))
                    )

                @Nested
                @DisplayName("Without simplifying consecutive closed loop traversals")
                inner class WithoutSimplifyingConsecutiveClosedLoopTraversals {
                    @Test
                    fun shouldReturnExpectedGeometry() {
                        findRouteAndCheckAssertionsOnSuccessResponse(
                            routeViaPoints = requestRoutePoints,
                            routingParams = DEFAULT_ROUTING_EXTRA_PARAMETERS
                        ) { resp ->

                            val expectedCoordinates =
                                PositionSequenceBuilders
                                    .variableSized(G2D::class.java)
                                    .add(24.56305, 60.16016)
                                    .add(24.56307, 60.16021)
                                    .add(24.56354, 60.16019)
                                    .add(24.56354, 60.16019)
                                    .add(24.56383, 60.16018)
                                    .add(24.56390, 60.16018)
                                    .add(24.56391, 60.16018)
                                    .add(24.56398, 60.16018)
                                    .add(24.56406, 60.16019)
                                    .add(24.56409, 60.16019)
                                    .add(24.56413, 60.16019)
                                    .add(24.56416, 60.16020)
                                    .add(24.56420, 60.16020)
                                    .add(24.56423, 60.16021)
                                    .add(24.56427, 60.16022)
                                    .add(24.56430, 60.16022)
                                    .add(24.56433, 60.16023)
                                    .add(24.56436, 60.16024)
                                    .add(24.56439, 60.16025)
                                    .add(24.56442, 60.16026)
                                    .add(24.56445, 60.16028)
                                    .add(24.56447, 60.16029)
                                    .add(24.56449, 60.16030)
                                    .add(24.56447, 60.16029)
                                    .add(24.56445, 60.16028)
                                    .add(24.56442, 60.16026)
                                    .add(24.56439, 60.16025)
                                    .add(24.56436, 60.16024)
                                    .add(24.56433, 60.16023)
                                    .add(24.56430, 60.16022)
                                    .add(24.56427, 60.16022)
                                    .add(24.56423, 60.16021)
                                    .add(24.56420, 60.16020)
                                    .add(24.56416, 60.16020)
                                    .add(24.56413, 60.16019)
                                    .add(24.56409, 60.16019)
                                    .add(24.56406, 60.16019)
                                    .add(24.56398, 60.16018)
                                    .add(24.56391, 60.16018)
                                    .add(24.56390, 60.16018)
                                    .add(24.56383, 60.16018)
                                    .add(24.56354, 60.16019)
                                    .add(24.56354, 60.16019)
                                    .add(24.56307, 60.16021)
                                    .add(24.56305, 60.16021)
                                    .add(24.56291, 60.16022)
                                    .add(24.56276, 60.16023)
                                    .add(24.56273, 60.16024)
                                    .add(24.56269, 60.16024)
                                    .add(24.56268, 60.16024)
                                    .add(24.56264, 60.16025)
                                    .add(24.56261, 60.16025)
                                    .add(24.56257, 60.16026)
                                    .add(24.56254, 60.16027)
                                    .add(24.56251, 60.16028)
                                    .add(24.56248, 60.16028)
                                    .add(24.56245, 60.16029)
                                    .add(24.56242, 60.16030)
                                    .add(24.56239, 60.16032)
                                    .add(24.56223, 60.16039)
                                    .add(24.56221, 60.16039)
                                    .add(24.56216, 60.16041)
                                    .add(24.56221, 60.16039)

                            val expectedGeometry: LineString<G2D> =
                                mkLineString(expectedCoordinates.toPositionSequence(), WGS84)

                            val actualGeometry: LineString<G2D> = resp.routes.first().geometry

                            assertThat(roundCoordinates(actualGeometry, 5)).isEqualTo(expectedGeometry)
                        }
                    }

                    @Test
                    fun shouldReturnExpectedInfrastructureLinksWithTraversalDirections() {
                        findRouteAndCheckAssertionsOnSuccessResponse(
                            routeViaPoints = requestRoutePoints,
                            routingParams = DEFAULT_ROUTING_EXTRA_PARAMETERS
                        ) { resp ->

                            val actualLinkIdsAndForwardTraversals: List<Pair<String, Boolean>> =
                                getExternalLinkIdsAndTraversalDirections(resp)

                            assertThat(actualLinkIdsAndForwardTraversals).isEqualTo(
                                listOf(
                                    "4f8aa489-14dd-4061-b197-41db30fc3e98:2" to true,
                                    "4979ed8b-a5c4-4c38-87e4-3243fa994b80:1" to false,
                                    "4979ed8b-a5c4-4c38-87e4-3243fa994b80:1" to true,
                                    "4979ed8b-a5c4-4c38-87e4-3243fa994b80:1" to true,
                                    "4979ed8b-a5c4-4c38-87e4-3243fa994b80:1" to false
                                )
                            )
                        }
                    }
                }

                @Nested
                @DisplayName("When closed loop traversals are simplified")
                inner class WhenClosedLoopTraversalsAreSimplified {
                    private val routingExtraParams =
                        RoutingExtraParameters(
                            linkQueryDistance = 50,
                            simplifyConsecutiveClosedLoopTraversals = true
                        )

                    @Test
                    fun shouldReturnExpectedInfrastructureLinksWithTraversalDirections() {
                        findRouteAndCheckAssertionsOnSuccessResponse(
                            routeViaPoints = requestRoutePoints,
                            routingParams = routingExtraParams
                        ) { resp ->

                            val actualLinkIdsAndForwardTraversals: List<Pair<String, Boolean>> =
                                getExternalLinkIdsAndTraversalDirections(resp)

                            // shorter list than without simplifying, the closed-loop link appears only once
                            assertThat(actualLinkIdsAndForwardTraversals).isEqualTo(
                                listOf(
                                    "4f8aa489-14dd-4061-b197-41db30fc3e98:2" to true,
                                    "4979ed8b-a5c4-4c38-87e4-3243fa994b80:1" to false
                                )
                            )
                        }
                    }
                }
            }
        }

        companion object {
            private val DEFAULT_ROUTING_EXTRA_PARAMETERS =
                RoutingExtraParameters(
                    linkQueryDistance = 50,
                    simplifyConsecutiveClosedLoopTraversals = false
                )

            private fun getExternalLinkIdsAndTraversalDirections(
                response: RoutingResponse.RoutingSuccessDTO
            ): List<Pair<String, Boolean>> =
                response
                    .routes
                    .first()
                    .paths
                    .map { traversal: LinkTraversalDTO ->
                        traversal.externalLinkRef.externalLinkId to traversal.isTraversalForwards
                    }
        }
    }
