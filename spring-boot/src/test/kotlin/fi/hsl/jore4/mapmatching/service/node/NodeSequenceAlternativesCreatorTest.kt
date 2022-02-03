package fi.hsl.jore4.mapmatching.service.node

import fi.hsl.jore4.mapmatching.model.HasInfrastructureNodeId
import fi.hsl.jore4.mapmatching.model.InfrastructureNodeId
import fi.hsl.jore4.mapmatching.model.NodeIdSequence
import fi.hsl.jore4.mapmatching.service.node.NodeResolutionParamsGenerator.TerminusLinkEndpointDiscreteness
import fi.hsl.jore4.mapmatching.service.node.NodeResolutionParamsGenerator.TerminusLinkEndpointDiscreteness.DISCRETE_ENDPOINT_NODES
import fi.hsl.jore4.mapmatching.service.node.NodeResolutionParamsGenerator.TerminusLinkEndpointDiscreteness.NON_DISCRETE_ENDPOINT_NODES
import fi.hsl.jore4.mapmatching.service.node.NodeResolutionParamsGenerator.TerminusLinkRelation
import fi.hsl.jore4.mapmatching.service.node.NodeResolutionParamsGenerator.TerminusLinkRelation.DISCRETE_LINKS
import fi.hsl.jore4.mapmatching.service.node.NodeResolutionParamsGenerator.TerminusLinkRelation.DISCRETE_LINKS_CONNECTED
import fi.hsl.jore4.mapmatching.service.node.NodeResolutionParamsGenerator.TerminusLinkRelation.DISCRETE_LINKS_UNCONNECTED
import fi.hsl.jore4.mapmatching.service.node.NodeResolutionParamsGenerator.TerminusLinkRelation.SAME
import fi.hsl.jore4.mapmatching.service.node.NodeResolutionParamsGenerator.ViaNodeGenerationScheme
import fi.hsl.jore4.mapmatching.service.node.NodeResolutionParamsGenerator.ViaNodeGenerationScheme.EMPTY
import fi.hsl.jore4.mapmatching.service.node.NodeResolutionParamsGenerator.ViaNodeGenerationScheme.FULLY_REDUNDANT_WITH_TERMINUS_LINKS
import fi.hsl.jore4.mapmatching.service.node.NodeResolutionParamsGenerator.ViaNodeGenerationScheme.NON_REDUNDANT_WITH_TERMINUS_LINKS
import fi.hsl.jore4.mapmatching.service.node.SnappedLinkStateExtension.toNodeIdList
import fi.hsl.jore4.mapmatching.util.CollectionUtils.filterOutConsecutiveDuplicates
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.quicktheories.QuickTheory.qt
import org.quicktheories.dsl.TheoryBuilder

@DisplayName("Test NodeSequenceAlternativesCreator class")
class NodeSequenceAlternativesCreatorTest {

    @Test
    @DisplayName("Verify ID of start infrastructure link")
    fun verifyStartLinkId() {
        forAllInputs()
            .checkAssert { input: NodeResolutionParams ->

                assertThat(createOutput(input))
                    .extracting(NodeSequenceAlternatives::startLinkId)
                    .isEqualTo(input.startLink.infrastructureLinkId)
            }
    }

    @Test
    @DisplayName("Verify ID of end infrastructure link")
    fun verifyEndLinkId() {
        forAllInputs()
            .checkAssert { input: NodeResolutionParams ->

                assertThat(createOutput(input))
                    .extracting(NodeSequenceAlternatives::endLinkId)
                    .isEqualTo(input.endLink.infrastructureLinkId)
            }
    }

    @Test
    @DisplayName("Verify that count of node ID sequences is between one and four")
    fun verifyNumberOfNodeIdSequences() {
        forAllInputs()
            .checkAssert { input: NodeResolutionParams ->

                assertThat(createOutput(input))
                    .extracting(NodeSequenceAlternatives::nodeIdSequences)
                    .asList()
                    .hasSizeGreaterThanOrEqualTo(1)
                    .hasSizeLessThanOrEqualTo(4)
            }
    }

    @Test
    @DisplayName("Verify that each alternative node ID sequence contains at least one item")
    fun eachNodeIdSequenceShouldContainAtLeastOneItem() {
        forAllInputs()
            .checkAssert { input: NodeResolutionParams ->

                val output: NodeSequenceAlternatives = createOutput(input)

                assertThat(output.nodeIdSequences)
                    .allMatch { it.size >= 1 }
            }
    }

    @Test
    @DisplayName("Verify that each alternative node ID sequence starts with either node ID of start link")
    fun eachNodeIdSequenceShouldStartWithEitherNodeIdOfStartLink() {
        forAllInputs()
            .checkAssert { input: NodeResolutionParams ->

                val output: NodeSequenceAlternatives = createOutput(input)

                assertThat(output.nodeIdSequences)
                    .allMatch { nodeIdSeq ->
                        input.startLink.hasNode(nodeIdSeq.firstNodeOrThrow())
                    }
            }
    }

    @Test
    @DisplayName("Verify that each alternative node ID sequence ends with either node ID of end link")
    fun eachNodeIdSequenceShouldEndWithEitherNodeIdOfEndLink() {
        forAllInputs()
            .checkAssert { input: NodeResolutionParams ->

                val output: NodeSequenceAlternatives = createOutput(input)

                assertThat(output.nodeIdSequences)
                    .allMatch { nodeIdSeq ->
                        input.endLink.hasNode(nodeIdSeq.lastNodeOrThrow())
                    }
            }
    }

    @Nested
    @DisplayName("When terminus links have non-discrete endpoint nodes i.e. they start from and end at same node")
    inner class WhenBothTerminusLinksStartFromAndEndAtSameNode {

        private fun forInputs(): TheoryBuilder<NodeResolutionParams> = forInputs(TerminusLinkRelation.ANY,
                                                                                 NON_DISCRETE_ENDPOINT_NODES,
                                                                                 ViaNodeGenerationScheme.ANY)

        @Test
        @DisplayName("Verify that there is only one node ID sequence")
        fun thereShouldBeExactlyOneNodeIdSequence() {
            forInputs()
                .checkAssert { input: NodeResolutionParams ->

                    assertThat(createOutput(input))
                        .extracting(NodeSequenceAlternatives::nodeIdSequences)
                        .asList()
                        .hasSize(1)
                }
        }
    }

    @Nested
    @DisplayName("When terminus links have discrete endpoint nodes")
    inner class WhenTerminusLinksHaveDiscreteEndpoints {

        private fun forInputs(): TheoryBuilder<NodeResolutionParams> = forInputs(TerminusLinkRelation.ANY,
                                                                                 DISCRETE_ENDPOINT_NODES,
                                                                                 ViaNodeGenerationScheme.ANY)

        @Test
        @DisplayName("Verify that each alternative node ID sequence contains at least two items")
        fun eachNodeIdSequenceShouldContainAtLeastTwoItems() {
            forInputs()
                .checkAssert { input: NodeResolutionParams ->

                    val output: NodeSequenceAlternatives = createOutput(input)

                    assertThat(output.nodeIdSequences)
                        .allMatch { it.size >= 2 }
                }
        }

        @Test
        @DisplayName("Verify that each alternative node ID sequence starts with two node IDs from start link")
        fun eachNodeIdSequenceShouldStartWithNodeIdsOfStartLink() {
            forInputs()
                .checkAssert { input: NodeResolutionParams ->

                    val output: NodeSequenceAlternatives = createOutput(input)

                    assertThat(output.nodeIdSequences)
                        .allMatch { nodeIdSeq ->
                            val firstTwoItems: Set<InfrastructureNodeId> = nodeIdSeq.list.take(2).toSet()
                            val startLinkAsNodeIdSet: Set<InfrastructureNodeId> = input.startLink.toNodeIdList().toSet()

                            firstTwoItems == startLinkAsNodeIdSet
                        }
                }
        }

        @Test
        @DisplayName("Verify that each alternative node ID sequence ends with two node IDs from end link")
        fun eachNodeIdSequenceShouldEndWithNodeIdsOfEndLink() {
            forInputs()
                .checkAssert { input: NodeResolutionParams ->

                    val output: NodeSequenceAlternatives = createOutput(input)

                    assertThat(output.nodeIdSequences)
                        .allMatch { nodeIdSeq ->
                            val lastTwoItems: Set<InfrastructureNodeId> = nodeIdSeq.list.takeLast(2).toSet()
                            val endLinkAsNodeIdSet: Set<InfrastructureNodeId> = input.endLink.toNodeIdList().toSet()

                            lastTwoItems == endLinkAsNodeIdSet
                        }
                }
        }

        @Nested
        @DisplayName("When list of given via nodes is empty")
        inner class WhenListOfGivenViaNodesIsEmpty {

            @Nested
            @DisplayName("When given two discrete infrastructure links")
            inner class WhenGivenTwoDiscreteLinks {

                private fun forInputs(): TheoryBuilder<NodeResolutionParams> = forInputs(DISCRETE_LINKS,
                                                                                         DISCRETE_ENDPOINT_NODES,
                                                                                         EMPTY)

                @Test
                @DisplayName("List of alternative node ID sequences should consist of four unique sequences")
                fun thereShouldBeFourUniqueNodeIdSequences() {
                    forInputs()
                        .checkAssert { input: NodeResolutionParams ->

                            assertThat(createOutput(input))
                                .extracting(NodeSequenceAlternatives::nodeIdSequences)
                                .asList()
                                .hasSize(4)
                                .doesNotHaveDuplicates()
                        }
                }

                @Test
                @DisplayName("Verify 1st sequence of node IDs")
                fun verifyFirstSequenceOfNodeIds() {
                    forInputs()
                        .checkAssert { input: NodeResolutionParams ->

                            assertThat(createOutput(input))
                                .extracting(NodeSequenceAlternatives::nodeIdSequences)
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
                    forInputs()
                        .checkAssert { input: NodeResolutionParams ->

                            assertThat(createOutput(input))
                                .extracting(NodeSequenceAlternatives::nodeIdSequences)
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
                    forInputs()
                        .checkAssert { input: NodeResolutionParams ->

                            assertThat(createOutput(input))
                                .extracting(NodeSequenceAlternatives::nodeIdSequences)
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
                    forInputs()
                        .checkAssert { input: NodeResolutionParams ->

                            assertThat(createOutput(input))
                                .extracting(NodeSequenceAlternatives::nodeIdSequences)
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

                    private fun forInputs(): TheoryBuilder<NodeResolutionParams> = forInputs(DISCRETE_LINKS_CONNECTED,
                                                                                             DISCRETE_ENDPOINT_NODES,
                                                                                             EMPTY)

                    @Test
                    @DisplayName("There should be one node ID sequence containing three items")
                    fun verifyCountOfNodeIdSequencesHavingThreeItems() {
                        forInputs()
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
                        forInputs()
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

                    private fun forInputs(): TheoryBuilder<NodeResolutionParams> = forInputs(DISCRETE_LINKS_UNCONNECTED,
                                                                                             DISCRETE_ENDPOINT_NODES,
                                                                                             EMPTY)

                    @Test
                    @DisplayName("There should be four node ID sequences containing four items")
                    fun verifyCountOfNodeIdSequencesHavingFourItems() {
                        forInputs()
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
    }

    @Nested
    @DisplayName("When list of given via nodes is empty")
    inner class WhenListOfGivenViaNodesIsEmpty {

        private fun forInputs(): TheoryBuilder<NodeResolutionParams> = forInputs(TerminusLinkRelation.ANY,
                                                                                 TerminusLinkEndpointDiscreteness.ANY,
                                                                                 EMPTY)

        @Test
        @DisplayName("Sequence of via node IDs should be empty")
        fun viaNodeIdsShouldBeEmpty() {
            forInputs()
                .checkAssert { input: NodeResolutionParams ->

                    assertThat(createOutput(input))
                        .extracting(NodeSequenceAlternatives::viaNodeIds)
                        .isEqualTo(NodeIdSequence.empty())
                }
        }

        @Nested
        @DisplayName("When given only one infrastructure link")
        inner class WhenGivenOnlyOneLink {

            @Nested
            @DisplayName("When terminus links have non-discrete endpoint nodes (start from and end at same node)")
            inner class WhenTerminusLinkStartsFromSameNodeAsItEndsAt {

                private fun forInputs(): TheoryBuilder<NodeResolutionParams> = forInputs(SAME,
                                                                                         NON_DISCRETE_ENDPOINT_NODES,
                                                                                         EMPTY)

                @Test
                @DisplayName("Verify that node ID sequence contains exactly one item")
                fun nodeIdSequenceShouldContainExactlyOneItem() {
                    forInputs()
                        .checkAssert { input: NodeResolutionParams ->

                            assertThat(createOutput(input).nodeIdSequences)
                                .element(0)
                                .extracting(NodeIdSequence::list)
                                .asList()
                                .hasSize(1)
                        }
                }

                @Test
                @DisplayName("Verify that the only node ID sequence consists of the only node possible")
                fun verifyNodeIdSequence() {
                    forInputs()
                        .checkAssert { input: NodeResolutionParams ->

                            assertThat(createOutput(input).nodeIdSequences)
                                .element(0)
                                .extracting(NodeIdSequence::list)
                                .asList()
                                .containsExactly(input.startLink.startNode.id)
                        }
                }
            }

            @Nested
            @DisplayName("When terminus links have non-discrete endpoint nodes (start from and end at same node)")
            inner class WhenLinkEndpointsAreDiscrete {

                private fun forInputs(): TheoryBuilder<NodeResolutionParams> = forInputs(SAME,
                                                                                         DISCRETE_ENDPOINT_NODES,
                                                                                         EMPTY)

                @Test
                @DisplayName("Verify that node ID sequence contains exactly two items")
                fun nodeIdSequenceShouldContainExactlyTwoItems() {
                    forInputs()
                        .checkAssert { input: NodeResolutionParams ->

                            assertThat(createOutput(input).nodeIdSequences)
                                .element(0)
                                .extracting(NodeIdSequence::list)
                                .asList()
                                .hasSize(2)
                        }
                }

                @Test
                @DisplayName("The one and only node ID sequence should contain endpoints of snapped link in order")
                fun verifyNodeIdSequence() {
                    forInputs()
                        .checkAssert { input: NodeResolutionParams ->

                            val snappedLink = input.startLink

                            assertThat(createOutput(input).nodeIdSequences)
                                .element(0)
                                .extracting(NodeIdSequence::list)
                                .asList()
                                .containsExactly(snappedLink.closerNodeId, snappedLink.furtherNodeId)
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

            private fun forInputs(): TheoryBuilder<NodeResolutionParams> = forInputs(TerminusLinkRelation.ANY,
                                                                                     TerminusLinkEndpointDiscreteness.ANY,
                                                                                     FULLY_REDUNDANT_WITH_TERMINUS_LINKS)

            @Test
            @DisplayName("Sequence of via node IDs should be empty")
            fun viaNodeIdsShouldBeEmpty() {
                forInputs()
                    .checkAssert { input: NodeResolutionParams ->

                        assertThat(createOutput(input))
                            .extracting(NodeSequenceAlternatives::viaNodeIds)
                            .extracting(NodeIdSequence::list)
                            .asList()
                            .isEmpty()
                    }
            }

            @Nested
            @DisplayName("When given only one infrastructure link as terminus link")
            inner class WhenGivenSingleTerminusLink {

                private fun forInputs(): TheoryBuilder<NodeResolutionParams> = forInputs(SAME,
                                                                                         TerminusLinkEndpointDiscreteness.ANY,
                                                                                         FULLY_REDUNDANT_WITH_TERMINUS_LINKS)

                @Test
                @DisplayName("List of node ID sequences should consist of only one list")
                fun thereShouldBeOnlyOneNodeIdSequence() {
                    forInputs()
                        .checkAssert { input: NodeResolutionParams ->

                            assertThat(createOutput(input))
                                .extracting(NodeSequenceAlternatives::nodeIdSequences)
                                .asList()
                                .hasSize(1)
                        }
                }
            }
        }

        @Nested
        @DisplayName("When sequence of via nodes is not redundant with regard to terminus links")
        inner class WhenViaNodesAreNonRedundantWithTerminusLinks {

            private fun forInputs(): TheoryBuilder<NodeResolutionParams> = forInputs(TerminusLinkRelation.ANY,
                                                                                     TerminusLinkEndpointDiscreteness.ANY,
                                                                                     NON_REDUNDANT_WITH_TERMINUS_LINKS)

            @Test
            @DisplayName("Sequence of via node IDs should not be empty")
            fun viaNodeIdsShouldNotBeEmpty() {
                forInputs()
                    .checkAssert { input: NodeResolutionParams ->

                        assertThat(createOutput(input))
                            .extracting(NodeSequenceAlternatives::viaNodeIds)
                            .extracting(NodeIdSequence::list)
                            .asList()
                            .isNotEmpty()
                    }
            }

            @Test
            @DisplayName("Check validity of via node ID sequence")
            fun verifyViaNodeIdSequence() {
                forInputs()
                    .checkAssert { input: NodeResolutionParams ->

                        val expectedViaNodeIds: List<InfrastructureNodeId> = filterOutConsecutiveDuplicates(
                            input.viaNodeResolvers.map(HasInfrastructureNodeId::getInfrastructureNodeId)
                        )

                        assertThat(createOutput(input))
                            .extracting(NodeSequenceAlternatives::viaNodeIds)
                            .extracting(NodeIdSequence::list)
                            .asList()
                            .isEqualTo(expectedViaNodeIds)
                    }
            }

            @Test
            @DisplayName("Verify that each alternative node ID sequence contains via node IDs in the middle")
            fun verifyEachNodeIdSequenceContainsViaNodeIdsInTheMiddle() {
                forInputs()
                    .checkAssert { input: NodeResolutionParams ->

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
                    }
            }
        }
    }

    companion object {

        private fun createOutput(input: NodeResolutionParams): NodeSequenceAlternatives =
            NodeSequenceAlternativesCreator.create(input.startLink,
                                                   input.viaNodeResolvers,
                                                   input.endLink)

        private fun forInputs(terminusLinkRelation: TerminusLinkRelation,
                              terminusLinkEndpointDiscreteness: TerminusLinkEndpointDiscreteness,
                              viaNodeGenerationScheme: ViaNodeGenerationScheme,
                              randomnessSeed: Long = System.nanoTime())
            : TheoryBuilder<NodeResolutionParams> {

            return qt()
                .withFixedSeed(randomnessSeed)
                .forAll(
                    NodeResolutionParamsGenerator
                        .builder()
                        .withTerminusLinkRelation(terminusLinkRelation)
                        .withTerminusLinkEndpointDiscreteness(terminusLinkEndpointDiscreteness)
                        .withViaNodeGenerationScheme(viaNodeGenerationScheme)
                        .build()
                        .describedAs { params ->
                            val viaNodeIds: List<Long> =
                                params.viaNodeResolvers.map { it.getInfrastructureNodeId().value }

                            // Make assertion failures more readable.
                            """
                                {
                                    startLink: {
                                        id: ${params.startLink.infrastructureLinkId.value},
                                        closestDistance: ${params.startLink.closestDistance},
                                        startNodeId: ${params.startLink.startNode.id.value},
                                        startNodeDistance: ${params.startLink.startNode.distanceToNode},
                                        endNodeId: ${params.startLink.endNode.id.value},
                                        endNodeDistance: ${params.startLink.endNode.distanceToNode}
                                    },
                                    viaNodeIds: $viaNodeIds,
                                    endLink: {
                                        id: ${params.endLink.infrastructureLinkId.value},
                                        closestDistance: ${params.endLink.closestDistance},
                                        startNodeId: ${params.endLink.startNode.id.value},
                                        startNodeDistance: ${params.endLink.startNode.distanceToNode},
                                        endNodeId: ${params.endLink.endNode.id.value},
                                        endNodeDistance: ${params.endLink.endNode.distanceToNode}
                                    }
                                }
                                """.trimIndent()
                        }
                )
        }

        private fun forAllInputs(): TheoryBuilder<NodeResolutionParams> {
            return forInputs(TerminusLinkRelation.ANY,
                             TerminusLinkEndpointDiscreteness.ANY,
                             ViaNodeGenerationScheme.ANY)
        }
    }
}
