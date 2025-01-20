package fi.hsl.jore4.mapmatching.repository.infrastructure

import fi.hsl.jore4.mapmatching.test.generators.SnapPointLocationAlongLinkFilter.AT_END
import fi.hsl.jore4.mapmatching.test.generators.SnapPointLocationAlongLinkFilter.AT_MIDPOINT
import fi.hsl.jore4.mapmatching.test.generators.SnapPointLocationAlongLinkFilter.AT_START
import fi.hsl.jore4.mapmatching.test.generators.SnapPointLocationAlongLinkFilter.BETWEEN_ENDPOINTS_EXCLUSIVE
import fi.hsl.jore4.mapmatching.test.generators.SnapPointLocationAlongLinkFilter.CLOSE_TO_END
import fi.hsl.jore4.mapmatching.test.generators.SnapPointLocationAlongLinkFilter.CLOSE_TO_START
import fi.hsl.jore4.mapmatching.test.generators.SnapPointLocationAlongLinkFilter.NOT_AT_END
import fi.hsl.jore4.mapmatching.test.generators.SnapPointLocationAlongLinkFilter.NOT_AT_START
import fi.hsl.jore4.mapmatching.test.generators.SnappedPointOnLinkGenerator
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.quicktheories.QuickTheory.qt

@DisplayName("Test SnappedPointOnLinkTest class")
class SnappedPointOnLinkTest {
    @Nested
    @DisplayName("isSnappedToStartNode")
    inner class IsSnappedToStartNode {
        @Test
        @DisplayName("When projected closest point is within snap-to-endpoint distance to link start")
        fun whenClosestPointWithinSnapDistanceToLinkStart() {
            qt().forAll(
                SnappedPointOnLinkGenerator.snapLink(
                    hasDiscreteEndpoints = true,
                    AT_START
                )
            )
                .checkAssert { pointOnLink: SnappedPointOnLink ->

                    assertThat(pointOnLink.isSnappedToStartNode).isEqualTo(true)
                }
        }

        @Test
        @DisplayName("When projected closest point is outside of snap-to-endpoint distance to link start")
        fun whenClosestPointOutsideSnapDistanceToLinkStart() {
            qt().forAll(
                SnappedPointOnLinkGenerator.snapLink(
                    hasDiscreteEndpoints = true,
                    NOT_AT_START
                )
            )
                .checkAssert { pointOnLink: SnappedPointOnLink ->

                    assertThat(pointOnLink.isSnappedToStartNode).isEqualTo(false)
                }
        }
    }

    @Nested
    @DisplayName("isSnappedToEndNode")
    inner class IsSnappedToEndNode {
        @Test
        @DisplayName("When projected closest point is within snap-to-endpoint distance to link end")
        fun whenClosestPointWithinSnapDistanceToLinkEnd() {
            qt().forAll(
                SnappedPointOnLinkGenerator.snapLink(
                    hasDiscreteEndpoints = true,
                    AT_END
                )
            )
                .checkAssert { pointOnLink: SnappedPointOnLink ->

                    assertThat(pointOnLink.isSnappedToEndNode).isEqualTo(true)
                }
        }

        @Test
        @DisplayName("When projected closest point is outside of snap-to-endpoint distance to link end")
        fun whenClosestPointOutsideSnapDistanceToLinkEnd() {
            qt().forAll(
                SnappedPointOnLinkGenerator.snapLink(
                    hasDiscreteEndpoints = true,
                    NOT_AT_END
                )
            )
                .checkAssert { pointOnLink: SnappedPointOnLink ->

                    assertThat(pointOnLink.isSnappedToEndNode).isEqualTo(false)
                }
        }
    }

    @Nested
    @DisplayName("getSnappedNodeOrNull")
    inner class GetSnappedNodeOrNull {
        @Test
        @DisplayName("When projected closest point is within snap-to-endpoint distance to link start")
        fun whenClosestPointWithinSnapDistanceToLinkStart() {
            qt().forAll(
                SnappedPointOnLinkGenerator.snapLink(
                    hasDiscreteEndpoints = true,
                    AT_START
                )
            )
                .checkAssert { pointOnLink: SnappedPointOnLink ->

                    assertThat(pointOnLink.getSnappedNodeOrNull()).isEqualTo(pointOnLink.startNodeId)
                }
        }

        @Test
        @DisplayName("When projected closest point is within snap-to-endpoint distance to link end")
        fun whenClosestPointWithinSnapDistanceToLinkEnd() {
            qt().forAll(
                SnappedPointOnLinkGenerator.snapLink(
                    hasDiscreteEndpoints = true,
                    AT_END
                )
            )
                .checkAssert { pointOnLink: SnappedPointOnLink ->

                    assertThat(pointOnLink.getSnappedNodeOrNull()).isEqualTo(pointOnLink.endNodeId)
                }
        }

        @Test
        @DisplayName("When projected closest point is outside a snap-to-endpoint distance from link endpoints")
        fun whenClosestPointOutsideOfSnapDistanceFromLinkEndpoints() {
            qt().forAll(
                SnappedPointOnLinkGenerator.snapLink(
                    hasDiscreteEndpoints = true,
                    BETWEEN_ENDPOINTS_EXCLUSIVE
                )
            )
                .checkAssert { pointOnLink: SnappedPointOnLink ->

                    assertThat(pointOnLink.getSnappedNodeOrNull()).isNull()
                }
        }
    }

    @Nested
    @DisplayName("withSnappedToTerminusNode")
    inner class WithSnappedToTerminusNode {
        private fun divideLinkLength(
            pointOnLink: SnappedPointOnLink,
            divisor: Int
        ): Double = pointOnLink.infrastructureLinkLength / divisor

        @Test
        @DisplayName("When closest point is already projected to link start")
        fun whenClosestPointAlreadyAtLinkStart() {
            qt().forAll(
                SnappedPointOnLinkGenerator.snapLink(
                    hasDiscreteEndpoints = true,
                    AT_START
                )
            )
                .checkAssert { pointOnLink: SnappedPointOnLink ->

                    assertThat(pointOnLink.isSnappedToStartNode).isEqualTo(true)
                    assertThat(pointOnLink.isSnappedToEndNode).isEqualTo(false)

                    val pointOnLink2: SnappedPointOnLink =
                        pointOnLink.withSnappedToTerminusNode(divideLinkLength(pointOnLink, 2))

                    assertThat(pointOnLink2.isSnappedToStartNode).isEqualTo(true)
                    assertThat(pointOnLink2.isSnappedToEndNode).isEqualTo(false)
                }
        }

        @Test
        @DisplayName("When closest point is already projected to link end")
        fun whenClosestPointAlreadyAtLinkEnd() {
            qt().forAll(
                SnappedPointOnLinkGenerator.snapLink(
                    hasDiscreteEndpoints = true,
                    AT_END
                )
            )
                .checkAssert { pointOnLink: SnappedPointOnLink ->

                    assertThat(pointOnLink.isSnappedToStartNode).isEqualTo(false)
                    assertThat(pointOnLink.isSnappedToEndNode).isEqualTo(true)

                    val pointOnLink2: SnappedPointOnLink =
                        pointOnLink.withSnappedToTerminusNode(divideLinkLength(pointOnLink, 2))

                    assertThat(pointOnLink2.isSnappedToStartNode).isEqualTo(false)
                    assertThat(pointOnLink2.isSnappedToEndNode).isEqualTo(true)
                }
        }

        @Test
        @DisplayName("When closest point is not already projected to start node but expected to be snapped to it")
        fun whenClosestPointIsNotAlreadyProjectedToStartNodeButExpectedToBeSnappedToIt() {
            qt().forAll(
                SnappedPointOnLinkGenerator.snapLink(
                    hasDiscreteEndpoints = true,
                    CLOSE_TO_START
                )
            )
                .checkAssert { pointOnLink: SnappedPointOnLink ->

                    assertThat(pointOnLink.isSnappedToStartNode).isEqualTo(false)
                    assertThat(pointOnLink.isSnappedToEndNode).isEqualTo(false)

                    val pointOnLink2: SnappedPointOnLink =
                        pointOnLink.withSnappedToTerminusNode(divideLinkLength(pointOnLink, 2))

                    assertThat(pointOnLink2.isSnappedToStartNode).isEqualTo(true)
                    assertThat(pointOnLink2.isSnappedToEndNode).isEqualTo(false)
                }
        }

        @Test
        @DisplayName("When closest point is not already projected to end node but expected to be snapped to it")
        fun whenClosestPointIsNotAlreadyProjectedToEndNodeButExpectedToBeSnappedToIt() {
            qt().forAll(
                SnappedPointOnLinkGenerator.snapLink(
                    hasDiscreteEndpoints = true,
                    CLOSE_TO_END
                )
            )
                .checkAssert { pointOnLink: SnappedPointOnLink ->

                    assertThat(pointOnLink.isSnappedToStartNode).isEqualTo(false)
                    assertThat(pointOnLink.isSnappedToEndNode).isEqualTo(false)

                    val pointOnLink2: SnappedPointOnLink =
                        pointOnLink.withSnappedToTerminusNode(divideLinkLength(pointOnLink, 2))

                    assertThat(pointOnLink2.isSnappedToStartNode).isEqualTo(false)
                    assertThat(pointOnLink2.isSnappedToEndNode).isEqualTo(true)
                }
        }

        @Test
        @DisplayName("When closest point is not projected to terminus nodes and not expected to be snapped to them")
        fun whenClosestPointIsNotProjectedToTerminusNodesAndNotExpectedToBeSnappedToThem() {
            qt().forAll(
                SnappedPointOnLinkGenerator.snapLink(
                    hasDiscreteEndpoints = true,
                    AT_MIDPOINT
                )
            )
                .checkAssert { pointOnLink: SnappedPointOnLink ->

                    assertThat(pointOnLink.isSnappedToStartNode).isEqualTo(false)
                    assertThat(pointOnLink.isSnappedToEndNode).isEqualTo(false)

                    val pointOnLink2: SnappedPointOnLink =
                        pointOnLink.withSnappedToTerminusNode(divideLinkLength(pointOnLink, 4))

                    assertThat(pointOnLink2.isSnappedToStartNode).isEqualTo(false)
                    assertThat(pointOnLink2.isSnappedToEndNode).isEqualTo(false)
                }
        }
    }

    @Nested
    @DisplayName("isOnLinkTerminatedByNode")
    inner class IsOnLinkTerminatedByNode {
        @Test
        @DisplayName("Should return true when given ID of start node")
        fun shouldReturnTrueForStartNodeId() {
            qt().forAll(SnappedPointOnLinkGenerator.snapLink())
                .checkAssert { pointOnLink: SnappedPointOnLink ->

                    assertThat(pointOnLink.isOnLinkTerminatedByNode(pointOnLink.startNodeId)).isEqualTo(true)
                }
        }

        @Test
        @DisplayName("Should return true when given ID of end node")
        fun shouldReturnTrueForEndNodeId() {
            qt().forAll(SnappedPointOnLinkGenerator.snapLink())
                .checkAssert { pointOnLink: SnappedPointOnLink ->

                    assertThat(pointOnLink.isOnLinkTerminatedByNode(pointOnLink.endNodeId)).isEqualTo(true)
                }
        }

        @Test
        @DisplayName("Should return false when given other node ID")
        fun whenTwoLinksDoNotShareNode() {
            qt().forAll(SnappedPointOnLinkGenerator.snapTwoUnconnectedLinks())
                .checkAssert { (firstPointOnLink: SnappedPointOnLink, secondPointOnLink: SnappedPointOnLink) ->

                    assertThat(
                        firstPointOnLink.isOnLinkTerminatedByNode(secondPointOnLink.startNodeId) ||
                            firstPointOnLink.isOnLinkTerminatedByNode(secondPointOnLink.endNodeId)
                    )
                        .isEqualTo(false)
                }
        }
    }

    @Nested
    @DisplayName("isOnLinkWithDiscreteNodes")
    inner class IsOnLinkWithDiscreteNodes {
        @Test
        @DisplayName("When endpoint nodes of infrastructure links are discrete")
        fun whenEndpointNodesAreDiscrete() {
            qt().forAll(SnappedPointOnLinkGenerator.snapLink(hasDiscreteEndpoints = true))
                .checkAssert { pointOnLink: SnappedPointOnLink ->

                    assertThat(pointOnLink.isOnLinkWithDiscreteNodes()).isEqualTo(true)
                }
        }

        @Test
        @DisplayName("When infrastructure link has same node at both endpoints")
        fun whenSingleNodeAppearsAtBothEndpoints() {
            qt().forAll(SnappedPointOnLinkGenerator.snapLink(hasDiscreteEndpoints = false))
                .checkAssert { pointOnLink: SnappedPointOnLink ->

                    assertThat(pointOnLink.isOnLinkWithDiscreteNodes()).isEqualTo(false)
                }
        }
    }
}
