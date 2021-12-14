package fi.hsl.jore4.mapmatching.service.node

import fi.hsl.jore4.mapmatching.util.CollectionUtils.filterOutConsecutiveDuplicates
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.quicktheories.QuickTheory.qt
import org.quicktheories.core.Gen
import org.quicktheories.dsl.TheoryBuilder

@DisplayName("Test NodeSequenceAlternativesCreator class")
class NodeSequenceAlternativesCreatorTest {

    private data class Result(val input: NodeResolutionParams, val output: NodeSequenceAlternatives)

    @Test
    @DisplayName("Verify ID of start infrastructure link")
    fun verifyStartLinkId() {
        withResultForAllKindOfInputs().checkAssert { (input: NodeResolutionParams, output: NodeSequenceAlternatives) ->

            assertThat(output)
                .extracting { it.startLinkId }
                .isEqualTo(input.startLink.infrastructureLinkId)
        }
    }

    @Test
    @DisplayName("Verify ID of end infrastructure link")
    fun verifyEndLinkId() {
        withResultForAllKindOfInputs().checkAssert { (input: NodeResolutionParams, output: NodeSequenceAlternatives) ->

            assertThat(output)
                .extracting { it.endLinkId }
                .isEqualTo(input.endLink.infrastructureLinkId)
        }
    }

    @Nested
    @DisplayName("When list of via node IDs is empty")
    inner class WhenListOfViaNodesIsEmpty {

        private fun withResultForInputWithoutViaNodes(): TheoryBuilder<Result> = qt()
            .forAll(MIXED_RESULT_WITHOUT_VIA_NODES)

        @Test
        @DisplayName("List of via node IDs should be empty")
        fun listOfViaNodeIdsShouldBeEmpty() {
            withResultForInputWithoutViaNodes()
                .checkAssert { (_: NodeResolutionParams, output: NodeSequenceAlternatives) ->

                assertThat(output)
                    .extracting { it.viaNodeIds }
                    .asList()
                    .isEmpty()
            }
        }

        @Nested
        @DisplayName("When given only one infrastructure link")
        inner class WhenGivenOnlyOneLink {

            private fun withSingleLinkResult(): TheoryBuilder<Result> = qt()
                .forAll(RESULT_FOR_SINGLE_LINK_WITHOUT_VIA_NODES)

            @Test
            @DisplayName("List of node ID sequences should consist of only one list")
            fun thereShouldBeOnlyOneNodeIdSequence() {
                withSingleLinkResult().checkAssert { (_: NodeResolutionParams, output: NodeSequenceAlternatives) ->

                    assertThat(output)
                        .extracting { it.nodeIdSequences }
                        .asList()
                        .hasSize(1)
                }
            }

            @Test
            @DisplayName("The only available node ID sequence should contain endpoints of link in order")
            fun verifyNodeIdSequence() {
                withSingleLinkResult().checkAssert { (input: NodeResolutionParams, output: NodeSequenceAlternatives) ->

                    val singleLink = input.startLink

                    assertThat(output)
                        .extracting { it.nodeIdSequences }
                        .asList()
                        .element(0)
                        .isEqualTo(listOf(singleLink.closerNodeId, singleLink.furtherNodeId))
                }
            }
        }

        @Nested
        @DisplayName("When given two distinct infrastructure links")
        inner class WhenGivenTwoDistinctLinks {

            private fun withResultForTwoLinks(): TheoryBuilder<Result> = qt()
                .forAll(MIXED_RESULT_FOR_TWO_LINKS_WITHOUT_VIA_NODES)

            @Test
            @DisplayName("List of node ID sequences should consist of four unique lists")
            fun thereShouldBeFourUniqueNodeIdSequences() {
                withResultForTwoLinks().checkAssert { (_: NodeResolutionParams, output: NodeSequenceAlternatives) ->

                    assertThat(output)
                        .extracting { it.nodeIdSequences }
                        .asList()
                        .hasSize(4)
                        .doesNotHaveDuplicates()
                }
            }

            @Test
            @DisplayName("Verify 1st sequence of node IDs")
            fun verifyFirstSequenceOfNodeIds() {
                withResultForTwoLinks().checkAssert { (input: NodeResolutionParams, output: NodeSequenceAlternatives) ->

                    assertThat(output)
                        .extracting { it.nodeIdSequences }
                        .asList()
                        .hasSizeGreaterThanOrEqualTo(1)
                        .element(0)
                        .isEqualTo(
                            filterOutConsecutiveDuplicates(
                                listOf(input.startLink.closerNodeId,
                                       input.startLink.furtherNodeId,
                                       input.endLink.closerNodeId,
                                       input.endLink.furtherNodeId)))
                }
            }

            @Test
            @DisplayName("Verify 2nd sequence of node IDs")
            fun verifySecondSequenceOfNodeIds() {
                withResultForTwoLinks().checkAssert { (input: NodeResolutionParams, output: NodeSequenceAlternatives) ->

                    assertThat(output)
                        .extracting { it.nodeIdSequences }
                        .asList()
                        .hasSizeGreaterThanOrEqualTo(2)
                        .element(1)
                        .isEqualTo(
                            filterOutConsecutiveDuplicates(
                                listOf(input.startLink.closerNodeId,
                                       input.startLink.furtherNodeId,
                                       input.endLink.furtherNodeId,
                                       input.endLink.closerNodeId)))
                }
            }

            @Test
            @DisplayName("Verify 3rd sequence of node IDs")
            fun verifyThirdSequenceOfNodeIds() {
                withResultForTwoLinks().checkAssert { (input: NodeResolutionParams, output: NodeSequenceAlternatives) ->

                    assertThat(output)
                        .extracting { it.nodeIdSequences }
                        .asList()
                        .hasSizeGreaterThanOrEqualTo(3)
                        .element(2)
                        .isEqualTo(
                            filterOutConsecutiveDuplicates(
                                listOf(input.startLink.furtherNodeId,
                                       input.startLink.closerNodeId,
                                       input.endLink.closerNodeId,
                                       input.endLink.furtherNodeId)))
                }
            }

            @Test
            @DisplayName("Verify 4th sequence of node IDs")
            fun verifyFourthSequenceOfNodeIds() {
                withResultForTwoLinks().checkAssert { (input: NodeResolutionParams, output: NodeSequenceAlternatives) ->

                    assertThat(output)
                        .extracting { it.nodeIdSequences }
                        .asList()
                        .hasSizeGreaterThanOrEqualTo(4)
                        .element(3)
                        .isEqualTo(
                            filterOutConsecutiveDuplicates(
                                listOf(input.startLink.furtherNodeId,
                                       input.startLink.closerNodeId,
                                       input.endLink.furtherNodeId,
                                       input.endLink.closerNodeId)))
                }
            }

            @Nested
            @DisplayName("When two infrastructure links have common node")
            inner class WhenTwoLinksHaveCommonNode {

                private fun withResultForTwoLinksSharingNode(): TheoryBuilder<Result> = qt()
                    .forAll(RESULT_FOR_TWO_CONNECTED_LINKS_WITHOUT_VIA_NODES)

                @Test
                @DisplayName("Count of sequences having three node IDs should be one")
                fun verifyCountOfSequencesHavingThreeNodeIds() {
                    withResultForTwoLinksSharingNode()
                        .checkAssert { (_: NodeResolutionParams, output: NodeSequenceAlternatives) ->

                            val numberOf3NodeIdSequences = output.nodeIdSequences.count { it.size == 3 }

                            assertThat(numberOf3NodeIdSequences)
                                .isEqualTo(1)
                        }
                }

                @Test
                @DisplayName("Count of sequences having four node IDs should be three")
                fun verifyCountOfSequencesHavingFourNodeIds() {
                    withResultForTwoLinksSharingNode()
                        .checkAssert { (_: NodeResolutionParams, output: NodeSequenceAlternatives) ->

                            val numberOf4NodeIdSequences = output.nodeIdSequences.count { it.size == 4 }

                            assertThat(numberOf4NodeIdSequences)
                                .isEqualTo(3)
                        }
                }
            }

            @Nested
            @DisplayName("When two infrastructure links do not have common node")
            inner class WhenTwoLinksDoNotShareCommonNode {

                private fun withResultForTwoUnconnectedLinks(): TheoryBuilder<Result> = qt()
                    .forAll(RESULT_FOR_TWO_UNCONNECTED_LINKS_WITHOUT_VIA_NODES)

                @Test
                @DisplayName("Count of sequences having four node IDs should be four")
                fun verifyCountOfSequencesHavingFourNodeIds() {
                    withResultForTwoUnconnectedLinks()
                        .checkAssert { (_: NodeResolutionParams, output: NodeSequenceAlternatives) ->

                            val numberOf3NodeIdSequences = output.nodeIdSequences.count { it.size == 4 }

                            assertThat(numberOf3NodeIdSequences)
                                .isEqualTo(4)
                        }
                }
            }
        }
    }

    companion object {

        // Single link

        private val RESULT_FOR_SINGLE_LINK_WITH_VIA_NODES: Gen<Result> = NodeResolutionParamsGenerator
            .SINGLE_LINK_WITH_VIA_NODES
            .map { input -> getResult(input) }

        private val RESULT_FOR_SINGLE_LINK_WITHOUT_VIA_NODES: Gen<Result> = NodeResolutionParamsGenerator
            .SINGLE_LINK_WITHOUT_VIA_NODES
            .map { input -> getResult(input) }

        // Two unconnected links (no common node)

        private val RESULT_FOR_TWO_UNCONNECTED_LINKS_WITH_VIA_NODES: Gen<Result> = NodeResolutionParamsGenerator
            .TWO_UNCONNECTED_LINKS_WITH_VIA_NODES
            .map { input -> getResult(input) }

        private val RESULT_FOR_TWO_UNCONNECTED_LINKS_WITHOUT_VIA_NODES: Gen<Result> = NodeResolutionParamsGenerator
            .TWO_UNCONNECTED_LINKS_WITHOUT_VIA_NODES
            .map { input -> getResult(input) }

        // Two connected links (with shared common node)

        private val RESULT_FOR_TWO_CONNECTED_LINKS_WITH_VIA_NODES: Gen<Result> = NodeResolutionParamsGenerator
            .TWO_CONNECTED_LINKS_WITH_VIA_NODES
            .map { input -> getResult(input) }

        private val RESULT_FOR_TWO_CONNECTED_LINKS_WITHOUT_VIA_NODES: Gen<Result> = NodeResolutionParamsGenerator
            .TWO_CONNECTED_LINKS_WITHOUT_VIA_NODES
            .map { input -> getResult(input) }

        // Mixed link pairs

        private val MIXED_RESULT_FOR_TWO_LINKS_WITH_VIA_NODES: Gen<Result> =
            RESULT_FOR_TWO_UNCONNECTED_LINKS_WITH_VIA_NODES
                .mix(RESULT_FOR_TWO_CONNECTED_LINKS_WITH_VIA_NODES, 50)

        private val MIXED_RESULT_FOR_TWO_LINKS_WITHOUT_VIA_NODES: Gen<Result> =
            RESULT_FOR_TWO_UNCONNECTED_LINKS_WITHOUT_VIA_NODES
                .mix(RESULT_FOR_TWO_CONNECTED_LINKS_WITHOUT_VIA_NODES, 50)

        // Mixed combos

        private val MIXED_RESULT_WITH_VIA_NODES: Gen<Result> =
            MIXED_RESULT_FOR_TWO_LINKS_WITH_VIA_NODES.mix(RESULT_FOR_SINGLE_LINK_WITH_VIA_NODES, 33)

        private val MIXED_RESULT_WITHOUT_VIA_NODES: Gen<Result> =
            MIXED_RESULT_FOR_TWO_LINKS_WITHOUT_VIA_NODES.mix(RESULT_FOR_SINGLE_LINK_WITHOUT_VIA_NODES, 33)

        // All kind of combinations

        private val MIXED_RESULT_FOR_ALL_KIND_OF_INPUTS: Gen<Result> =
            MIXED_RESULT_WITH_VIA_NODES.mix(MIXED_RESULT_WITHOUT_VIA_NODES, 25)

        // Helper methods

        private fun getResult(input: NodeResolutionParams): Result {
            val output: NodeSequenceAlternatives =
                NodeSequenceAlternativesCreator.create(input.startLink, input.viaNodeResolvers, input.endLink)

            return Result(input, output)
        }

        private fun withResultForAllKindOfInputs(): TheoryBuilder<Result> = qt()
            .forAll(MIXED_RESULT_FOR_ALL_KIND_OF_INPUTS)
    }
}
