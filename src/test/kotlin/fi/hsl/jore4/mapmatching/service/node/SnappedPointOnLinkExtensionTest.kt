package fi.hsl.jore4.mapmatching.service.node

import fi.hsl.jore4.mapmatching.model.TrafficFlowDirectionType
import fi.hsl.jore4.mapmatching.model.TrafficFlowDirectionType.AGAINST_DIGITISED_DIRECTION
import fi.hsl.jore4.mapmatching.model.TrafficFlowDirectionType.ALONG_DIGITISED_DIRECTION
import fi.hsl.jore4.mapmatching.model.TrafficFlowDirectionType.BIDIRECTIONAL
import fi.hsl.jore4.mapmatching.repository.infrastructure.SnappedPointOnLink
import fi.hsl.jore4.mapmatching.service.node.SnappedPointOnLinkExtension.toVisitedNodes
import fi.hsl.jore4.mapmatching.test.generators.SnapPointLocationAlongLinkFilter
import fi.hsl.jore4.mapmatching.test.generators.SnapPointLocationAlongLinkFilter.AT_END
import fi.hsl.jore4.mapmatching.test.generators.SnapPointLocationAlongLinkFilter.AT_START
import fi.hsl.jore4.mapmatching.test.generators.SnapPointLocationAlongLinkFilter.BETWEEN_ENDPOINTS_EXCLUSIVE
import fi.hsl.jore4.mapmatching.test.generators.SnappedPointOnLinkGenerator
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.quicktheories.QuickTheory.qt
import org.quicktheories.dsl.TheoryBuilder

@DisplayName("Test SnappedPointOnLinkExtensionTest class")
class SnappedPointOnLinkExtensionTest {
    private fun forSnappedLinksWithDiscreteEndpoints(
        trafficFlowDirectionType: TrafficFlowDirectionType,
        snapPointLocationFilter: SnapPointLocationAlongLinkFilter
    ): TheoryBuilder<SnappedPointOnLink> =
        qt().forAll(
            SnappedPointOnLinkGenerator.snapLink(
                hasDiscreteEndpoints = true,
                trafficFlowDirectionType,
                snapPointLocationFilter
            )
        )

    @Nested
    @DisplayName("toVisitedNodes")
    inner class ToVisitedNodes {
        @Test
        @DisplayName("When infrastructure link starts from same node as it ends at")
        fun whenEndpointNodesAreNonDiscrete() =
            qt()
                .forAll(SnappedPointOnLinkGenerator.snapLink(hasDiscreteEndpoints = false))
                .checkAssert { pointOnLink ->

                    assertThat(pointOnLink.toVisitedNodes())
                        .isEqualTo(
                            VisitSingleNode(
                                pointOnLink.startNodeId
                            )
                        )
                }

        @Nested
        @DisplayName("When snapped to start point of infrastructure link")
        inner class WhenSnappedToStartPointOfInfrastructureLink {
            @Test
            @DisplayName("When infrastructure link is bidirectional")
            fun whenInfrastructureLinkIsBidirectional() =
                forSnappedLinksWithDiscreteEndpoints(
                    BIDIRECTIONAL,
                    AT_START
                ).checkAssert { pointOnLink ->

                    assertThat(pointOnLink.toVisitedNodes())
                        .isEqualTo(
                            VisitSingleNode(
                                pointOnLink.startNodeId
                            )
                        )
                }

            @Test
            @DisplayName("When traversal is along digitised direction of infrastructure link")
            fun whenTraversalIsAlongDigitisedDirectionOfInfrastructureLink() =
                forSnappedLinksWithDiscreteEndpoints(
                    ALONG_DIGITISED_DIRECTION,
                    AT_START
                ).checkAssert { pointOnLink ->

                    assertThat(pointOnLink.toVisitedNodes())
                        .isEqualTo(
                            VisitSingleNode(
                                pointOnLink.startNodeId
                            )
                        )
                }

            @Test
            @DisplayName("When traversal is against digitised direction of infrastructure link")
            fun whenTraversalIsAgainstDigitisedDirectionOfInfrastructureLink() =
                forSnappedLinksWithDiscreteEndpoints(
                    AGAINST_DIGITISED_DIRECTION,
                    AT_START
                ).checkAssert { pointOnLink ->

                    assertThat(pointOnLink.toVisitedNodes())
                        .isEqualTo(
                            VisitSingleNode(
                                pointOnLink.startNodeId
                            )
                        )
                }
        }

        @Nested
        @DisplayName("When snapped to end point of infrastructure link")
        inner class WhenSnappedToEndPointOfInfrastructureLink {
            @Test
            @DisplayName("When infrastructure link is bidirectional")
            fun whenInfrastructureLinkIsBidirectional() =
                forSnappedLinksWithDiscreteEndpoints(
                    BIDIRECTIONAL,
                    AT_END
                ).checkAssert { pointOnLink ->

                    assertThat(pointOnLink.toVisitedNodes())
                        .isEqualTo(
                            VisitSingleNode(
                                pointOnLink.endNodeId
                            )
                        )
                }

            @Test
            @DisplayName("When traversal is along digitised direction of infrastructure link")
            fun whenTraversalIsAlongDigitisedDirectionOfInfrastructureLink() =
                forSnappedLinksWithDiscreteEndpoints(
                    ALONG_DIGITISED_DIRECTION,
                    AT_END
                ).checkAssert { pointOnLink ->

                    assertThat(pointOnLink.toVisitedNodes())
                        .isEqualTo(
                            VisitSingleNode(
                                pointOnLink.endNodeId
                            )
                        )
                }

            @Test
            @DisplayName("When traversal is against digitised direction of infrastructure link")
            fun whenTraversalIsAgainstDigitisedDirectionOfInfrastructureLink() =
                forSnappedLinksWithDiscreteEndpoints(
                    AGAINST_DIGITISED_DIRECTION,
                    AT_END
                ).checkAssert { pointOnLink ->

                    assertThat(pointOnLink.toVisitedNodes())
                        .isEqualTo(
                            VisitSingleNode(
                                pointOnLink.endNodeId
                            )
                        )
                }
        }

        @Nested
        @DisplayName("When snapped to in-between endpoints of infrastructure link")
        inner class WhenSnappedToInBetweenLinkEndpoints {
            @Test
            @DisplayName("When infrastructure link is bidirectional")
            fun whenInfrastructureLinkIsBidirectional() =
                forSnappedLinksWithDiscreteEndpoints(
                    BIDIRECTIONAL,
                    BETWEEN_ENDPOINTS_EXCLUSIVE
                ).checkAssert { pointOnLink ->

                    assertThat(pointOnLink.toVisitedNodes())
                        .isEqualTo(
                            VisitNodesOfSingleLinkBidirectionally(
                                pointOnLink.startNodeId,
                                pointOnLink.endNodeId
                            )
                        )
                }

            @Test
            @DisplayName("When infrastructure link is one-way and traversal is along the digitised direction")
            fun whenInfrastructureLinkIsOneWayAndTraversalIsAlongDigitisedDirection() =
                forSnappedLinksWithDiscreteEndpoints(
                    ALONG_DIGITISED_DIRECTION,
                    BETWEEN_ENDPOINTS_EXCLUSIVE
                ).checkAssert { pointOnLink ->

                    assertThat(pointOnLink.toVisitedNodes())
                        .isEqualTo(
                            VisitNodesOfSingleLinkUnidirectionally(
                                pointOnLink.startNodeId,
                                pointOnLink.endNodeId
                            )
                        )
                }

            @Test
            @DisplayName("When infrastructure link is one-way and traversal is against the digitised direction")
            fun whenInfrastructureLinkIsOneWayAndTraversalIsAgainstDigitisedDirection() =
                forSnappedLinksWithDiscreteEndpoints(
                    AGAINST_DIGITISED_DIRECTION,
                    BETWEEN_ENDPOINTS_EXCLUSIVE
                ).checkAssert { pointOnLink ->

                    assertThat(pointOnLink.toVisitedNodes())
                        .isEqualTo(
                            VisitNodesOfSingleLinkUnidirectionally(
                                pointOnLink.endNodeId,
                                pointOnLink.startNodeId
                            )
                        )
                }
        }
    }
}
