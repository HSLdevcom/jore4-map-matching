package fi.hsl.jore4.mapmatching.repository.infrastructure

import fi.hsl.jore4.mapmatching.test.generators.SnapPointLocationAlongLinkFilter.AT_END
import fi.hsl.jore4.mapmatching.test.generators.SnapPointLocationAlongLinkFilter.AT_MIDPOINT
import fi.hsl.jore4.mapmatching.test.generators.SnapPointLocationAlongLinkFilter.AT_START
import fi.hsl.jore4.mapmatching.test.generators.SnapPointLocationAlongLinkFilter.CLOSE_TO_END
import fi.hsl.jore4.mapmatching.test.generators.SnapPointLocationAlongLinkFilter.CLOSE_TO_START
import fi.hsl.jore4.mapmatching.test.generators.SnapPointLocationAlongLinkFilter.NOT_AT_END
import fi.hsl.jore4.mapmatching.test.generators.SnapPointLocationAlongLinkFilter.NOT_AT_START
import fi.hsl.jore4.mapmatching.test.generators.SnappedLinkStateGenerator
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.quicktheories.QuickTheory.qt

@DisplayName("Test SnappedLinkState class")
class SnappedLinkStateTest {

    @Nested
    @DisplayName("isSnappedToStartNode")
    inner class IsSnappedToStartNode {

        @Test
        @DisplayName("When projected closest point is within snap-to-endpoint distance to link start")
        fun whenClosestPointWithinSnapDistanceToLinkStart() {
            qt().forAll(SnappedLinkStateGenerator.snapLink(hasDiscreteEndpoints = true,
                                                           AT_START))
                .checkAssert { snap: SnappedLinkState ->

                    assertThat(snap.isSnappedToStartNode).isEqualTo(true)
                }
        }

        @Test
        @DisplayName("When projected closest point is outside of snap-to-endpoint distance to link start")
        fun whenClosestPointOutsideSnapDistanceToLinkStart() {
            qt().forAll(SnappedLinkStateGenerator.snapLink(hasDiscreteEndpoints = true,
                                                           NOT_AT_START))
                .checkAssert { snap: SnappedLinkState ->

                    assertThat(snap.isSnappedToStartNode).isEqualTo(false)
                }
        }
    }

    @Nested
    @DisplayName("isSnappedToEndNode")
    inner class IsSnappedToEndNode {

        @Test
        @DisplayName("When projected closest point is within snap-to-endpoint distance to link end")
        fun whenClosestPointWithinSnapDistanceToLinkEnd() {
            qt().forAll(SnappedLinkStateGenerator.snapLink(hasDiscreteEndpoints = true,
                                                           AT_END))
                .checkAssert { snap: SnappedLinkState ->

                    assertThat(snap.isSnappedToEndNode).isEqualTo(true)
                }
        }

        @Test
        @DisplayName("When projected closest point is outside of snap-to-endpoint distance to link end")
        fun whenClosestPointOutsideSnapDistanceToLinkEnd() {
            qt().forAll(SnappedLinkStateGenerator.snapLink(hasDiscreteEndpoints = true,
                                                           NOT_AT_END))
                .checkAssert { snap: SnappedLinkState ->

                    assertThat(snap.isSnappedToEndNode).isEqualTo(false)
                }
        }
    }

    @Nested
    @DisplayName("withSnappedToTerminusNode")
    inner class WithSnappedToTerminusNode {

        private fun divideLinkLength(snap: SnappedLinkState, divisor: Int): Double =
            snap.infrastructureLinkLength / divisor

        @Test
        @DisplayName("When closest point is already projected to link start")
        fun whenClosestPointAlreadyAtLinkStart() {
            qt().forAll(SnappedLinkStateGenerator.snapLink(hasDiscreteEndpoints = true,
                                                           AT_START))
                .checkAssert { snap: SnappedLinkState ->

                    assertThat(snap.isSnappedToStartNode).isEqualTo(true)
                    assertThat(snap.isSnappedToEndNode).isEqualTo(false)

                    val snap2: SnappedLinkState = snap.withSnappedToTerminusNode(divideLinkLength(snap, 2))

                    assertThat(snap2.isSnappedToStartNode).isEqualTo(true)
                    assertThat(snap2.isSnappedToEndNode).isEqualTo(false)
                }
        }

        @Test
        @DisplayName("When closest point is already projected to link end")
        fun whenClosestPointAlreadyAtLinkEnd() {
            qt().forAll(SnappedLinkStateGenerator.snapLink(hasDiscreteEndpoints = true,
                                                           AT_END))
                .checkAssert { snap: SnappedLinkState ->

                    assertThat(snap.isSnappedToStartNode).isEqualTo(false)
                    assertThat(snap.isSnappedToEndNode).isEqualTo(true)

                    val snap2: SnappedLinkState = snap.withSnappedToTerminusNode(divideLinkLength(snap, 2))

                    assertThat(snap2.isSnappedToStartNode).isEqualTo(false)
                    assertThat(snap2.isSnappedToEndNode).isEqualTo(true)
                }
        }

        @Test
        @DisplayName("When closest point is not already projected to start node but expected to be snapped to it")
        fun whenClosestPointIsNotAlreadyProjectedToStartNodeButExpectedToBeSnappedToIt() {
            qt().forAll(SnappedLinkStateGenerator.snapLink(hasDiscreteEndpoints = true,
                                                           CLOSE_TO_START))
                .checkAssert { snap: SnappedLinkState ->

                    assertThat(snap.isSnappedToStartNode).isEqualTo(false)
                    assertThat(snap.isSnappedToEndNode).isEqualTo(false)

                    val snap2: SnappedLinkState = snap.withSnappedToTerminusNode(divideLinkLength(snap, 2))

                    assertThat(snap2.isSnappedToStartNode).isEqualTo(true)
                    assertThat(snap2.isSnappedToEndNode).isEqualTo(false)
                }
        }

        @Test
        @DisplayName("When closest point is not already projected to end node but expected to be snapped to it")
        fun whenClosestPointIsNotAlreadyProjectedToEndNodeButExpectedToBeSnappedToIt() {
            qt().forAll(SnappedLinkStateGenerator.snapLink(hasDiscreteEndpoints = true,
                                                           CLOSE_TO_END))
                .checkAssert { snap: SnappedLinkState ->

                    assertThat(snap.isSnappedToStartNode).isEqualTo(false)
                    assertThat(snap.isSnappedToEndNode).isEqualTo(false)

                    val snap2: SnappedLinkState = snap.withSnappedToTerminusNode(divideLinkLength(snap, 2))

                    assertThat(snap2.isSnappedToStartNode).isEqualTo(false)
                    assertThat(snap2.isSnappedToEndNode).isEqualTo(true)
                }
        }

        @Test
        @DisplayName("When closest point is not projected to terminus nodes and not expected to be snapped to them")
        fun whenClosestPointIsNotProjectedToTerminusNodesAndNotExpectedToBeSnappedToThem() {
            qt().forAll(SnappedLinkStateGenerator.snapLink(hasDiscreteEndpoints = true,
                                                           AT_MIDPOINT))
                .checkAssert { snap: SnappedLinkState ->

                    assertThat(snap.isSnappedToStartNode).isEqualTo(false)
                    assertThat(snap.isSnappedToEndNode).isEqualTo(false)

                    val snap2: SnappedLinkState = snap.withSnappedToTerminusNode(divideLinkLength(snap, 4))

                    assertThat(snap2.isSnappedToStartNode).isEqualTo(false)
                    assertThat(snap2.isSnappedToEndNode).isEqualTo(false)
                }
        }
    }

    @Nested
    @DisplayName("hasNode")
    inner class HasNode {

        @Test
        @DisplayName("Should return true when given ID of start node")
        fun shouldReturnTrueForStartNodeId() {
            qt().forAll(SnappedLinkStateGenerator.snapLink())
                .checkAssert { snap: SnappedLinkState ->

                    assertThat(snap.hasNode(snap.startNodeId)).isEqualTo(true)
                }
        }

        @Test
        @DisplayName("Should return true when given ID of end node")
        fun shouldReturnTrueForEndNodeId() {
            qt().forAll(SnappedLinkStateGenerator.snapLink())
                .checkAssert { snap: SnappedLinkState ->

                    assertThat(snap.hasNode(snap.endNodeId)).isEqualTo(true)
                }
        }

        @Test
        @DisplayName("Should return false when given other node ID")
        fun whenTwoLinksDoNotShareNode() {
            qt().forAll(SnappedLinkStateGenerator.snapTwoUnconnectedLinks())
                .checkAssert { (firstSnap: SnappedLinkState, secondSnap: SnappedLinkState) ->

                    assertThat(firstSnap.hasNode(secondSnap.startNodeId) || firstSnap.hasNode(secondSnap.endNodeId))
                        .isEqualTo(false)
                }
        }
    }

    @Nested
    @DisplayName("hasSharedNode")
    inner class HasSharedNode {

        @Test
        @DisplayName("When two infrastructure links have a common node")
        fun whenTwoLinksShareNode() {
            qt().forAll(SnappedLinkStateGenerator.snapTwoConnectedLinks())
                .checkAssert { (firstSnap: SnappedLinkState, secondSnap: SnappedLinkState) ->

                    assertThat(firstSnap.hasSharedNode(secondSnap))
                        .isEqualTo(true)
                }
        }

        @Test
        @DisplayName("When two infrastructure links do not have a common node")
        fun whenTwoLinksDoNotShareNode() {
            qt().forAll(SnappedLinkStateGenerator.snapTwoUnconnectedLinks())
                .checkAssert { (firstSnap: SnappedLinkState, secondSnap: SnappedLinkState) ->

                    assertThat(firstSnap.hasSharedNode(secondSnap))
                        .isEqualTo(false)
                }
        }
    }

    @Nested
    @DisplayName("hasDiscreteNodes")
    inner class HasDiscreteNodes {

        @Test
        @DisplayName("When endpoint nodes of infrastructure links are discrete")
        fun whenEndpointNodesAreDiscrete() {
            qt().forAll(SnappedLinkStateGenerator.snapLink(hasDiscreteEndpoints = true))
                .checkAssert { snap: SnappedLinkState ->

                    assertThat(snap.hasDiscreteNodes()).isEqualTo(true)
                }
        }

        @Test
        @DisplayName("When infrastructure link has same node at both endpoints")
        fun whenSingleNodeAppearsAtBothEndpoints() {
            qt().forAll(SnappedLinkStateGenerator.snapLink(hasDiscreteEndpoints = false))
                .checkAssert { snap: SnappedLinkState ->

                    assertThat(snap.hasDiscreteNodes()).isEqualTo(false)
                }
        }
    }
}
