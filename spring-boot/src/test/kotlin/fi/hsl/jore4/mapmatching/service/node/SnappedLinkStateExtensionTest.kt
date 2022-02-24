package fi.hsl.jore4.mapmatching.service.node

import fi.hsl.jore4.mapmatching.model.TrafficFlowDirectionType
import fi.hsl.jore4.mapmatching.model.TrafficFlowDirectionType.AGAINST_DIGITISED_DIRECTION
import fi.hsl.jore4.mapmatching.model.TrafficFlowDirectionType.ALONG_DIGITISED_DIRECTION
import fi.hsl.jore4.mapmatching.model.TrafficFlowDirectionType.BIDIRECTIONAL
import fi.hsl.jore4.mapmatching.repository.infrastructure.SnappedLinkState
import fi.hsl.jore4.mapmatching.service.node.SnappedLinkStateExtension.toVisitedNodes
import fi.hsl.jore4.mapmatching.test.generators.SnapPointLocationAlongLinkFilter
import fi.hsl.jore4.mapmatching.test.generators.SnapPointLocationAlongLinkFilter.AT_END
import fi.hsl.jore4.mapmatching.test.generators.SnapPointLocationAlongLinkFilter.AT_START
import fi.hsl.jore4.mapmatching.test.generators.SnapPointLocationAlongLinkFilter.BETWEEN_ENDPOINTS_EXCLUSIVE
import fi.hsl.jore4.mapmatching.test.generators.SnapPointLocationAlongLinkFilter.CLOSE_TO_END
import fi.hsl.jore4.mapmatching.test.generators.SnapPointLocationAlongLinkFilter.CLOSE_TO_START
import fi.hsl.jore4.mapmatching.test.generators.SnappedLinkStateGenerator
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.quicktheories.QuickTheory.qt
import org.quicktheories.dsl.TheoryBuilder

@DisplayName("Test SnappedLinkStateExtension class")
class SnappedLinkStateExtensionTest {

    private fun forSnappedLinksWithDiscreteEndpoints(trafficFlowDirectionType: TrafficFlowDirectionType,
                                                     snapPointLocationFilter: SnapPointLocationAlongLinkFilter)
        : TheoryBuilder<SnappedLinkState> {

        return qt().forAll(SnappedLinkStateGenerator.snapLink(hasDiscreteEndpoints = true,
                                                              trafficFlowDirectionType,
                                                              snapPointLocationFilter))
    }

    @Nested
    @DisplayName("toVisitedNodes")
    inner class ToVisitedNodes {

        @Test
        @DisplayName("When infrastructure link starts from same node as it ends at")
        fun whenEndpointNodesAreNonDiscrete() {
            return qt()
                .forAll(SnappedLinkStateGenerator.snapLink(hasDiscreteEndpoints = false))
                .checkAssert { snappedLink ->

                    assertThat(snappedLink.toVisitedNodes())
                        .isEqualTo(
                            VisitSingleNode(
                                snappedLink.startNodeId))
                }
        }

        @Nested
        @DisplayName("When snapped to start point of infrastructure link")
        inner class WhenSnappedToStartPointOfInfrastructureLink {

            @Test
            @DisplayName("When infrastructure link is bidirectional")
            fun whenInfrastructureLinkIsBidirectional() {
                return forSnappedLinksWithDiscreteEndpoints(BIDIRECTIONAL,
                                                            AT_START)
                    .checkAssert { snappedLink ->

                        assertThat(snappedLink.toVisitedNodes())
                            .isEqualTo(
                                VisitSingleNode(
                                    snappedLink.startNodeId))
                    }
            }

            @Test
            @DisplayName("When traversal is along digitised direction of infrastructure link")
            fun whenTraversalIsAlongDigitisedDirectionOfInfrastructureLink() {
                return forSnappedLinksWithDiscreteEndpoints(ALONG_DIGITISED_DIRECTION,
                                                            AT_START)
                    .checkAssert { snappedLink ->

                        assertThat(snappedLink.toVisitedNodes())
                            .isEqualTo(
                                VisitSingleNode(
                                    snappedLink.startNodeId))
                    }
            }

            @Test
            @DisplayName("When traversal is against digitised direction of infrastructure link")
            fun whenTraversalIsAgainstDigitisedDirectionOfInfrastructureLink() {
                return forSnappedLinksWithDiscreteEndpoints(AGAINST_DIGITISED_DIRECTION,
                                                            AT_START)
                    .checkAssert { snappedLink ->

                        assertThat(snappedLink.toVisitedNodes())
                            .isEqualTo(
                                VisitSingleNode(
                                    snappedLink.startNodeId))
                    }
            }
        }

        @Nested
        @DisplayName("When snapped to end point of infrastructure link")
        inner class WhenSnappedToEndPointOfInfrastructureLink {

            @Test
            @DisplayName("When infrastructure link is bidirectional")
            fun whenInfrastructureLinkIsBidirectional() {
                return forSnappedLinksWithDiscreteEndpoints(BIDIRECTIONAL,
                                                            AT_END)
                    .checkAssert { snappedLink ->

                        assertThat(snappedLink.toVisitedNodes())
                            .isEqualTo(
                                VisitSingleNode(
                                    snappedLink.endNodeId))
                    }
            }

            @Test
            @DisplayName("When traversal is along digitised direction of infrastructure link")
            fun whenTraversalIsAlongDigitisedDirectionOfInfrastructureLink() {
                return forSnappedLinksWithDiscreteEndpoints(ALONG_DIGITISED_DIRECTION,
                                                            AT_END)
                    .checkAssert { snappedLink ->

                        assertThat(snappedLink.toVisitedNodes())
                            .isEqualTo(
                                VisitSingleNode(
                                    snappedLink.endNodeId))
                    }
            }

            @Test
            @DisplayName("When traversal is against digitised direction of infrastructure link")
            fun whenTraversalIsAgainstDigitisedDirectionOfInfrastructureLink() {
                return forSnappedLinksWithDiscreteEndpoints(AGAINST_DIGITISED_DIRECTION,
                                                            AT_END)
                    .checkAssert { snappedLink ->

                        assertThat(snappedLink.toVisitedNodes())
                            .isEqualTo(
                                VisitSingleNode(
                                    snappedLink.endNodeId))
                    }
            }
        }

        @Nested
        @DisplayName("When snapped to in-between endpoints of infrastructure link")
        inner class WhenSnappedToInBetweenLinkEndpoints {

            @Test
            @DisplayName("When infrastructure link is bidirectional")
            fun whenInfrastructureLinkIsBidirectional() {
                return forSnappedLinksWithDiscreteEndpoints(BIDIRECTIONAL,
                                                            BETWEEN_ENDPOINTS_EXCLUSIVE)
                    .checkAssert { snappedLink ->

                        assertThat(snappedLink.toVisitedNodes())
                            .isEqualTo(
                                VisitNodesOfSingleLinkBidirectionally(
                                    snappedLink.startNodeId, snappedLink.endNodeId))
                    }
            }

            @Test
            @DisplayName("When infrastructure link is one-way and traversal is along the digitised direction")
            fun whenInfrastructureLinkIsOneWayAndTraversalIsAlongDigitisedDirection() {
                return forSnappedLinksWithDiscreteEndpoints(ALONG_DIGITISED_DIRECTION,
                                                            BETWEEN_ENDPOINTS_EXCLUSIVE)
                    .checkAssert { snappedLink ->

                        assertThat(snappedLink.toVisitedNodes())
                            .isEqualTo(
                                VisitNodesOfSingleLinkUnidirectionally(
                                    snappedLink.startNodeId, snappedLink.endNodeId))
                    }
            }

            @Test
            @DisplayName("When infrastructure link is one-way and traversal is against the digitised direction")
            fun whenInfrastructureLinkIsOneWayAndTraversalIsAgainstDigitisedDirection() {
                return forSnappedLinksWithDiscreteEndpoints(AGAINST_DIGITISED_DIRECTION,
                                                            BETWEEN_ENDPOINTS_EXCLUSIVE)
                    .checkAssert { snappedLink ->

                        assertThat(snappedLink.toVisitedNodes())
                            .isEqualTo(
                                VisitNodesOfSingleLinkUnidirectionally(
                                        snappedLink.endNodeId, snappedLink.startNodeId))
                    }
            }
        }
    }
}
