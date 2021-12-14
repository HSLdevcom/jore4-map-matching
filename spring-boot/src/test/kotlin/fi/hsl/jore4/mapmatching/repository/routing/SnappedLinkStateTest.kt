package fi.hsl.jore4.mapmatching.repository.routing

import fi.hsl.jore4.mapmatching.repository.infrastructure.SnappedLinkState
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
            qt().forAll(SnappedLinkStateGenerator.snappedLinkState())
                .checkAssert { link: SnappedLinkState ->

                    assertThat(link.hasNode(link.startNode.id))
                        .isEqualTo(true)
                }
        }

        @Test
        @DisplayName("Should return true when given ID of end node")
        fun shouldReturnTrueForEndNodeId() {
            qt().forAll(SnappedLinkStateGenerator.snappedLinkState())
                .checkAssert { link: SnappedLinkState ->

                    assertThat(link.hasNode(link.endNode.id))
                        .isEqualTo(true)
                }
        }

        @Test
        @DisplayName("Should return false when given other node ID")
        fun whenTwoLinksDoNotShareNode() {
            qt().forAll(SnappedLinkStateGenerator.twoUnconnectedLinks())
                .checkAssert { (firstLink: SnappedLinkState, secondLink: SnappedLinkState) ->

                    assertThat(firstLink.hasNode(secondLink.startNode.id) || firstLink.hasNode(secondLink.endNode.id))
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
            qt().forAll(SnappedLinkStateGenerator.twoConnectedLinks())
                .checkAssert { (firstLink: SnappedLinkState, secondLink: SnappedLinkState) ->

                    assertThat(firstLink.hasSharedNode(secondLink))
                        .isEqualTo(true)
                }
        }

        @Test
        @DisplayName("When two infrastructure links do not have a common node")
        fun whenTwoLinksDoNotShareNode() {
            qt().forAll(SnappedLinkStateGenerator.twoUnconnectedLinks())
                .checkAssert { (firstLink: SnappedLinkState, secondLink: SnappedLinkState) ->

                    assertThat(firstLink.hasSharedNode(secondLink))
                        .isEqualTo(false)
                }
        }
    }
}
