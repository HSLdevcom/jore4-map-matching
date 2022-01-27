package fi.hsl.jore4.mapmatching.service.node

import fi.hsl.jore4.mapmatching.model.InfrastructureNodeId
import fi.hsl.jore4.mapmatching.model.NodeIdSequence
import fi.hsl.jore4.mapmatching.service.node.NodeResolutionParamsGenerator.TerminusLinkRelation
import fi.hsl.jore4.mapmatching.service.node.NodeResolutionParamsGenerator.TerminusLinkRelation.CONNECTED
import fi.hsl.jore4.mapmatching.service.node.NodeResolutionParamsGenerator.TerminusLinkRelation.DISCRETE
import fi.hsl.jore4.mapmatching.service.node.NodeResolutionParamsGenerator.TerminusLinkRelation.SAME
import fi.hsl.jore4.mapmatching.service.node.NodeResolutionParamsGenerator.TerminusLinkRelation.UNCONNECTED
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
import org.slf4j.Logger
import org.slf4j.LoggerFactory

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
    @DisplayName("Verify that each alternative node ID sequence contains at least two items")
    fun eachNodeIdSequenceShouldContainAtLeastTwoItems() {
        forAllInputs()
            .checkAssert { input: NodeResolutionParams ->

                val output: NodeSequenceAlternatives = createOutput(input)

                assertThat(output.nodeIdSequences)
                    .allMatch { it.size >= 2 }
            }
    }

    @Test
    @DisplayName("Verify that each alternative node ID sequence starts with node IDs of start link")
    fun eachNodeIdSequenceShouldStartWithNodeIdsOfStartLink() {
        forAllInputs()
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
        forAllInputs()
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

        private fun forInputs(): TheoryBuilder<NodeResolutionParams> = forInputs(TerminusLinkRelation.ANY,
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

            private fun forInputs(): TheoryBuilder<NodeResolutionParams> = forInputs(SAME, EMPTY)

            @Test
            @DisplayName("There should be only one node ID sequence in the list")
            fun thereShouldBeOnlyOneNodeIdSequence() {
                forInputs()
                    .checkAssert { input: NodeResolutionParams ->

                        assertThat(createOutput(input))
                            .extracting(NodeSequenceAlternatives::nodeIdSequences)
                            .asList()
                            .hasSize(1)
                    }
            }

            @Test
            @DisplayName("Verify that node ID sequence contains exactly two items")
            fun nodeIdSequenceShouldContainExactlyTwoItems() {
                forInputs()
                    .checkAssert { input: NodeResolutionParams ->

                        val output: NodeSequenceAlternatives = createOutput(input)

                        assertThat(output.nodeIdSequences)
                            .allMatch { it.size == 2 }
                    }
            }

            @Test
            @DisplayName("The one and only node ID sequence should contain endpoints of snapped link in order")
            fun verifyNodeIdSequence() {
                forInputs()
                    .checkAssert { input: NodeResolutionParams ->

                        val snappedLink = input.startLink

                        assertThat(createOutput(input))
                            .extracting(NodeSequenceAlternatives::nodeIdSequences)
                            .asList()
                            .element(0)
                            .isEqualTo(NodeIdSequence(listOf(snappedLink.closerNodeId, snappedLink.furtherNodeId)))
                    }
            }
        }

        @Nested
        @DisplayName("When given two discrete infrastructure links")
        inner class WhenGivenTwoDiscreteLinks {

            private fun forInputs(): TheoryBuilder<NodeResolutionParams> = forInputs(DISCRETE, EMPTY)

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

                private fun forInputs(): TheoryBuilder<NodeResolutionParams> = forInputs(CONNECTED, EMPTY)

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

                private fun forInputs(): TheoryBuilder<NodeResolutionParams> = forInputs(UNCONNECTED, EMPTY)

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

    @Nested
    @DisplayName("When list of given via nodes is not empty")
    inner class WhenListOfGivenViaNodesIsNotEmpty {

        @Nested
        @DisplayName("When sequence of via nodes is fully redundant with regard to terminus links")
        inner class WhenViaNodesAreFullyRedundantWithTerminusLinks {

            private fun forInputs(): TheoryBuilder<NodeResolutionParams> = forInputs(TerminusLinkRelation.ANY,
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

            private fun forInputs(randomnessSeed: Long): TheoryBuilder<NodeResolutionParams> =
                forInputs(TerminusLinkRelation.ANY,
                          NON_REDUNDANT_WITH_TERMINUS_LINKS,
                          randomnessSeed)

            @Test
            @DisplayName("Sequence of via node IDs should not be empty")
            fun viaNodeIdsShouldNotBeEmpty() {
                findTheoryBuilderWithWorkingRandomnessSeed { seed -> forInputs(seed) }
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
                findTheoryBuilderWithWorkingRandomnessSeed { seed -> forInputs(seed) }
                    .checkAssert { input: NodeResolutionParams ->

                        val expectedViaNodeIds: List<InfrastructureNodeId> = filterOutConsecutiveDuplicates(
                            input.viaNodeResolvers.map { it.getInfrastructureNodeId() }
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
                findTheoryBuilderWithWorkingRandomnessSeed { seed -> forInputs(seed) }
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

        private val LOGGER: Logger = LoggerFactory.getLogger(NodeSequenceAlternativesCreatorTest::class.java)

        private fun createOutput(input: NodeResolutionParams): NodeSequenceAlternatives =
            NodeSequenceAlternativesCreator.create(input.startLink,
                                                   input.viaNodeResolvers,
                                                   input.endLink)

        private fun forInputs(terminusLinkRelation: TerminusLinkRelation,
                              viaNodeGenerationScheme: ViaNodeGenerationScheme,
                              randomnessSeed: Long = System.nanoTime())
            : TheoryBuilder<NodeResolutionParams> {

            return qt()
                .withFixedSeed(randomnessSeed)
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

        private fun forAllInputs(): TheoryBuilder<NodeResolutionParams> =
            forInputs(TerminusLinkRelation.ANY, ViaNodeGenerationScheme.ANY)

        private fun <T> findTheoryBuilderWithWorkingRandomnessSeed(maxRetries: Int = 10,
                                                                   getTheoryBuilder: (seed: Long) -> TheoryBuilder<T>)
            : TheoryBuilder<T> {

            if (maxRetries < 0) {
                throw IllegalArgumentException("maxRetries must not be negative: $maxRetries")
            }
            if (maxRetries > 20) {
                throw IllegalArgumentException("maxRetries must not be greater than 20: $maxRetries")
            }

            var theoryBuilder: TheoryBuilder<T>?

            for (attempt in 1..(1 + maxRetries)) {
                val retry = attempt - 1
                val seed = retry.toLong()

                if (retry > 0) {
                    LOGGER.debug("Retry #$retry with randomnessSeed=$seed")
                }

                theoryBuilder = getTheoryBuilder(seed)

                try {
                    theoryBuilder.checkAssert {
                        // Basically, it is just tested whether running TheoryBuilder throws IllegalArgumentException.
                    }
                    return theoryBuilder
                } catch (ex: IllegalArgumentException) {
                    LOGGER.warn("TheoryBuilder.checkAssert() failed with randomnessSeed=$seed")

                    if (attempt <= maxRetries) continue else throw ex
                }
            }

            // should never enter here, just to make compiler happy
            throw IllegalStateException("Illegal state while finding working seed for TheoryBuilder")
        }
    }
}
