package fi.hsl.jore4.mapmatching.service.node

import fi.hsl.jore4.mapmatching.model.NodeIdSequence
import fi.hsl.jore4.mapmatching.repository.infrastructure.SnappedLinkState
import fi.hsl.jore4.mapmatching.service.node.SnappedLinkStateExtension.getNodeIdSequenceCombinations
import fi.hsl.jore4.mapmatching.service.node.SnappedLinkStateExtension.toNodeIdList
import fi.hsl.jore4.mapmatching.test.generators.LinkEndpointsProximityFilter
import fi.hsl.jore4.mapmatching.test.generators.LinkEndpointsProximityFilter.END_NODE_CLOSER
import fi.hsl.jore4.mapmatching.test.generators.LinkEndpointsProximityFilter.START_NODE_CLOSER_OR_EQUAL_DISTANCE
import fi.hsl.jore4.mapmatching.test.generators.SnappedLinkStateGenerator
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.quicktheories.QuickTheory.qt
import org.quicktheories.dsl.TheoryBuilder

@DisplayName("Test SnappedLinkStateExtension class")
class SnappedLinkStateExtensionTest {

    private fun forSnappedLinksWithDiscreteEndpoints(trafficFlowDirectionType: Int,
                                                     nodeProximityFilter: LinkEndpointsProximityFilter? = null)
        : TheoryBuilder<SnappedLinkState> {

        return qt().forAll(SnappedLinkStateGenerator.snapLink(withDiscreteEndpoints = true,
                                                              trafficFlowDirectionType,
                                                              nodeProximityFilter))
    }

    @Nested
    @DisplayName("toNodeList")
    inner class ToNodeIdList {

        @Nested
        @DisplayName("When endpoint nodes of infrastructure link are discrete")
        inner class WhenEndpointNodesAreDiscrete {

            @Test
            @DisplayName("When link in one-way and direction of traffic flow is same as digitised direction")
            fun whenLinkIsOneWayAndDirectionOfTrafficFlowIsSameAsDigitisedDirection() {
                return forSnappedLinksWithDiscreteEndpoints(trafficFlowDirectionType = 4)
                    .checkAssert { snappedLink ->

                        assertThat(snappedLink.toNodeIdList())
                            .isEqualTo(listOf(snappedLink.startNodeId, snappedLink.endNodeId))
                    }
            }

            @Test
            @DisplayName("When link in one-way and direction of traffic flow is against digitised direction")
            fun whenLinkIsOneWayAndDirectionOfTrafficFlowIsAgainstDigitisedDirection() {
                return forSnappedLinksWithDiscreteEndpoints(trafficFlowDirectionType = 3)
                    .checkAssert { snappedLink ->

                        assertThat(snappedLink.toNodeIdList())
                            .isEqualTo(listOf(snappedLink.endNodeId, snappedLink.startNodeId))
                    }
            }

            @Nested
            @DisplayName("When link is bidirectional")
            inner class WhenLinkIsBidirectional {

                @Test
                @DisplayName("When distance to start node is less than or equal to distance to end node")
                fun whenDistanceToStartNodeIsLessThanOrEqualToDistanceToEndNode() {
                    return forSnappedLinksWithDiscreteEndpoints(trafficFlowDirectionType = 2,
                                                                START_NODE_CLOSER_OR_EQUAL_DISTANCE)
                        .checkAssert { snappedLink ->

                            assertThat(snappedLink.toNodeIdList())
                                .isEqualTo(listOf(snappedLink.startNodeId, snappedLink.endNodeId))
                        }
                }

                @Test
                @DisplayName("When distance to start node is greater than distance to end node")
                fun whenDistanceToStartNodeIsGreaterThanDistanceToEndNode() {
                    return forSnappedLinksWithDiscreteEndpoints(trafficFlowDirectionType = 2,
                                                                END_NODE_CLOSER)
                        .checkAssert { snappedLink ->

                            assertThat(snappedLink.toNodeIdList())
                                .isEqualTo(listOf(snappedLink.endNodeId, snappedLink.startNodeId))
                        }
                }
            }
        }

        @Nested
        @DisplayName("When single node appears at both endpoints of infrastructure link")
        inner class WhenSingleNodeAppearsAtBothEndpoints {

            @Test
            @DisplayName("Should return single node ID independent of node distances")
            fun independentOfNodeDistances() {
                return qt()
                    .forAll(SnappedLinkStateGenerator.snapLink(withDiscreteEndpoints = false))
                    .checkAssert { snappedLink ->

                        assertThat(snappedLink.toNodeIdList())
                            .isEqualTo(listOf(snappedLink.startNodeId))
                    }
            }
        }
    }

    @Nested
    @DisplayName("Get node ID sequence combinations")
    inner class GetNodeIdSequenceCombinations {

        @Nested
        @DisplayName("When endpoint nodes of infrastructure link are discrete")
        inner class WhenEndpointNodesAreDiscrete {

            @Test
            @DisplayName("When link in one-way and direction of traffic flow is same as digitised direction")
            fun whenLinkIsOneWayAndDirectionOfTrafficFlowIsSameAsDigitisedDirection() {
                return forSnappedLinksWithDiscreteEndpoints(trafficFlowDirectionType = 4)
                    .checkAssert { snappedLink ->

                        assertThat(snappedLink.getNodeIdSequenceCombinations())
                            .isEqualTo(listOf(
                                NodeIdSequence(listOf(snappedLink.startNodeId, snappedLink.endNodeId))))
                    }
            }

            @Test
            @DisplayName("When link in one-way and direction of traffic flow is against digitised direction")
            fun whenLinkIsOneWayAndDirectionOfTrafficFlowIsAgainstDigitisedDirection() {
                return forSnappedLinksWithDiscreteEndpoints(trafficFlowDirectionType = 3)
                    .checkAssert { snappedLink ->

                        assertThat(snappedLink.getNodeIdSequenceCombinations())
                            .isEqualTo(listOf(
                                NodeIdSequence(listOf(snappedLink.endNodeId, snappedLink.startNodeId))))
                    }
            }

            @Nested
            @DisplayName("When link is bidirectional")
            inner class WhenLinkIsBidirectional {

                @Test
                @DisplayName("When distance to start node is less than or equal to distance to end node")
                fun whenDistanceToStartNodeIsLessThanOrEqualToDistanceToEndNode() {
                    return forSnappedLinksWithDiscreteEndpoints(trafficFlowDirectionType = 2,
                                                                START_NODE_CLOSER_OR_EQUAL_DISTANCE)
                        .checkAssert { snappedLink ->
                            val nodeIdSequences: List<NodeIdSequence> = snappedLink.getNodeIdSequenceCombinations()

                            assertThat(nodeIdSequences)
                                .isEqualTo(listOf(
                                    NodeIdSequence(listOf(snappedLink.startNodeId, snappedLink.endNodeId)),
                                    NodeIdSequence(listOf(snappedLink.endNodeId, snappedLink.startNodeId))))
                        }
                }

                @Test
                @DisplayName("When distance to start node is greater than distance to end node")
                fun whenDistanceToStartNodeIsGreaterThanDistanceToEndNode() {
                    return forSnappedLinksWithDiscreteEndpoints(trafficFlowDirectionType = 2,
                                                                END_NODE_CLOSER)
                        .checkAssert { snappedLink ->
                            val nodeIdSequences: List<NodeIdSequence> = snappedLink.getNodeIdSequenceCombinations()

                            assertThat(nodeIdSequences)
                                .isEqualTo(listOf(
                                    NodeIdSequence(listOf(snappedLink.endNodeId, snappedLink.startNodeId)),
                                    NodeIdSequence(listOf(snappedLink.startNodeId, snappedLink.endNodeId))))
                        }
                }
            }
        }

        @Nested
        @DisplayName("When single node appears at both endpoints")
        inner class WhenSingleNodeAppearsAtBothEndpoints {

            @Test
            @DisplayName("Should return NodeIdSequence consisting of single node ID independent of node distances")
            fun independentOfNodeDistances() {
                return qt()
                    .forAll(SnappedLinkStateGenerator.snapLink(withDiscreteEndpoints = false))
                    .checkAssert { snappedLink ->
                        val nodeIdSequences: List<NodeIdSequence> = snappedLink.getNodeIdSequenceCombinations()

                        assertThat(nodeIdSequences)
                            .isEqualTo(listOf(NodeIdSequence(listOf(snappedLink.startNodeId))))
                    }
            }
        }
    }
}
