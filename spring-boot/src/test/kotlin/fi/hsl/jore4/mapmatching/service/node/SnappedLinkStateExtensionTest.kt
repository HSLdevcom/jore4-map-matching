package fi.hsl.jore4.mapmatching.service.node

import fi.hsl.jore4.mapmatching.model.InfrastructureNodeId
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

    private fun withSnappedLink(nodeProximityFilter: LinkEndpointsProximityFilter): TheoryBuilder<SnappedLinkState> =
        qt().forAll(SnappedLinkStateGenerator.snapLink(nodeProximityFilter))

    @Nested
    @DisplayName("toNodeList")
    inner class ToNodeIdList {

        @Test
        @DisplayName("When distance to start node is less than or equal to distance to end node")
        fun whenDistanceToStartNodeIsLessThanOrEqualToDistanceToEndNode() {
            return withSnappedLink(START_NODE_CLOSER_OR_EQUAL_DISTANCE)
                .checkAssert { snappedLink ->
                    val nodeIds: List<InfrastructureNodeId> = snappedLink.toNodeIdList()

                    assertThat(nodeIds)
                        .isEqualTo(listOf(snappedLink.startNode.id, snappedLink.endNode.id))
                }
        }

        @Test
        @DisplayName("When distance to start node is greater than distance to end node")
        fun whenDistanceToStartNodeIsGreaterThanDistanceToEndNode() {
            return withSnappedLink(END_NODE_CLOSER)
                .checkAssert { snappedLink ->
                    val nodeIds: List<InfrastructureNodeId> = snappedLink.toNodeIdList()

                    assertThat(nodeIds)
                        .isEqualTo(listOf(snappedLink.endNode.id, snappedLink.startNode.id))
                }
        }
    }

    @Nested
    @DisplayName("Get node ID sequence combinations")
    inner class GetNodeIdSequenceCombinations {

        @Test
        @DisplayName("When distance to start node is less than or equal to distance to end node")
        fun whenDistanceToStartNodeIsLessThanOrEqualToDistanceToEndNode() {
            return withSnappedLink(START_NODE_CLOSER_OR_EQUAL_DISTANCE)
                .checkAssert { snappedLink ->
                    val nodeIdSequences: List<NodeIdSequence> = snappedLink.getNodeIdSequenceCombinations()

                    assertThat(nodeIdSequences)
                        .isEqualTo(listOf(
                            NodeIdSequence(listOf(snappedLink.startNode.id, snappedLink.endNode.id)),
                            NodeIdSequence(listOf(snappedLink.endNode.id, snappedLink.startNode.id))))
                }
        }

        @Test
        @DisplayName("When distance to start node is greater than distance to end node")
        fun whenDistanceToStartNodeIsGreaterThanDistanceToEndNode() {
            return withSnappedLink(END_NODE_CLOSER)
                .checkAssert { snappedLink ->
                    val nodeIdSequences: List<NodeIdSequence> = snappedLink.getNodeIdSequenceCombinations()

                    assertThat(nodeIdSequences)
                        .isEqualTo(listOf(
                            NodeIdSequence(listOf(snappedLink.endNode.id, snappedLink.startNode.id)),
                            NodeIdSequence(listOf(snappedLink.startNode.id, snappedLink.endNode.id))))
                }
        }
    }
}
