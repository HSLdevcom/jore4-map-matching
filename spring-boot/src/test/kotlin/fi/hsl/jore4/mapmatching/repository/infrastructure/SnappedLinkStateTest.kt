package fi.hsl.jore4.mapmatching.repository.infrastructure

import fi.hsl.jore4.mapmatching.test.generators.SnappedLinkStateGenerator
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.quicktheories.QuickTheory.qt

@DisplayName("Test SnappedLinkState class")
class SnappedLinkStateTest {

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
            qt().forAll(SnappedLinkStateGenerator.snapLink(withDiscreteEndpoints = true))
                .checkAssert { snap: SnappedLinkState ->

                    assertThat(snap.hasDiscreteNodes()).isEqualTo(true)
                }
        }

        @Test
        @DisplayName("When infrastructure link has same node at both endpoints")
        fun whenSingleNodeAppearsAtBothEndpoints() {
            qt().forAll(SnappedLinkStateGenerator.snapLink(withDiscreteEndpoints = false))
                .checkAssert { snap: SnappedLinkState ->

                    assertThat(snap.hasDiscreteNodes()).isEqualTo(false)
                }
        }
    }
}
