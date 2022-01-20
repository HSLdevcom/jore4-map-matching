package fi.hsl.jore4.mapmatching.service.node

import fi.hsl.jore4.mapmatching.model.InfrastructureNodeId
import fi.hsl.jore4.mapmatching.model.NodeIdSequence
import fi.hsl.jore4.mapmatching.service.node.NodeResolutionParamsGenerator.TerminusLinkRelation
import fi.hsl.jore4.mapmatching.service.node.NodeResolutionParamsGenerator.ViaNodeGenerationScheme
import fi.hsl.jore4.mapmatching.service.node.SnappedLinkStateExtension.toNodeIdList
import fi.hsl.jore4.mapmatching.test.util.ConsumerStackTracePrinter
import fi.hsl.jore4.mapmatching.util.CollectionUtils.filterOutConsecutiveDuplicates
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.quicktheories.QuickTheory.qt
import org.quicktheories.dsl.TheoryBuilder
import java.util.function.Consumer

@DisplayName("Test NodeSequenceAlternativesCreator class")
class NodeSequenceAlternativesCreatorTest {

    @Test
    @DisplayName("Verify ID of start infrastructure link")
    fun verifyStartLinkId() {
        forAllKindOfInputs()
            .checkAssert { input: NodeResolutionParams ->

                assertThat(createOutput(input))
                    .extracting { it.startLinkId }
                    .isEqualTo(input.startLink.infrastructureLinkId)
            }
    }

    @Test
    @DisplayName("Verify ID of end infrastructure link")
    fun verifyEndLinkId() {
        forAllKindOfInputs()
            .checkAssert { input: NodeResolutionParams ->

                assertThat(createOutput(input))
                    .extracting { it.endLinkId }
                    .isEqualTo(input.endLink.infrastructureLinkId)
            }
    }

    @Test
    @DisplayName("Verify that count of node ID sequences is between one and four")
    fun verifyNumberOfNodeIdSequences() {
        forAllKindOfInputs()
            .checkAssert { input: NodeResolutionParams ->

                assertThat(createOutput(input))
                    .extracting { it.nodeIdSequences }
                    .asList()
                    .hasSizeGreaterThanOrEqualTo(1)
                    .hasSizeLessThanOrEqualTo(4)
            }
    }

    @Test
    @DisplayName("Verify that each alternative node ID sequence contains at least two items")
    fun eachNodeIdSequenceShouldContainAtLeastTwoItems() {
        forAllKindOfInputs()
            .checkAssert { input: NodeResolutionParams ->

                val output: NodeSequenceAlternatives = createOutput(input)

                assertThat(output.nodeIdSequences)
                    .allMatch { it.size >= 2 }
            }
    }

    @Test
    @DisplayName("Verify that each alternative node ID sequence starts with node IDs of start link")
    fun eachNodeIdSequenceShouldStartWithNodeIdsOfStartLink() {
        forAllKindOfInputs()
            .checkAssert { input: NodeResolutionParams ->

                val output: NodeSequenceAlternatives = createOutput(input)

                assertThat(output.nodeIdSequences)
                    .allMatch { nodeIdSeq ->
                        val firstTwoItems: List<InfrastructureNodeId> = nodeIdSeq.list.take(2)

                        val startLinkAsNodeIdList: List<InfrastructureNodeId> = input.startLink.toNodeIdList()

                        firstTwoItems == startLinkAsNodeIdList || firstTwoItems == startLinkAsNodeIdList.reversed()
                    }
            }
    }

    @Test
    @DisplayName("Verify that each alternative node ID sequence ends with node IDs of end link")
    fun eachNodeIdSequenceShouldEndWithNodeIdsOfEndLink() {
        forAllKindOfInputs()
            .checkAssert { input: NodeResolutionParams ->

                val output: NodeSequenceAlternatives = createOutput(input)

                assertThat(output.nodeIdSequences)
                    .allMatch { nodeIdSeq ->
                        val lastTwoItems: List<InfrastructureNodeId> = nodeIdSeq.list.takeLast(2)

                        val endLinkAsNodeIdList: List<InfrastructureNodeId> = input.endLink.toNodeIdList()

                        lastTwoItems == endLinkAsNodeIdList || lastTwoItems == endLinkAsNodeIdList.reversed()
                    }
            }
    }

    @Nested
    @DisplayName("When list of given via nodes is empty")
    inner class WhenListOfGivenViaNodesIsEmpty {

        private fun forEmptyViaNodeInputs(terminusLinkRelation: TerminusLinkRelation)
            : TheoryBuilder<NodeResolutionParams> = forInputs(terminusLinkRelation, ViaNodeGenerationScheme.EMPTY)

        @Test
        @DisplayName("Sequence of via node IDs should be empty")
        fun viaNodeIdsShouldBeEmpty() {
            forEmptyViaNodeInputs(TerminusLinkRelation.ANY)
                .checkAssert { input: NodeResolutionParams ->

                    assertThat(createOutput(input))
                        .extracting { it.viaNodeIds }
                        .isEqualTo(NodeIdSequence.empty())
                }
        }

        @Nested
        @DisplayName("When given only one infrastructure link")
        inner class WhenGivenOnlyOneLink {

            private fun forSingleLinkInputs(): TheoryBuilder<NodeResolutionParams> =
                forEmptyViaNodeInputs(TerminusLinkRelation.SAME)

            @Test
            @DisplayName("There should be only one node ID sequence in the list")
            fun thereShouldBeOnlyOneNodeIdSequence() {
                forSingleLinkInputs()
                    .checkAssert { input: NodeResolutionParams ->

                        assertThat(createOutput(input))
                            .extracting { it.nodeIdSequences }
                            .asList()
                            .hasSize(1)
                    }
            }

            @Test
            @DisplayName("Verify that node ID sequence contains exactly two items")
            fun nodeIdSequenceShouldContainExactlyTwoItems() {
                forSingleLinkInputs()
                    .checkAssert { input: NodeResolutionParams ->

                        val output: NodeSequenceAlternatives = createOutput(input)

                        assertThat(output.nodeIdSequences)
                            .allMatch { it.size == 2 }
                    }
            }

            @Test
            @DisplayName("The one and only node ID sequence should contain endpoints of snapped link in order")
            fun verifyNodeIdSequence() {
                forSingleLinkInputs()
                    .checkAssert { input: NodeResolutionParams ->

                        val snappedLink = input.startLink

                        assertThat(createOutput(input))
                            .extracting { it.nodeIdSequences }
                            .asList()
                            .element(0)
                            .isEqualTo(NodeIdSequence(listOf(snappedLink.closerNodeId, snappedLink.furtherNodeId)))
                    }
            }
        }

        @Nested
        @DisplayName("When given two distinct infrastructure links")
        inner class WhenGivenTwoDistinctLinks {

            private fun forDistinctLinkInputs(): TheoryBuilder<NodeResolutionParams> =
                forEmptyViaNodeInputs(TerminusLinkRelation.DISTINCT)

            @Test
            @DisplayName("List of alternative node ID sequences should consist of four unique sequences")
            fun thereShouldBeFourUniqueNodeIdSequences() {
                forDistinctLinkInputs()
                    .checkAssert { input: NodeResolutionParams ->

                        assertThat(createOutput(input))
                            .extracting { it.nodeIdSequences }
                            .asList()
                            .hasSize(4)
                            .doesNotHaveDuplicates()
                    }
            }

            @Test
            @DisplayName("Verify 1st sequence of node IDs")
            fun verifyFirstSequenceOfNodeIds() {
                forDistinctLinkInputs()
                    .checkAssert { input: NodeResolutionParams ->

                        assertThat(createOutput(input))
                            .extracting { it.nodeIdSequences }
                            .asList()
                            .hasSizeGreaterThanOrEqualTo(1)
                            .element(0)
                            .isEqualTo(NodeIdSequence(
                                filterOutConsecutiveDuplicates(
                                    listOf(input.startLink.closerNodeId,
                                           input.startLink.furtherNodeId,
                                           input.endLink.closerNodeId,
                                           input.endLink.furtherNodeId))))
                    }
            }

            @Test
            @DisplayName("Verify 2nd sequence of node IDs")
            fun verifySecondSequenceOfNodeIds() {
                forDistinctLinkInputs()
                    .checkAssert { input: NodeResolutionParams ->

                        assertThat(createOutput(input))
                            .extracting { it.nodeIdSequences }
                            .asList()
                            .hasSizeGreaterThanOrEqualTo(2)
                            .element(1)
                            .isEqualTo(NodeIdSequence(
                                filterOutConsecutiveDuplicates(
                                    listOf(input.startLink.closerNodeId,
                                           input.startLink.furtherNodeId,
                                           input.endLink.furtherNodeId,
                                           input.endLink.closerNodeId))))
                    }
            }

            @Test
            @DisplayName("Verify 3rd sequence of node IDs")
            fun verifyThirdSequenceOfNodeIds() {
                forDistinctLinkInputs()
                    .checkAssert { input: NodeResolutionParams ->

                        assertThat(createOutput(input))
                            .extracting { it.nodeIdSequences }
                            .asList()
                            .hasSizeGreaterThanOrEqualTo(3)
                            .element(2)
                            .isEqualTo(NodeIdSequence(
                                filterOutConsecutiveDuplicates(
                                    listOf(input.startLink.furtherNodeId,
                                           input.startLink.closerNodeId,
                                           input.endLink.closerNodeId,
                                           input.endLink.furtherNodeId))))
                    }
            }

            @Test
            @DisplayName("Verify 4th sequence of node IDs")
            fun verifyFourthSequenceOfNodeIds() {
                forDistinctLinkInputs()
                    .checkAssert { input: NodeResolutionParams ->

                        assertThat(createOutput(input))
                            .extracting { it.nodeIdSequences }
                            .asList()
                            .hasSizeGreaterThanOrEqualTo(4)
                            .element(3)
                            .isEqualTo(NodeIdSequence(
                                filterOutConsecutiveDuplicates(
                                    listOf(input.startLink.furtherNodeId,
                                           input.startLink.closerNodeId,
                                           input.endLink.furtherNodeId,
                                           input.endLink.closerNodeId))))
                    }
            }

            @Nested
            @DisplayName("When two infrastructure links have common node")
            inner class WhenTwoLinksHaveCommonNode {

                private fun forConnectedTerminusLinkInputs(): TheoryBuilder<NodeResolutionParams> =
                    forEmptyViaNodeInputs(TerminusLinkRelation.CONNECTED)

                @Test
                @DisplayName("There should be one node ID sequence containing three items")
                fun verifyCountOfNodeIdSequencesHavingThreeItems() {
                    forConnectedTerminusLinkInputs()
                        .checkAssert { input: NodeResolutionParams ->

                            val output: NodeSequenceAlternatives = createOutput(input)
                            val numberOf3NodeIdSequences = output.nodeIdSequences.count { it.size == 3 }

                            assertThat(numberOf3NodeIdSequences)
                                .isEqualTo(1)
                        }
                }

                @Test
                @DisplayName("There should be three node ID sequences containing four items")
                fun verifyCountOfNodeIdSequencesHavingFourItems() {
                    forConnectedTerminusLinkInputs()
                        .checkAssert { input: NodeResolutionParams ->

                            val output: NodeSequenceAlternatives = createOutput(input)
                            val numberOf4NodeIdSequences = output.nodeIdSequences.count { it.size == 4 }

                            assertThat(numberOf4NodeIdSequences)
                                .isEqualTo(3)
                        }
                }
            }

            @Nested
            @DisplayName("When two infrastructure links do not have common node")
            inner class WhenTwoLinksDoNotShareCommonNode {

                private fun forUnconnectedTerminusLinkInputs(): TheoryBuilder<NodeResolutionParams> =
                    forEmptyViaNodeInputs(TerminusLinkRelation.UNCONNECTED)

                @Test
                @DisplayName("There should be four node ID sequences containing four items")
                fun verifyCountOfNodeIdSequencesHavingFourItems() {
                    forUnconnectedTerminusLinkInputs()
                        .checkAssert { input: NodeResolutionParams ->

                            val output: NodeSequenceAlternatives = createOutput(input)
                            val numberOf3NodeIdSequences = output.nodeIdSequences.count { it.size == 4 }

                            assertThat(numberOf3NodeIdSequences)
                                .isEqualTo(4)
                        }
                }
            }
        }
    }

    @Nested
    @DisplayName("When list of given via nodes is not empty")
    inner class WhenListOfGivenViaNodesIsNotEmpty {

        @Nested
        @DisplayName("When sequence of via nodes is fully redundant with regard to terminus links")
        inner class WhenViaNodesAreFullyRedundantWithTerminusLinks {

            private fun forFullyRedundantViaNodeInputs(terminusLinkRelation: TerminusLinkRelation)
                : TheoryBuilder<NodeResolutionParams> = forInputs(terminusLinkRelation,
                                                                  ViaNodeGenerationScheme.FULLY_REDUNDANT_WITH_TERMINUS_LINKS)

            @Test
            @DisplayName("Sequence of via node IDs should be empty")
            fun viaNodeIdsShouldBeEmpty() {
                forFullyRedundantViaNodeInputs(TerminusLinkRelation.ANY)
                    .checkAssert { input: NodeResolutionParams ->

                        assertThat(createOutput(input))
                            .extracting { it.viaNodeIds.list }
                            .asList()
                            .isEmpty()
                    }
            }

            @Nested
            @DisplayName("When given only one infrastructure link as terminus link")
            inner class WhenGivenSingleTerminusLink {

                @Test
                @DisplayName("List of node ID sequences should consist of only one list")
                fun thereShouldBeOnlyOneNodeIdSequence() {
                    forFullyRedundantViaNodeInputs(TerminusLinkRelation.SAME)
                        .checkAssert { input: NodeResolutionParams ->

                            assertThat(createOutput(input))
                                .extracting { it.nodeIdSequences }
                                .asList()
                                .hasSize(1)
                        }
                }
            }
        }

        @Nested
        @DisplayName("When sequence of via nodes is not redundant with regard to terminus links")
        inner class WhenViaNodesAreNonRedundantWithTerminusLinks {

            private fun forNonRedundantViaNodeInputs(): TheoryBuilder<NodeResolutionParams> =
                forInputs(TerminusLinkRelation.ANY,
                          ViaNodeGenerationScheme.NON_REDUNDANT_WITH_TERMINUS_LINKS)

            @Test
            @DisplayName("Sequence of via node IDs should not be empty")
            fun viaNodeIdsShouldNotBeEmpty() {
                forNonRedundantViaNodeInputs()
                    .checkAssert(traceException { input: NodeResolutionParams ->

                        assertThat(createOutput(input))
                            .extracting { it.viaNodeIds.list }
                            .asList()
                            .isNotEmpty()
                    })
            }

            @Test
            @DisplayName("Check validity of via node ID sequence")
            fun verifyViaNodeIdSequence() {
                forNonRedundantViaNodeInputs()
                    .checkAssert(traceException { input: NodeResolutionParams ->

                        val expectedViaNodeIds: List<InfrastructureNodeId> = filterOutConsecutiveDuplicates(
                            input.viaNodeResolvers.map { it.getInfrastructureNodeId() }
                        )

                        assertThat(createOutput(input))
                            .extracting { it.viaNodeIds.list }
                            .asList()
                            .isEqualTo(expectedViaNodeIds)
                    })
            }

            @Test
            @DisplayName("Verify that each alternative node ID sequence contains via node IDs in the middle")
            fun verifyEachNodeIdSequenceContainsViaNodeIdsInTheMiddle() {
                forNonRedundantViaNodeInputs()
                    .checkAssert(traceException { input: NodeResolutionParams ->

                        val output: NodeSequenceAlternatives = createOutput(input)

                        val viaNodeIds: List<InfrastructureNodeId> = output.viaNodeIds.list

                        assertThat(output.nodeIdSequences)
                            .allMatch { nodeIdSeq: NodeIdSequence ->

                                // try 2 (links) X 2 (endpoint nodes) = 4 different candidates
                                listOf(1, 2)
                                    .flatMap { dropFromStart ->
                                        listOf(1, 2).map { dropFromEnd ->
                                            nodeIdSeq.list.drop(dropFromStart).dropLast(dropFromEnd)
                                        }
                                    }
                                    .any { it == viaNodeIds }
                            }
                    })
            }
        }
    }

    companion object {

        private fun createOutput(input: NodeResolutionParams): NodeSequenceAlternatives =
            NodeSequenceAlternativesCreator.create(input.startLink,
                                                   input.viaNodeResolvers,
                                                   input.endLink)

        private fun forInputs(terminusLinkRelation: TerminusLinkRelation,
                              viaNodeGenerationScheme: ViaNodeGenerationScheme): TheoryBuilder<NodeResolutionParams> {

            return qt()
                .forAll(
                    NodeResolutionParamsGenerator
                        .builder()
                        .withStartLinkRelatedToEndLink(terminusLinkRelation)
                        .withViaNodeGenerationScheme(viaNodeGenerationScheme)
                        .build()
                        .describedAs { params ->
                            val viaNodeIds: List<Long> =
                                params.viaNodeResolvers.map { it.getInfrastructureNodeId().value }

                            // Make assertion failures more readable.
                            "{\n" +
                                "    startLink: {\n" +
                                "        id: ${params.startLink.infrastructureLinkId.value},\n" +
                                "        closestDistance: ${params.startLink.closestDistance},\n" +
                                "        startNodeId: ${params.startLink.startNode.id.value},\n" +
                                "        startNodeDistance: ${params.startLink.startNode.distanceToNode},\n" +
                                "        endNodeId: ${params.startLink.endNode.id.value},\n" +
                                "        endNodeDistance: ${params.startLink.endNode.distanceToNode}\n" +
                                "    },\n" +
                                "    viaNodeIds: $viaNodeIds,\n" +
                                "    endLink: {\n" +
                                "        id: ${params.endLink.infrastructureLinkId.value},\n" +
                                "        closestDistance: ${params.endLink.closestDistance},\n" +
                                "        startNodeId: ${params.endLink.startNode.id.value},\n" +
                                "        startNodeDistance: ${params.endLink.startNode.distanceToNode},\n" +
                                "        endNodeId: ${params.endLink.endNode.id.value},\n" +
                                "        endNodeDistance: ${params.endLink.endNode.distanceToNode}\n" +
                                "    }\n" +
                                "}"
                        }
                )
        }

        private fun forAllKindOfInputs(): TheoryBuilder<NodeResolutionParams> =
            forInputs(TerminusLinkRelation.ANY, ViaNodeGenerationScheme.ANY)

        private fun <T> traceException(consumer: Consumer<T>): Consumer<T> = ConsumerStackTracePrinter(consumer)
    }
}
