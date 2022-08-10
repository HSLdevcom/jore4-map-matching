package fi.hsl.jore4.mapmatching.service.routing

import fi.hsl.jore4.mapmatching.model.VehicleType
import fi.hsl.jore4.mapmatching.service.common.response.LinkTraversalDTO
import fi.hsl.jore4.mapmatching.service.common.response.ResponseCode
import fi.hsl.jore4.mapmatching.service.common.response.RoutingResponse
import fi.hsl.jore4.mapmatching.test.IntTest
import fi.hsl.jore4.mapmatching.test.IntegrationTest
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
class RoutingService_FindRouteTest @Autowired constructor(val routingService: IRoutingService) : IntegrationTest() {

    private fun findRoute(routeViaPoints: List<Point<G2D>>,
                          vehicleType: VehicleType = VehicleType.GENERIC_BUS,
                          linkQueryDistance: Int = 50)
        : RoutingResponse = routingService.findRoute(routeViaPoints,
                                                     vehicleType,
                                                     linkQueryDistance)

    private fun findRouteAndCheckAssertionsOnSuccessResponse(routeViaPoints: List<Point<G2D>>,
                                                             vehicleType: VehicleType = VehicleType.GENERIC_BUS,
                                                             linkQueryDistance: Int = 50,
                                                             checkAssertions: (response: RoutingResponse.RoutingSuccessDTO) -> Unit) {

        when (val response: RoutingResponse = findRoute(routeViaPoints, vehicleType, linkQueryDistance)) {
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

    private fun findRouteAndCheckAssertionsOnFailureResponse(routeViaPoints: List<Point<G2D>>,
                                                             vehicleType: VehicleType = VehicleType.GENERIC_BUS,
                                                             linkQueryDistance: Int = 50,
                                                             checkAssertions: (response: RoutingResponse.RoutingFailureDTO) -> Unit) {

        when (val response: RoutingResponse = findRoute(routeViaPoints, vehicleType, linkQueryDistance)) {
            is RoutingResponse.RoutingFailureDTO -> {
                // Verify common assertion(s).
                assertThat(response.code).isNotEqualTo(ResponseCode.Ok)

                // Check custom assertions provided within block parameter.
                checkAssertions(response)
            }

            else -> fail<Void>("RoutingResponse is not a failure response as expected")
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
            testWithPoints(listOf(toPoint(g(1.0, 2.0)),
                                  toPoint(g(1.1, 2.2))))
        }

        @Test
        @DisplayName("When infrastructure link found for some but not all given route points")
        fun whenInfraLinkFoundForSomeButNotAllGivenRoutePoints() {
            testWithPoints(listOf(toPoint(g(1.0, 2.0)),
                                  toPoint(g(24.95707730, 60.16800990))))
        }
    }

    @Nested
    @DisplayName("When result route via request points span multiple infrastructure links")
    inner class WhenResultRouteViaRequestPointsSpanMultipleInfrastructureLinks {

        private val requestRoutePoints: List<Point<G2D>> = listOf(toPoint(g(24.95707730, 60.16800990)),
                                                                  toPoint(g(24.95851405, 60.16911157)))

        @Test
        fun shouldReturnExpectedGeometry() {
            findRouteAndCheckAssertionsOnSuccessResponse(requestRoutePoints) { resp ->

                val positionSequenceBuilder = PositionSequenceBuilders.variableSized(G2D::class.java)
                    .add(24.957078115997536, 60.168008946494965)
                    .add(24.95715147812108, 60.16802453476507)
                    .add(24.95723875994145, 60.168060496789664)
                    .add(24.957323462359152, 60.168123777899666)
                    .add(24.95758037487319, 60.16831753532955)
                    .add(24.957757980575668, 60.168433705609274)
                    .add(24.957965939306, 60.168577522712035)
                    .add(24.958263473250017, 60.16876486296617)
                    .add(24.958394690444972, 60.168845216182085)
                    .add(24.95847818526909, 60.16892453755149)
                    .add(24.958509823912653, 60.16906042733862)
                    .add(24.958509747716203, 60.16911156840859)

                val expectedGeometry: LineString<G2D> =
                    mkLineString(positionSequenceBuilder.toPositionSequence(), WGS84)

                val actualGeometry: LineString<G2D> = resp.routes.first().geometry

                assertThat(actualGeometry).isEqualTo(expectedGeometry)
            }
        }

        @Test
        fun shouldReturnExpectedInfrastructureLinksWithTraversalDirections() {
            findRouteAndCheckAssertionsOnSuccessResponse(requestRoutePoints) { resp ->

                val actualLinkIdsAndForwardTraversals: List<Pair<String, Boolean>> = resp
                    .routes.first()
                    .paths.map { traversal: LinkTraversalDTO ->
                        traversal.externalLinkRef.externalLinkId to traversal.isTraversalForwards
                    }

                assertThat(actualLinkIdsAndForwardTraversals).isEqualTo(listOf(
                    "441679" to true,
                    "441872" to true,
                    "441874" to true
                ))
            }
        }
    }

    @Nested
    @DisplayName("When request route points coincide single one-way link in the forward direction")
    inner class WhenRequestRoutePointsCoincideSingleOneWayLinkInForwardDirection {

        private val requestRoutePoints: List<Point<G2D>> = listOf(toPoint(g(24.95734619, 60.16813263)),
                                                                  toPoint(g(24.95762143, 60.16834154)))

        @Test
        fun shouldReturnExpectedGeometry() {
            findRouteAndCheckAssertionsOnSuccessResponse(requestRoutePoints) { resp ->

                val positionSequenceBuilder = PositionSequenceBuilders.variableSized(G2D::class.java)
                    .add(24.957338539378313, 60.16813514871233)
                    .add(24.95758037487319, 60.16831753532955)
                    .add(24.957618673627945, 60.168342586286755)

                val expectedGeometry: LineString<G2D> =
                    mkLineString(positionSequenceBuilder.toPositionSequence(), WGS84)

                val actualGeometry: LineString<G2D> = resp.routes.first().geometry

                assertThat(actualGeometry).isEqualTo(expectedGeometry)
            }
        }

        @Test
        fun shouldReturnExpectedInfrastructureLinksWithTraversalDirections() {
            findRouteAndCheckAssertionsOnSuccessResponse(requestRoutePoints) { resp ->

                val actualLinkIdsAndForwardTraversals: List<Pair<String, Boolean>> = resp
                    .routes.first()
                    .paths.map { traversal: LinkTraversalDTO ->
                        traversal.externalLinkRef.externalLinkId to traversal.isTraversalForwards
                    }

                assertThat(actualLinkIdsAndForwardTraversals).isEqualTo(listOf(
                    "441872" to true
                ))
            }
        }
    }

    @Nested
    @DisplayName("When request route points coincide single one-way link in the backward direction")
    inner class WhenRequestRoutePointsCoincideSingleOneWayLinkInBackwardDirection {

        private val requestRoutePoints: List<Point<G2D>> = listOf(toPoint(g(24.95762143, 60.16834154)),
                                                                  toPoint(g(24.95734619, 60.16813263)))

        @Test
        fun shouldReturnExpectedGeometry() {
            findRouteAndCheckAssertionsOnSuccessResponse(requestRoutePoints) { resp ->

                val positionSequenceBuilder = PositionSequenceBuilders.variableSized(G2D::class.java)
                    .add(24.957618673627945, 60.168342586286755)
                    .add(24.957757980575668, 60.168433705609274)
                    .add(24.957632827286027, 60.1684575186299)
                    .add(24.957461812613946, 60.168342617436046)
                    .add(24.957336775866754, 60.16824669407173)
                    .add(24.957175919885138, 60.16813717657471)
                    .add(24.957109340744697, 60.16810512648815)
                    .add(24.95723875994145, 60.168060496789664)
                    .add(24.957323462359152, 60.168123777899666)
                    .add(24.957338539378313, 60.16813514871233)

                val expectedGeometry: LineString<G2D> =
                    mkLineString(positionSequenceBuilder.toPositionSequence(), WGS84)

                val actualGeometry: LineString<G2D> = resp.routes.first().geometry

                assertThat(actualGeometry).isEqualTo(expectedGeometry)
            }
        }

        @Test
        fun shouldReturnExpectedInfrastructureLinksWithTraversalDirections() {
            findRouteAndCheckAssertionsOnSuccessResponse(requestRoutePoints) { resp ->

                val actualLinkIdsAndForwardTraversals: List<Pair<String, Boolean>> = resp
                    .routes.first()
                    .paths.map { traversal: LinkTraversalDTO ->
                        traversal.externalLinkRef.externalLinkId to traversal.isTraversalForwards
                    }

                assertThat(actualLinkIdsAndForwardTraversals).isEqualTo(listOf(
                    "441872" to true,
                    "441880" to true,
                    "441870" to false,
                    "441890" to false,
                    "441872" to true,
                ))
            }
        }
    }
}
