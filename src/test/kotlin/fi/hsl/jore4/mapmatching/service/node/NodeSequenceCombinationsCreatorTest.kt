package fi.hsl.jore4.mapmatching.service.node

import fi.hsl.jore4.mapmatching.model.InfrastructureNodeId
import fi.hsl.jore4.mapmatching.model.NodeIdSequence
import fi.hsl.jore4.mapmatching.test.generators.CommonGenerators.pair
import fi.hsl.jore4.mapmatching.test.generators.InfrastructureNodeIdGenerator.discreteNodeIdPair
import fi.hsl.jore4.mapmatching.test.generators.InfrastructureNodeIdGenerator.discreteNodeIdQuadruple
import fi.hsl.jore4.mapmatching.test.generators.InfrastructureNodeIdGenerator.infrastructureNodeId
import fi.hsl.jore4.mapmatching.test.generators.Retry
import fi.hsl.jore4.mapmatching.util.CollectionUtils.filterOutConsecutiveDuplicates
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.InstanceOfAssertFactories
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.quicktheories.QuickTheory.qt
import org.quicktheories.core.Gen
import org.quicktheories.generators.Generate.booleans
import org.quicktheories.generators.Generate.pick
import org.quicktheories.generators.SourceDSL.integers
import org.quicktheories.generators.SourceDSL.lists
import kotlin.reflect.KClass

class NodeSequenceCombinationsCreatorTest {
    @Nested
    @DisplayName("When given instance of VisitSingleNode")
    inner class WhenGivenVisitSingleNode {
        @Test
        fun shouldContainOnlyOneNodeIdSequence() {
            qt()
                .forAll(infrastructureNodeId())
                .checkAssert { nodeId ->

                    val nodesToVisit = VisitSingleNode(nodeId)

                    assertThat(NodeSequenceCombinationsCreator.create(nodesToVisit))
                        .asInstanceOf(InstanceOfAssertFactories.LIST)
                        .hasSize(1)
                }
        }

        @Test
        fun shouldContainExactlyTheVisitedNodeId() {
            qt()
                .forAll(infrastructureNodeId())
                .checkAssert { nodeId ->

                    val nodesToVisit = VisitSingleNode(nodeId)

                    assertThat(NodeSequenceCombinationsCreator.create(nodesToVisit))
                        .asInstanceOf(InstanceOfAssertFactories.LIST)
                        .isEqualTo(
                            listOf(
                                NodeIdSequence(
                                    listOf(
                                        nodeId
                                    )
                                )
                            )
                        )
                }
        }
    }

    @Nested
    @DisplayName("When given instance of VisitNodesOfSingleLinkUnidirectionally")
    inner class WhenGivenVisitNodesOfSingleLinkUnidirectionally {
        @Test
        fun shouldContainOnlyOneNodeIdSequence() {
            qt()
                .forAll(pair(infrastructureNodeId()))
                .checkAssert { (startNodeId, endNodeId) ->

                    val nodesToVisit = VisitNodesOfSingleLinkUnidirectionally(startNodeId, endNodeId)

                    assertThat(NodeSequenceCombinationsCreator.create(nodesToVisit))
                        .asInstanceOf(InstanceOfAssertFactories.LIST)
                        .hasSize(1)
                }
        }

        @Test
        fun shouldContainExactlyTheVisitedNodeIds() {
            qt()
                .forAll(pair(infrastructureNodeId()))
                .checkAssert { (startNodeId, endNodeId) ->

                    val nodesToVisit = VisitNodesOfSingleLinkUnidirectionally(startNodeId, endNodeId)

                    assertThat(NodeSequenceCombinationsCreator.create(nodesToVisit))
                        .asInstanceOf(InstanceOfAssertFactories.LIST)
                        .isEqualTo(
                            listOf(
                                NodeIdSequence(
                                    listOf(
                                        startNodeId,
                                        endNodeId
                                    )
                                )
                            )
                        )
                }
        }
    }

    @Nested
    @DisplayName("When given instance of VisitNodesOfSingleLinkBidirectionally")
    inner class WhenGivenVisitNodesOfSingleLinkBidirectionally {
        @Test
        fun shouldContainTwoNodeIdSequences() {
            qt()
                .forAll(pair(infrastructureNodeId()))
                .checkAssert { (firstNodeId, secondNodeId) ->

                    val nodesToVisit = VisitNodesOfSingleLinkBidirectionally(firstNodeId, secondNodeId)

                    assertThat(NodeSequenceCombinationsCreator.create(nodesToVisit))
                        .asInstanceOf(InstanceOfAssertFactories.LIST)
                        .hasSize(2)
                }
        }

        @Test
        fun shouldContainPermutationsOfVisitedNodeIds() {
            qt()
                .forAll(pair(infrastructureNodeId()))
                .checkAssert { (firstNodeId, secondNodeId) ->

                    val nodesToVisit = VisitNodesOfSingleLinkBidirectionally(firstNodeId, secondNodeId)

                    assertThat(NodeSequenceCombinationsCreator.create(nodesToVisit))
                        .asInstanceOf(InstanceOfAssertFactories.LIST)
                        .isEqualTo(
                            listOf(
                                NodeIdSequence(
                                    listOf(
                                        firstNodeId,
                                        secondNodeId
                                    )
                                ),
                                NodeIdSequence(
                                    listOf(
                                        secondNodeId,
                                        firstNodeId
                                    )
                                )
                            )
                        )
                }
        }
    }

    @Nested
    @DisplayName("When given instance of VisitNodesOnMultipleLinks")
    inner class WhenGivenVisitNodesOnMultipleLinks {
        @Test
        fun numberOfNodeIdSequencesShouldBeBetween1AndFour() {
            qt()
                .forAll(
                    generateVisitedNodesOnLink(),
                    generateViaNodeIds(allowEmpty = true),
                    generateVisitedNodesOnLink()
                ).checkAssert {
                    someNodesToVisitOnStartLink,
                    viaNodeIds,
                    someNodesToVisitOnEndLink
                    ->

                    val nodesToVisit =
                        VisitNodesOnMultipleLinks(
                            someNodesToVisitOnStartLink,
                            viaNodeIds,
                            someNodesToVisitOnEndLink
                        )

                    assertThat(NodeSequenceCombinationsCreator.create(nodesToVisit))
                        .asInstanceOf(InstanceOfAssertFactories.LIST)
                        .hasSizeGreaterThanOrEqualTo(1)
                        .hasSizeLessThanOrEqualTo(4)
                }
        }

        @Test
        fun alternativesShouldContainAtLeastOneNodeId() {
            qt()
                .forAll(
                    generateVisitedNodesOnLink(),
                    generateViaNodeIds(allowEmpty = true),
                    generateVisitedNodesOnLink()
                ).checkAssert {
                    someNodesToVisitOnStartLink,
                    viaNodeIds,
                    someNodesToVisitOnEndLink
                    ->

                    val nodesToVisit =
                        VisitNodesOnMultipleLinks(
                            someNodesToVisitOnStartLink,
                            viaNodeIds,
                            someNodesToVisitOnEndLink
                        )

                    assertThat(NodeSequenceCombinationsCreator.create(nodesToVisit))
                        .allMatch { nodeIdSeq ->
                            nodeIdSeq.size >= 1
                        }
                }
        }

        @Test
        fun nodeIdSequencesShouldBeIrreducibleWithRegardToConsecutiveDuplicates() {
            qt()
                .forAll(
                    generateVisitedNodesOnLink(),
                    generateViaNodeIds(allowEmpty = true),
                    generateVisitedNodesOnLink()
                ).checkAssert {
                    someNodesToVisitOnStartLink,
                    viaNodeIds,
                    someNodesToVisitOnEndLink
                    ->

                    val nodesToVisit =
                        VisitNodesOnMultipleLinks(
                            someNodesToVisitOnStartLink,
                            viaNodeIds,
                            someNodesToVisitOnEndLink
                        )

                    assertThat(NodeSequenceCombinationsCreator.create(nodesToVisit))
                        .allMatch { nodeIdSeq ->
                            val listOfNodeIds: List<InfrastructureNodeId> = nodeIdSeq.list

                            listOfNodeIds.size == filterOutConsecutiveDuplicates(listOfNodeIds).size
                        }
                }
        }

        @Nested
        @DisplayName("When via node IDs are not overlapping with nodes of terminus links")
        inner class WhenViaNodeIdsNotOverlappingWithTerminusLinks {
            @Test
            fun alternativesShouldContainReducedViaNodeIds() {
                val genNodesToVisitOnStartLink: Gen<VisitedNodesOnLink> = generateVisitedNodesOnLink()
                val genNodesToVisitOnEndLink: Gen<VisitedNodesOnLink> = generateVisitedNodesOnLink()

                val genVisitedNodes: Gen<VisitNodesOnMultipleLinks> =
                    genNodesToVisitOnStartLink
                        .flatMap { nodesToVisitOnStartLink ->
                            genNodesToVisitOnEndLink
                                .flatMap { nodesToVisitOnEndLink ->

                                    val nodeIdsToExclude: List<InfrastructureNodeId> =
                                        nodesToVisitOnStartLink.toListOfNodeIds() +
                                            nodesToVisitOnEndLink.toListOfNodeIds()

                                    // Generate via node IDs that are not overlapping with nodes of terminus links.
                                    generateViaNodeIds(allowEmpty = false, excludedNodeIds = nodeIdsToExclude.toSet())
                                        .map { viaNodeIds ->

                                            VisitNodesOnMultipleLinks(
                                                nodesToVisitOnStartLink,
                                                viaNodeIds,
                                                nodesToVisitOnEndLink
                                            )
                                        }
                                }
                        }

                qt()
                    .forAll(genVisitedNodes)
                    .checkAssert { nodesToVisit ->

                        val (someNodesToVisitOnStartLink, viaNodeIds, someNodesToVisitOnEndLink) = nodesToVisit
                        val reducedViaNodeIds: List<InfrastructureNodeId> = filterOutConsecutiveDuplicates(viaNodeIds)

                        assertThat(NodeSequenceCombinationsCreator.create(nodesToVisit))
                            .allMatch { nodeIdSeq ->

                                val actualViaNodeIds: List<InfrastructureNodeId> =
                                    nodeIdSeq.list
                                        .drop(getNodeCount(someNodesToVisitOnStartLink))
                                        .dropLast(getNodeCount(someNodesToVisitOnEndLink))

                                actualViaNodeIds == reducedViaNodeIds
                            }
                    }
            }
        }

        @Nested
        @DisplayName("When visiting only single node on start link")
        inner class WhenVisitingOnlySingleNodeOnStartLink {
            private fun forAll() =
                qt().forAll(
                    infrastructureNodeId(),
                    generateViaNodeIds(allowEmpty = true),
                    generateVisitedNodesOnLink()
                )

            @Test
            fun shouldContainAtMostTwoNodeIdSequences() {
                forAll().checkAssert { singleNodeIdOnStartLink, viaNodeIds, someNodesToVisitOnEndLink ->

                    val nodesToVisit =
                        VisitNodesOnMultipleLinks(
                            VisitSingleNode(singleNodeIdOnStartLink),
                            viaNodeIds,
                            someNodesToVisitOnEndLink
                        )

                    assertThat(NodeSequenceCombinationsCreator.create(nodesToVisit))
                        .asInstanceOf(InstanceOfAssertFactories.LIST)
                        .hasSizeLessThanOrEqualTo(2)
                }
            }

            @Test
            fun alternativesShouldStartWithSingleNodeIdOfStartLink() {
                forAll().checkAssert { singleNodeIdOnStartLink, viaNodeIds, someNodesToVisitOnEndLink ->

                    val nodesToVisit =
                        VisitNodesOnMultipleLinks(
                            VisitSingleNode(singleNodeIdOnStartLink),
                            viaNodeIds,
                            someNodesToVisitOnEndLink
                        )

                    assertThat(NodeSequenceCombinationsCreator.create(nodesToVisit))
                        .allMatch { nodeIdSeq ->
                            nodeIdSeq.list.first() == singleNodeIdOnStartLink
                        }
                }
            }
        }

        @Nested
        @DisplayName("When visiting only single node on end link")
        inner class WhenVisitingOnlySingleNodeOnEndLink {
            private fun forAll() =
                qt().forAll(
                    generateVisitedNodesOnLink(),
                    generateViaNodeIds(allowEmpty = true),
                    infrastructureNodeId()
                )

            @Test
            fun shouldContainAtMostTwoNodeIdSequences() {
                forAll().checkAssert { someNodesToVisitOnStartLink, viaNodeIds, singleNodeIdOnEndLink ->

                    val nodesToVisit =
                        VisitNodesOnMultipleLinks(
                            someNodesToVisitOnStartLink,
                            viaNodeIds,
                            VisitSingleNode(singleNodeIdOnEndLink)
                        )

                    assertThat(NodeSequenceCombinationsCreator.create(nodesToVisit))
                        .asInstanceOf(InstanceOfAssertFactories.LIST)
                        .hasSizeLessThanOrEqualTo(2)
                }
            }

            @Test
            fun alternativesShouldEndWithSingleNodeIdOfEndLink() {
                forAll().checkAssert { someNodesToVisitOnStartLink, viaNodeIds, singleNodeIdOnEndLink ->

                    val nodesToVisit =
                        VisitNodesOnMultipleLinks(
                            someNodesToVisitOnStartLink,
                            viaNodeIds,
                            VisitSingleNode(singleNodeIdOnEndLink)
                        )

                    assertThat(NodeSequenceCombinationsCreator.create(nodesToVisit))
                        .allMatch { nodeIdSeq ->
                            nodeIdSeq.list.last() == singleNodeIdOnEndLink
                        }
                }
            }
        }

        @Nested
        @DisplayName("When visiting nodes on start link unidirectionally")
        inner class WhenVisitingNodesOnStartLinkUnidirectionally {
            private fun forAll() =
                qt().forAll(
                    discreteNodeIdPair(),
                    generateViaNodeIds(allowEmpty = true),
                    generateVisitedNodesOnLink()
                )

            @Test
            fun shouldContainAtMostTwoNodeIdSequences() {
                forAll().checkAssert { (startNodeId, endNodeId), viaNodeIds, someNodesToVisitOnEndLink ->

                    val nodesToVisit =
                        VisitNodesOnMultipleLinks(
                            VisitNodesOfSingleLinkUnidirectionally(startNodeId, endNodeId),
                            viaNodeIds,
                            someNodesToVisitOnEndLink
                        )

                    assertThat(NodeSequenceCombinationsCreator.create(nodesToVisit))
                        .asInstanceOf(InstanceOfAssertFactories.LIST)
                        .hasSizeLessThanOrEqualTo(2)
                }
            }

            @Test
            fun alternativesShouldContainAtLeastTwoNodeIds() {
                forAll().checkAssert { (startNodeId, endNodeId), viaNodeIds, someNodesToVisitOnEndLink ->

                    val nodesToVisit =
                        VisitNodesOnMultipleLinks(
                            VisitNodesOfSingleLinkUnidirectionally(startNodeId, endNodeId),
                            viaNodeIds,
                            someNodesToVisitOnEndLink
                        )

                    assertThat(NodeSequenceCombinationsCreator.create(nodesToVisit))
                        .allMatch { nodeIdSeq ->
                            nodeIdSeq.size >= 2
                        }
                }
            }

            @Test
            fun alternativesShouldStartWithNodesOfStartLink() {
                forAll().checkAssert { (startNodeId, endNodeId), viaNodeIds, someNodesToVisitOnEndLink ->

                    val nodesToVisitOnStartLink = VisitNodesOfSingleLinkUnidirectionally(startNodeId, endNodeId)

                    val nodesToVisit =
                        VisitNodesOnMultipleLinks(
                            nodesToVisitOnStartLink,
                            viaNodeIds,
                            someNodesToVisitOnEndLink
                        )

                    assertThat(NodeSequenceCombinationsCreator.create(nodesToVisit))
                        .allMatch { nodeIdSeq ->
                            nodeIdSeq.list.take(2) == nodesToVisitOnStartLink.toListOfNodeIds()
                        }
                }
            }
        }

        @Nested
        @DisplayName("When visiting nodes on end link unidirectionally")
        inner class WhenVisitingNodesOnEndLinkUnidirectionally {
            private fun forAll() =
                qt().forAll(
                    generateVisitedNodesOnLink(),
                    generateViaNodeIds(allowEmpty = true),
                    discreteNodeIdPair()
                )

            @Test
            fun shouldContainAtMostTwoNodeIdSequences() {
                forAll().checkAssert { someNodesToVisitOnStartLink, viaNodeIds, (startNodeId, endNodeId) ->

                    val nodesToVisit =
                        VisitNodesOnMultipleLinks(
                            someNodesToVisitOnStartLink,
                            viaNodeIds,
                            VisitNodesOfSingleLinkUnidirectionally(startNodeId, endNodeId)
                        )

                    assertThat(NodeSequenceCombinationsCreator.create(nodesToVisit))
                        .asInstanceOf(InstanceOfAssertFactories.LIST)
                        .hasSizeLessThanOrEqualTo(2)
                }
            }

            @Test
            fun alternativesShouldContainAtLeastTwoNodeIds() {
                forAll().checkAssert { someNodesToVisitOnStartLink, viaNodeIds, (startNodeId, endNodeId) ->

                    val nodesToVisit =
                        VisitNodesOnMultipleLinks(
                            someNodesToVisitOnStartLink,
                            viaNodeIds,
                            VisitNodesOfSingleLinkUnidirectionally(startNodeId, endNodeId)
                        )

                    assertThat(NodeSequenceCombinationsCreator.create(nodesToVisit))
                        .allMatch { nodeIdSeq ->
                            nodeIdSeq.size >= 2
                        }
                }
            }

            @Test
            fun alternativesShouldEndWithNodesOfEndLink() {
                forAll().checkAssert { someNodesToVisitOnStartLink, viaNodeIds, (startNodeId, endNodeId) ->

                    val nodesToVisitOnEndLink = VisitNodesOfSingleLinkUnidirectionally(startNodeId, endNodeId)

                    val nodesToVisit =
                        VisitNodesOnMultipleLinks(
                            someNodesToVisitOnStartLink,
                            viaNodeIds,
                            nodesToVisitOnEndLink
                        )

                    assertThat(NodeSequenceCombinationsCreator.create(nodesToVisit))
                        .allMatch { nodeIdSeq ->
                            nodeIdSeq.list.takeLast(2) == nodesToVisitOnEndLink.toListOfNodeIds()
                        }
                }
            }
        }

        @Nested
        @DisplayName("When visiting nodes on start link bidirectionally")
        inner class WhenVisitingNodesOnStartLinkBidirectionally {
            private fun forAll() =
                qt().forAll(
                    discreteNodeIdPair(),
                    generateViaNodeIds(allowEmpty = true),
                    generateVisitedNodesOnLink()
                )

            @Test
            fun hasAtLeastTwoNodeIdSequences() {
                forAll().checkAssert { (startNodeId, endNodeId), viaNodeIds, someNodesToVisitOnEndLink ->

                    val nodesToVisit =
                        VisitNodesOnMultipleLinks(
                            VisitNodesOfSingleLinkBidirectionally(startNodeId, endNodeId),
                            viaNodeIds,
                            someNodesToVisitOnEndLink
                        )

                    assertThat(NodeSequenceCombinationsCreator.create(nodesToVisit))
                        .asInstanceOf(InstanceOfAssertFactories.LIST)
                        .hasSizeGreaterThanOrEqualTo(2)
                }
            }

            @Test
            fun alternativesShouldContainAtLeastTwoNodeIds() {
                forAll().checkAssert { (startNodeId, endNodeId), viaNodeIds, someNodesToVisitOnEndLink ->

                    val nodesToVisit =
                        VisitNodesOnMultipleLinks(
                            VisitNodesOfSingleLinkBidirectionally(startNodeId, endNodeId),
                            viaNodeIds,
                            someNodesToVisitOnEndLink
                        )

                    assertThat(NodeSequenceCombinationsCreator.create(nodesToVisit))
                        .allMatch { nodeIdSeq ->
                            nodeIdSeq.size >= 2
                        }
                }
            }

            @Test
            fun alternativesShouldStartWithNodesOfStartLink() {
                forAll().checkAssert { (startNodeId, endNodeId), viaNodeIds, someNodesToVisitOnEndLink ->

                    val nodesToVisitOnStartLink = VisitNodesOfSingleLinkBidirectionally(startNodeId, endNodeId)

                    val nodesToVisit =
                        VisitNodesOnMultipleLinks(
                            nodesToVisitOnStartLink,
                            viaNodeIds,
                            someNodesToVisitOnEndLink
                        )

                    assertThat(NodeSequenceCombinationsCreator.create(nodesToVisit))
                        .allMatch { nodeIdSeq ->
                            val expectedNodeIds: List<InfrastructureNodeId> = nodesToVisitOnStartLink.toListOfNodeIds()

                            val firstTwoNodeIds: List<InfrastructureNodeId> = nodeIdSeq.list.take(2)

                            firstTwoNodeIds == expectedNodeIds || firstTwoNodeIds == expectedNodeIds.reversed()
                        }
                }
            }
        }

        @Nested
        @DisplayName("When visiting nodes on end link bidirectionally")
        inner class WhenVisitingNodesOnEndLinkBidirectionally {
            private fun forAll() =
                qt().forAll(
                    generateVisitedNodesOnLink(),
                    generateViaNodeIds(allowEmpty = true),
                    discreteNodeIdPair()
                )

            @Test
            fun haAtLeastTwoNodeIdSequences() {
                forAll().checkAssert { someNodesToVisitOnStartLink, viaNodeIds, (startNodeId, endNodeId) ->

                    val nodesToVisit =
                        VisitNodesOnMultipleLinks(
                            someNodesToVisitOnStartLink,
                            viaNodeIds,
                            VisitNodesOfSingleLinkBidirectionally(startNodeId, endNodeId)
                        )

                    assertThat(NodeSequenceCombinationsCreator.create(nodesToVisit))
                        .asInstanceOf(InstanceOfAssertFactories.LIST)
                        .hasSizeGreaterThanOrEqualTo(2)
                }
            }

            @Test
            fun alternativesShouldContainAtLeastTwoNodeIds() {
                forAll().checkAssert { someNodesToVisitOnStartLink, viaNodeIds, (startNodeId, endNodeId) ->

                    val nodesToVisit =
                        VisitNodesOnMultipleLinks(
                            someNodesToVisitOnStartLink,
                            viaNodeIds,
                            VisitNodesOfSingleLinkBidirectionally(startNodeId, endNodeId)
                        )

                    assertThat(NodeSequenceCombinationsCreator.create(nodesToVisit))
                        .allMatch { nodeIdSeq ->
                            nodeIdSeq.size >= 2
                        }
                }
            }

            @Test
            fun alternativesShouldEndWithNodesOfEndLink() {
                forAll().checkAssert { someNodesToVisitOnStartLink, viaNodeIds, (startNodeId, endNodeId) ->

                    val nodesToVisitOnEndLink = VisitNodesOfSingleLinkBidirectionally(startNodeId, endNodeId)

                    val nodesToVisit =
                        VisitNodesOnMultipleLinks(
                            someNodesToVisitOnStartLink,
                            viaNodeIds,
                            nodesToVisitOnEndLink
                        )

                    assertThat(NodeSequenceCombinationsCreator.create(nodesToVisit))
                        .allMatch { nodeIdSeq ->
                            val expectedNodeIds: List<InfrastructureNodeId> = nodesToVisitOnEndLink.toListOfNodeIds()

                            val lastTwoNodeIds: List<InfrastructureNodeId> = nodeIdSeq.list.takeLast(2)

                            lastTwoNodeIds == expectedNodeIds || lastTwoNodeIds == expectedNodeIds.reversed()
                        }
                }
            }
        }

        @Nested
        @DisplayName("When visiting nodes of both terminus links bidirectionally")
        inner class WhenVisitingNodesOfBothTerminusLinksBidirectionally {
            private fun forAll() =
                qt().forAll(
                    discreteNodeIdPair(),
                    generateViaNodeIds(allowEmpty = true),
                    discreteNodeIdPair()
                )

            @Test
            fun numberOfNodeIdSequencesShouldBeFour() {
                forAll().checkAssert { (startNodeId1, endNodeId1), viaNodeIds, (startNodeId2, endNodeId2) ->

                    val nodesToVisit =
                        VisitNodesOnMultipleLinks(
                            VisitNodesOfSingleLinkBidirectionally(startNodeId1, endNodeId1),
                            viaNodeIds,
                            VisitNodesOfSingleLinkBidirectionally(startNodeId2, endNodeId2)
                        )

                    assertThat(NodeSequenceCombinationsCreator.create(nodesToVisit))
                        .asInstanceOf(InstanceOfAssertFactories.LIST)
                        .hasSize(4)
                }
            }

            @Test
            fun alternativesShouldContainAtLeastThreeNodeIds() {
                forAll().checkAssert { (startNodeId1, endNodeId1), viaNodeIds, (startNodeId2, endNodeId2) ->

                    val nodesToVisit =
                        VisitNodesOnMultipleLinks(
                            VisitNodesOfSingleLinkBidirectionally(startNodeId1, endNodeId1),
                            viaNodeIds,
                            VisitNodesOfSingleLinkBidirectionally(startNodeId2, endNodeId2)
                        )

                    assertThat(NodeSequenceCombinationsCreator.create(nodesToVisit))
                        .allMatch { nodeIdSeq ->
                            nodeIdSeq.size >= 3
                        }
                }
            }
        }

        @Nested
        @DisplayName("When terminus links having discrete endpoints are unconnected")
        inner class WhenTerminusLinksHavingDiscreteEndpointsAreUnconnected {
            @Test
            fun alternativesShouldContainAtLeastFourNodeIds() {
                fun createVisitedNodesOnLink(
                    firstNodeId: InfrastructureNodeId,
                    secondNodeId: InfrastructureNodeId,
                    bidirectional: Boolean
                ): VisitedNodesOnLink =
                    if (bidirectional) {
                        VisitNodesOfSingleLinkBidirectionally(firstNodeId, secondNodeId)
                    } else {
                        VisitNodesOfSingleLinkUnidirectionally(firstNodeId, secondNodeId)
                    }

                val genTerminusLinkPair: Gen<Pair<VisitedNodesOnLink, VisitedNodesOnLink>> =
                    discreteNodeIdQuadruple().zip(
                        booleans(),
                        booleans()
                    ) {
                        (nodeId1, nodeId2, nodeId3, nodeId4),
                        isStartLinkBidirectional,
                        isEndLinkBidirectional
                        ->

                        Pair(
                            createVisitedNodesOnLink(nodeId1, nodeId2, isStartLinkBidirectional),
                            createVisitedNodesOnLink(nodeId3, nodeId4, isEndLinkBidirectional)
                        )
                    }

                qt()
                    .forAll(genTerminusLinkPair, generateViaNodeIds(allowEmpty = true))
                    .checkAssert { (nodesToVisitOnStartLink, nodesToVisitOnendLink), viaNodeIds ->

                        val nodesToVisit =
                            VisitNodesOnMultipleLinks(
                                nodesToVisitOnStartLink,
                                viaNodeIds,
                                nodesToVisitOnendLink
                            )

                        assertThat(NodeSequenceCombinationsCreator.create(nodesToVisit))
                            .allMatch { nodeIdSeq ->
                                nodeIdSeq.size >= 4
                            }
                    }
            }
        }
    }

    companion object {
        private val GENERATE_VISITED_NODES_ON_LINK_KCLASS: Gen<KClass<out VisitedNodesOnLink>> =
            pick(
                listOf(
                    VisitSingleNode::class,
                    VisitNodesOfSingleLinkUnidirectionally::class,
                    VisitNodesOfSingleLinkBidirectionally::class
                )
            )

        fun generateVisitedNodesOnLink(): Gen<VisitedNodesOnLink> =
            GENERATE_VISITED_NODES_ON_LINK_KCLASS.flatMap { className ->
                when (className) {
                    VisitSingleNode::class -> infrastructureNodeId().map(::VisitSingleNode)
                    else ->
                        discreteNodeIdPair()
                            .map { (firstNodeId, secondNodeId) ->

                                if (className == VisitNodesOfSingleLinkUnidirectionally::class) {
                                    VisitNodesOfSingleLinkUnidirectionally(firstNodeId, secondNodeId)
                                } else {
                                    VisitNodesOfSingleLinkBidirectionally(firstNodeId, secondNodeId)
                                }
                            }
                }
            }

        fun generateViaNodeIds(
            allowEmpty: Boolean,
            excludedNodeIds: Set<InfrastructureNodeId> = emptySet()
        ): Gen<List<InfrastructureNodeId>> {
            val minNumberOfNodes: Int = if (allowEmpty) 0 else 1

            val genNodeId: Gen<InfrastructureNodeId> =
                if (excludedNodeIds.isEmpty()) {
                    infrastructureNodeId()
                } else {
                    Retry(infrastructureNodeId()) { nodeId ->
                        nodeId !in excludedNodeIds
                    }
                }

            return integers()
                .between(minNumberOfNodes, 7)
                .flatMap { numberOfNodes ->
                    lists()
                        .of(genNodeId)
                        .ofSize(numberOfNodes)
                }
        }

        fun getNodeCount(visitedNodes: VisitedNodesOnLink): Int = if (visitedNodes is VisitSingleNode) 1 else 2
    }
}
