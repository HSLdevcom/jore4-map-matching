package fi.hsl.jore4.mapmatching.service.node

import fi.hsl.jore4.mapmatching.repository.infrastructure.SnappedLinkState
import fi.hsl.jore4.mapmatching.service.node.VisitedNodesResolverParamsGenerator.LinkDirection
import fi.hsl.jore4.mapmatching.service.node.VisitedNodesResolverParamsGenerator.LinkDirection.BIDIRECTIONAL
import fi.hsl.jore4.mapmatching.service.node.VisitedNodesResolverParamsGenerator.LinkDirection.ONE_WAY_AGAINST_DIGITISED_DIRECTION
import fi.hsl.jore4.mapmatching.service.node.VisitedNodesResolverParamsGenerator.LinkDirection.ONE_WAY_ALONG_DIGITISED_DIRECTION
import fi.hsl.jore4.mapmatching.service.node.VisitedNodesResolverParamsGenerator.LinkEndpointDiscreteness.DISCRETE_NODES
import fi.hsl.jore4.mapmatching.service.node.VisitedNodesResolverParamsGenerator.LinkEndpointDiscreteness.NON_DISCRETE_NODES
import fi.hsl.jore4.mapmatching.service.node.VisitedNodesResolverParamsGenerator.SnapPointLocation
import fi.hsl.jore4.mapmatching.service.node.VisitedNodesResolverParamsGenerator.SnapPointLocation.AT_END
import fi.hsl.jore4.mapmatching.service.node.VisitedNodesResolverParamsGenerator.SnapPointLocation.AT_MIDPOINT
import fi.hsl.jore4.mapmatching.service.node.VisitedNodesResolverParamsGenerator.SnapPointLocation.AT_OR_CLOSE_TO_END
import fi.hsl.jore4.mapmatching.service.node.VisitedNodesResolverParamsGenerator.SnapPointLocation.AT_OR_CLOSE_TO_START
import fi.hsl.jore4.mapmatching.service.node.VisitedNodesResolverParamsGenerator.SnapPointLocation.AT_START
import fi.hsl.jore4.mapmatching.service.node.VisitedNodesResolverParamsGenerator.SnapPointLocation.BETWEEN_ENDPOINTS_EXCLUSIVE
import fi.hsl.jore4.mapmatching.service.node.VisitedNodesResolverParamsGenerator.SnapPointLocation.CLOSE_TO_END
import fi.hsl.jore4.mapmatching.service.node.VisitedNodesResolverParamsGenerator.SnapPointLocation.CLOSE_TO_START
import fi.hsl.jore4.mapmatching.service.node.VisitedNodesResolverParamsGenerator.TerminusLinkProperties
import fi.hsl.jore4.mapmatching.service.node.VisitedNodesResolverParamsGenerator.TerminusLinkRelation
import fi.hsl.jore4.mapmatching.service.node.VisitedNodesResolverParamsGenerator.TerminusLinkRelation.DISCRETE_LINKS
import fi.hsl.jore4.mapmatching.service.node.VisitedNodesResolverParamsGenerator.TerminusLinkRelation.DISCRETE_LINKS_UNCONNECTED
import fi.hsl.jore4.mapmatching.service.node.VisitedNodesResolverParamsGenerator.TerminusLinkRelation.SAME_LINK
import fi.hsl.jore4.mapmatching.service.node.VisitedNodesResolverParamsGenerator.ViaNodeGenerationScheme
import fi.hsl.jore4.mapmatching.service.node.VisitedNodesResolverParamsGenerator.ViaNodeGenerationScheme.VIA_NODES_EMPTY
import fi.hsl.jore4.mapmatching.service.node.VisitedNodesResolverParamsGenerator.ViaNodeGenerationScheme.VIA_NODES_FULLY_REDUNDANT
import fi.hsl.jore4.mapmatching.service.node.VisitedNodesResolverParamsGenerator.ViaNodeGenerationScheme.VIA_NODES_NON_REDUNDANT
import fi.hsl.jore4.mapmatching.service.node.VisitedNodesResolverParamsGenerator.ViaNodeGenerationScheme.VIA_NODES_NOT_STARTING_OR_ENDING_WITH_NODES_OF_TERMINUS_LINKS
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.quicktheories.QuickTheory.qt
import org.quicktheories.core.Gen
import org.quicktheories.dsl.TheoryBuilder
import org.quicktheories.generators.Generate.oneOf

@Suppress("MemberVisibilityCanBePrivate")
class VisitedNodesResolverTest {

    @Nested
    @DisplayName("When start link is same as end link")
    inner class WhenStartLinksIsSameAsEndLink {

        @Nested
        @DisplayName("When endpoints of single link are non-discrete (link starts from and ends at same node)")
        inner class WhenEndpointsOfLinkAreNonDiscrete {

            fun forInputs(viaNodeGenerationScheme: ViaNodeGenerationScheme)
                : TheoryBuilder<VisitedNodesResolverParams> {

                return forAll(withSingleLink(TerminusLinkProperties.from(NON_DISCRETE_NODES),
                                             viaNodeGenerationScheme))
            }

            @Test
            fun whenNoViaNodesAreGiven() {
                forInputs(VIA_NODES_EMPTY)
                    .checkAssert { input: VisitedNodesResolverParams ->

                        assertThat(createOutput(input))
                            .isInstanceOfSatisfying(VisitSingleNode::class.java) {

                                assertThat(it.nodeId).isEqualTo(input.startLink.startNodeId)
                            }
                    }
            }

            @Test
            fun whenViaNodeIdsAreFullyRedundant() {
                forInputs(VIA_NODES_FULLY_REDUNDANT)
                    .checkAssert { input: VisitedNodesResolverParams ->

                        assertThat(createOutput(input))
                            .isInstanceOfSatisfying(VisitSingleNode::class.java) {

                                assertThat(it.nodeId).isEqualTo(input.startLink.startNodeId)
                            }
                    }
            }

            @Test
            fun whenViaNodeIdsAreNotRedundant() {
                forInputs(VIA_NODES_NON_REDUNDANT)
                    .checkAssert { input: VisitedNodesResolverParams ->

                        assertThat(createOutput(input))
                            .isExactlyInstanceOf(VisitNodesOnMultipleLinks::class.java)
                    }
            }
        }

        @Nested
        @DisplayName("When endpoints of single link are discrete")
        inner class WhenEndpointsOfSingleLinkAreDiscrete {

            fun withSingleLink(linkDirection: LinkDirection,
                               snapPointLocation: SnapPointLocation,
                               viaNodeScheme: ViaNodeGenerationScheme)
                : Gen<VisitedNodesResolverParams> {

                return withSingleLink(TerminusLinkProperties(DISCRETE_NODES, linkDirection, snapPointLocation),
                                      viaNodeScheme)
            }

            @Nested
            @DisplayName("When single link is bidirectional")
            inner class WhenSingleLinkIsBidirectional {

                fun forInputs(snapPointLocation: SnapPointLocation, viaNodeScheme: ViaNodeGenerationScheme)
                    : TheoryBuilder<VisitedNodesResolverParams> {

                    return forAll(withSingleLink(BIDIRECTIONAL, snapPointLocation, viaNodeScheme))
                }

                @Nested
                @DisplayName("When first snap point is closer to link start")
                inner class WhenFirstSnapPointIsCloserToLinkStart {

                    fun forInputs(viaNodeScheme: ViaNodeGenerationScheme)
                        : TheoryBuilder<VisitedNodesResolverParams> = forInputs(AT_OR_CLOSE_TO_START,
                                                                                viaNodeScheme)

                    @Test
                    fun whenNoViaNodesAreGiven() {
                        forInputs(VIA_NODES_EMPTY)
                            .checkAssert { input: VisitedNodesResolverParams ->

                                assertThat(createOutput(input))
                                    .isInstanceOfSatisfying(VisitNodesOfSingleLinkUnidirectionally::class.java) {

                                        assertThat(it.startNodeId).isEqualTo(input.startLink.startNodeId)
                                        assertThat(it.endNodeId).isEqualTo(input.startLink.endNodeId)
                                    }
                            }
                    }

                    @Test
                    fun whenViaNodeIdsAreFullyRedundant() {
                        forInputs(VIA_NODES_FULLY_REDUNDANT)
                            .checkAssert { input: VisitedNodesResolverParams ->

                                assertThat(createOutput(input))
                                    .isInstanceOfSatisfying(VisitNodesOfSingleLinkUnidirectionally::class.java) {

                                        assertThat(it.startNodeId).isEqualTo(input.startLink.startNodeId)
                                        assertThat(it.endNodeId).isEqualTo(input.startLink.endNodeId)
                                    }
                            }
                    }

                    @Test
                    fun whenViaNodeIdsAreNotRedundant() {
                        forInputs(VIA_NODES_NON_REDUNDANT)
                            .checkAssert { input: VisitedNodesResolverParams ->

                                assertThat(createOutput(input))
                                    .isExactlyInstanceOf(VisitNodesOnMultipleLinks::class.java)
                            }
                    }
                }

                @Nested
                @DisplayName("When first snap point is closer to link end")
                inner class WhenFirstSnapPointIsCloserToLinkEnd {

                    fun forInputs(viaNodeScheme: ViaNodeGenerationScheme)
                        : TheoryBuilder<VisitedNodesResolverParams> = forInputs(AT_OR_CLOSE_TO_END,
                                                                                viaNodeScheme)

                    @Test
                    fun whenNoViaNodesAreGiven() {
                        forInputs(VIA_NODES_EMPTY)
                            .checkAssert { input: VisitedNodesResolverParams ->

                                assertThat(createOutput(input))
                                    .isInstanceOfSatisfying(VisitNodesOfSingleLinkUnidirectionally::class.java) {

                                        assertThat(it.startNodeId).isEqualTo(input.startLink.endNodeId)
                                        assertThat(it.endNodeId).isEqualTo(input.startLink.startNodeId)
                                    }
                            }
                    }

                    @Test
                    fun whenViaNodeIdsAreFullyRedundant() {
                        forInputs(VIA_NODES_FULLY_REDUNDANT)
                            .checkAssert { input: VisitedNodesResolverParams ->

                                assertThat(createOutput(input))
                                    .isInstanceOfSatisfying(VisitNodesOfSingleLinkUnidirectionally::class.java) {

                                        assertThat(it.startNodeId).isEqualTo(input.startLink.endNodeId)
                                        assertThat(it.endNodeId).isEqualTo(input.startLink.startNodeId)
                                    }
                            }
                    }

                    @Test
                    fun whenViaNodeIdsAreNotRedundant() {
                        forInputs(VIA_NODES_NON_REDUNDANT)
                            .checkAssert { input: VisitedNodesResolverParams ->

                                assertThat(createOutput(input))
                                    .isExactlyInstanceOf(VisitNodesOnMultipleLinks::class.java)
                            }
                    }
                }

                @Nested
                @DisplayName("When first snap point is equal to second snap point")
                inner class WhenFirstSnapPointIsEqualToSecondSnapPoint {

                    fun forInputs(viaNodeScheme: ViaNodeGenerationScheme): TheoryBuilder<VisitedNodesResolverParams> =
                        forInputs(AT_MIDPOINT,
                                  viaNodeScheme)

                    @Test
                    fun whenNoViaNodesAreGiven() {
                        forInputs(VIA_NODES_EMPTY)
                            .checkAssert { input: VisitedNodesResolverParams ->

                                assertThat(createOutput(input))
                                    .isInstanceOfSatisfying(VisitSingleNode::class.java) {

                                        assertThat(it.nodeId).isEqualTo(input.startLink.startNodeId)
                                    }
                            }
                    }

                    @Test
                    fun whenViaNodeIdsAreFullyRedundant() {
                        forInputs(VIA_NODES_FULLY_REDUNDANT)
                            .checkAssert { input: VisitedNodesResolverParams ->

                                assertThat(createOutput(input))
                                    .isInstanceOfSatisfying(VisitSingleNode::class.java) {

                                        assertThat(it.nodeId).isEqualTo(input.startLink.startNodeId)
                                    }
                            }
                    }

                    @Test
                    fun whenViaNodeIdsAreNotRedundant() {
                        forInputs(VIA_NODES_NON_REDUNDANT)
                            .checkAssert { input: VisitedNodesResolverParams ->

                                assertThat(createOutput(input))
                                    .isExactlyInstanceOf(VisitNodesOnMultipleLinks::class.java)
                            }
                    }
                }
            }

            @Nested
            @DisplayName("When single link is one-way and against digitised direction")
            inner class WhenSingleLinkIsOneWayAndAgainstDigitisedDirection {

                @Nested
                @DisplayName("When first snap point is NOT closer to link start")
                inner class WhenFirstSnapPointIsNotCloserToLinkStart {

                    fun withSingleLink(snapPointLocation: SnapPointLocation,
                                       viaNodeScheme: ViaNodeGenerationScheme)
                        : Gen<VisitedNodesResolverParams> {

                        return withSingleLink(ONE_WAY_AGAINST_DIGITISED_DIRECTION, snapPointLocation, viaNodeScheme)
                    }

                    fun forInputs(viaNodeScheme: ViaNodeGenerationScheme): TheoryBuilder<VisitedNodesResolverParams> {
                        return forAll(oneOf(withSingleLink(AT_OR_CLOSE_TO_END, viaNodeScheme),
                                            withSingleLink(AT_MIDPOINT, viaNodeScheme)))
                    }

                    @Test
                    fun whenNoViaNodesAreGiven() {
                        forInputs(VIA_NODES_EMPTY)
                            .checkAssert { input: VisitedNodesResolverParams ->

                                assertThat(createOutput(input))
                                    .isInstanceOfSatisfying(VisitNodesOfSingleLinkUnidirectionally::class.java) {

                                        assertThat(it.startNodeId).isEqualTo(input.startLink.endNodeId)
                                        assertThat(it.endNodeId).isEqualTo(input.startLink.startNodeId)
                                    }
                            }
                    }

                    @Test
                    fun whenViaNodeIdsAreFullyRedundant() {
                        forInputs(VIA_NODES_FULLY_REDUNDANT)
                            .checkAssert { input: VisitedNodesResolverParams ->

                                assertThat(createOutput(input))
                                    .isInstanceOfSatisfying(VisitNodesOfSingleLinkUnidirectionally::class.java) {

                                        assertThat(it.startNodeId).isEqualTo(input.startLink.endNodeId)
                                        assertThat(it.endNodeId).isEqualTo(input.startLink.startNodeId)
                                    }
                            }
                    }

                    @Test
                    fun whenViaNodeIdsAreNotRedundant() {
                        forInputs(VIA_NODES_NON_REDUNDANT)
                            .checkAssert { input: VisitedNodesResolverParams ->

                                assertThat(createOutput(input))
                                    .isExactlyInstanceOf(VisitNodesOnMultipleLinks::class.java)
                            }
                    }
                }

                @Nested
                @DisplayName("When first snap point is closer to link start")
                inner class WhenFirstSnapPointIsCloserToLinkStart {

                    fun forInputs(viaNodeScheme: ViaNodeGenerationScheme): TheoryBuilder<VisitedNodesResolverParams> {
                        return forAll(withSingleLink(ONE_WAY_AGAINST_DIGITISED_DIRECTION,
                                                     AT_OR_CLOSE_TO_START,
                                                     viaNodeScheme))
                    }

                    @Nested
                    @DisplayName("When no via nodes are given")
                    inner class WhenNoViaNodesAreGiven {

                        fun forInputs(): TheoryBuilder<VisitedNodesResolverParams> = forInputs(VIA_NODES_EMPTY)

                        @Test
                        fun viaNodeIdsShouldBeEmpty() {
                            forInputs().checkAssert { input: VisitedNodesResolverParams ->

                                assertThat(createOutput(input))
                                    .isInstanceOfSatisfying(VisitNodesOnMultipleLinks::class.java) {

                                        assertThat(it.viaNodeIds)
                                            .asList()
                                            .isEmpty()
                                    }
                            }
                        }

                        @Test
                        fun nodesShouldBeVisitedTwice() {
                            forInputs().checkAssert { input: VisitedNodesResolverParams ->

                                assertThat(createOutput(input))
                                    .isInstanceOfSatisfying(VisitNodesOnMultipleLinks::class.java) {

                                        val expectedNodesToVisit =
                                            VisitNodesOfSingleLinkUnidirectionally(input.startLink.endNodeId,
                                                                                   input.startLink.startNodeId)

                                        assertThat(it.nodesToVisitOnStartLink).isEqualTo(expectedNodesToVisit)
                                        assertThat(it.nodesToVisitOnEndLink).isEqualTo(expectedNodesToVisit)
                                    }
                            }
                        }
                    }

                    @Nested
                    @DisplayName("When via node IDs are fully redundant")
                    inner class WhenViaNodeIdsAreFullyRedundant {

                        fun forInputs(): TheoryBuilder<VisitedNodesResolverParams> =
                            forInputs(VIA_NODES_FULLY_REDUNDANT)

                        @Test
                        fun viaNodeIdsShouldBeEmpty() {
                            forInputs().checkAssert { input: VisitedNodesResolverParams ->

                                assertThat(createOutput(input))
                                    .isInstanceOfSatisfying(VisitNodesOnMultipleLinks::class.java) {

                                        assertThat(it.viaNodeIds)
                                            .asList()
                                            .isEmpty()
                                    }
                            }
                        }

                        @Test
                        fun nodesShouldBeVisitedTwice() {
                            forInputs().checkAssert { input: VisitedNodesResolverParams ->

                                assertThat(createOutput(input))
                                    .isInstanceOfSatisfying(VisitNodesOnMultipleLinks::class.java) {

                                        val expectedNodesToVisit =
                                            VisitNodesOfSingleLinkUnidirectionally(input.startLink.endNodeId,
                                                                                   input.startLink.startNodeId)

                                        assertThat(it.nodesToVisitOnStartLink).isEqualTo(expectedNodesToVisit)
                                        assertThat(it.nodesToVisitOnEndLink).isEqualTo(expectedNodesToVisit)
                                    }
                            }
                        }
                    }

                    @Nested
                    @DisplayName("When via node IDs are not redundant")
                    inner class WhenViaNodeIdsAreNotRedundant {

                        @Test
                        fun viaNodeIdsShouldNotBeEmpty() {
                            forInputs(VIA_NODES_NON_REDUNDANT)
                                .checkAssert { input: VisitedNodesResolverParams ->

                                    assertThat(createOutput(input))
                                        .isInstanceOfSatisfying(VisitNodesOnMultipleLinks::class.java) {

                                            assertThat(it.viaNodeIds)
                                                .asList()
                                                .isNotEmpty()
                                        }
                                }
                        }
                    }
                }
            }

            @Nested
            @DisplayName("When single link is one-way and along digitised direction")
            inner class WhenSingleLinkIsOneWayAndAlongDigitisedDirection {

                @Nested
                @DisplayName("When first snap point is NOT closer to link end")
                inner class WhenFirstSnapPointIsNotCloserToLinkEnd {

                    fun withSingleLink(snapPointLocation: SnapPointLocation,
                                       viaNodeScheme: ViaNodeGenerationScheme)
                        : Gen<VisitedNodesResolverParams> {

                        return withSingleLink(ONE_WAY_ALONG_DIGITISED_DIRECTION, snapPointLocation, viaNodeScheme)
                    }

                    fun forInputs(viaNodeScheme: ViaNodeGenerationScheme): TheoryBuilder<VisitedNodesResolverParams> {
                        return forAll(oneOf(withSingleLink(AT_OR_CLOSE_TO_START, viaNodeScheme),
                                            withSingleLink(AT_MIDPOINT, viaNodeScheme)))
                    }

                    @Test
                    fun whenNoViaNodesAreGiven() {
                        forInputs(VIA_NODES_EMPTY)
                            .checkAssert { input: VisitedNodesResolverParams ->

                                assertThat(createOutput(input))
                                    .isInstanceOfSatisfying(VisitNodesOfSingleLinkUnidirectionally::class.java) {

                                        assertThat(it.startNodeId).isEqualTo(input.startLink.startNodeId)
                                        assertThat(it.endNodeId).isEqualTo(input.startLink.endNodeId)
                                    }
                            }
                    }

                    @Test
                    fun whenViaNodeIdsAreFullyRedundant() {
                        forInputs(VIA_NODES_FULLY_REDUNDANT)
                            .checkAssert { input: VisitedNodesResolverParams ->

                                assertThat(createOutput(input))
                                    .isInstanceOfSatisfying(VisitNodesOfSingleLinkUnidirectionally::class.java) {

                                        assertThat(it.startNodeId).isEqualTo(input.startLink.startNodeId)
                                        assertThat(it.endNodeId).isEqualTo(input.startLink.endNodeId)
                                    }
                            }
                    }

                    @Test
                    fun whenViaNodeIdsAreNotRedundant() {
                        forInputs(VIA_NODES_NON_REDUNDANT)
                            .checkAssert { input: VisitedNodesResolverParams ->

                                assertThat(createOutput(input))
                                    .isExactlyInstanceOf(VisitNodesOnMultipleLinks::class.java)
                            }
                    }
                }

                @Nested
                @DisplayName("When first snap point is closer to link end")
                inner class WhenFirstSnapPointIsCloserToLinkEnd {

                    fun forInputs(viaNodeScheme: ViaNodeGenerationScheme): TheoryBuilder<VisitedNodesResolverParams> {
                        return forAll(withSingleLink(ONE_WAY_ALONG_DIGITISED_DIRECTION,
                                                     AT_OR_CLOSE_TO_END,
                                                     viaNodeScheme))
                    }

                    @Nested
                    @DisplayName("When no via nodes are given")
                    inner class WhenNoViaNodesAreGiven {

                        fun forInputs(): TheoryBuilder<VisitedNodesResolverParams> = forInputs(VIA_NODES_EMPTY)

                        @Test
                        fun viaNodeIdsShouldBeEmpty() {
                            forInputs().checkAssert { input: VisitedNodesResolverParams ->

                                assertThat(createOutput(input))
                                    .isInstanceOfSatisfying(VisitNodesOnMultipleLinks::class.java) {

                                        assertThat(it.viaNodeIds)
                                            .asList()
                                            .isEmpty()
                                    }
                            }
                        }

                        @Test
                        fun nodesShouldBeVisitedTwice() {
                            forInputs().checkAssert { input: VisitedNodesResolverParams ->

                                assertThat(createOutput(input))
                                    .isInstanceOfSatisfying(VisitNodesOnMultipleLinks::class.java) {

                                        val expectedNodesToVisit =
                                            VisitNodesOfSingleLinkUnidirectionally(input.startLink.startNodeId,
                                                                                   input.startLink.endNodeId)

                                        assertThat(it.nodesToVisitOnStartLink).isEqualTo(expectedNodesToVisit)
                                        assertThat(it.nodesToVisitOnEndLink).isEqualTo(expectedNodesToVisit)
                                    }
                            }
                        }
                    }

                    @Nested
                    @DisplayName("When via node IDs are fully redundant")
                    inner class WhenViaNodeIdsAreFullyRedundant {

                        fun forInputs(): TheoryBuilder<VisitedNodesResolverParams> =
                            forInputs(VIA_NODES_FULLY_REDUNDANT)

                        @Test
                        fun viaNodeIdsShouldBeEmpty() {
                            forInputs().checkAssert { input: VisitedNodesResolverParams ->

                                assertThat(createOutput(input))
                                    .isInstanceOfSatisfying(VisitNodesOnMultipleLinks::class.java) {

                                        assertThat(it.viaNodeIds)
                                            .asList()
                                            .isEmpty()
                                    }
                            }
                        }

                        @Test
                        fun nodesShouldBeVisitedTwice() {
                            forInputs().checkAssert { input: VisitedNodesResolverParams ->

                                assertThat(createOutput(input))
                                    .isInstanceOfSatisfying(VisitNodesOnMultipleLinks::class.java) {

                                        val expectedNodesToVisit =
                                            VisitNodesOfSingleLinkUnidirectionally(input.startLink.startNodeId,
                                                                                   input.startLink.endNodeId)

                                        assertThat(it.nodesToVisitOnStartLink).isEqualTo(expectedNodesToVisit)
                                        assertThat(it.nodesToVisitOnEndLink).isEqualTo(expectedNodesToVisit)
                                    }
                            }
                        }
                    }

                    @Nested
                    @DisplayName("When via node IDs are not redundant")
                    inner class WhenViaNodeIdsAreNotRedundant {

                        @Test
                        fun viaNodeIdsShouldNotBeEmpty() {
                            forInputs(VIA_NODES_NON_REDUNDANT)
                                .checkAssert { input: VisitedNodesResolverParams ->

                                    assertThat(createOutput(input))
                                        .isInstanceOfSatisfying(VisitNodesOnMultipleLinks::class.java) {

                                            assertThat(it.viaNodeIds)
                                                .asList()
                                                .isNotEmpty()
                                        }
                                }
                        }
                    }
                }
            }
        }
    }

    @Nested
    @DisplayName("When start link is NOT same as end link")
    inner class WhenStartLinksIsNotSameAsEndLink {

        @Test
        fun assertTypeOfVisitedNodes() {
            forAll(createInput(DISCRETE_LINKS,
                               ViaNodeGenerationScheme.ANY))
                .checkAssert { input: VisitedNodesResolverParams ->

                    assertThat(createOutput(input))
                        .isExactlyInstanceOf(VisitNodesOnMultipleLinks::class.java)
                }
        }

        @Nested
        @DisplayName("Verify via node IDs")
        inner class VerifyViaNodeIds {

            fun forInputs(viaNodeScheme: ViaNodeGenerationScheme): TheoryBuilder<VisitedNodesResolverParams> =
                forAll(createInput(DISCRETE_LINKS,
                                   viaNodeScheme))

            @Test
            fun whenListOfViaNodeIdsIsEmpty() {
                forInputs(VIA_NODES_EMPTY)
                    .checkAssert { input: VisitedNodesResolverParams ->

                        assertThat(createOutput(input))
                            .isInstanceOfSatisfying(VisitNodesOnMultipleLinks::class.java) {

                                assertThat(it.viaNodeIds)
                                    .asList()
                                    .isEmpty()
                            }
                    }
            }

            @Test
            fun whenViaNodeIdsAreFullyRedundant() {
                forInputs(VIA_NODES_FULLY_REDUNDANT)
                    .checkAssert { input: VisitedNodesResolverParams ->

                        assertThat(createOutput(input))
                            .isInstanceOfSatisfying(VisitNodesOnMultipleLinks::class.java) {

                                assertThat(it.viaNodeIds)
                                    .asList()
                                    .isNotEmpty()
                            }
                    }
            }

            @Test
            fun whenViaNodeIdsAreNotRedundant() {
                forInputs(VIA_NODES_NON_REDUNDANT)
                    .checkAssert { input: VisitedNodesResolverParams ->

                        assertThat(createOutput(input))
                            .isInstanceOfSatisfying(VisitNodesOnMultipleLinks::class.java) {

                                assertThat(it.viaNodeIds)
                                    .asList()
                                    .isNotEmpty()
                            }
                    }
            }
        }

        @Nested
        @DisplayName("Verify visited nodes on start link")
        inner class VerifyVisitedNodesOnStartLink {

            fun expectVisitSingleNode(input: VisitedNodesResolverParams) {
                assertThat(createOutput(input))
                    .isInstanceOfSatisfying(VisitNodesOnMultipleLinks::class.java) { visit ->

                        assertThat(visit.nodesToVisitOnStartLink)
                            .isInstanceOfSatisfying(VisitSingleNode::class.java) {

                                assertThat(it.nodeId)
                                    .isEqualTo(input.startLink.closerNodeId)
                            }
                    }
            }

            fun expectVisitNodesOfSingleLinkUnidirectionally(input: VisitedNodesResolverParams,
                                                             nodesReversed: Boolean = false) {

                assertThat(createOutput(input))
                    .isInstanceOfSatisfying(VisitNodesOnMultipleLinks::class.java) { visit ->

                        assertThat(visit.nodesToVisitOnStartLink)
                            .isInstanceOfSatisfying(VisitNodesOfSingleLinkUnidirectionally::class.java) {

                                val snappedLink: SnappedLinkState = input.startLink

                                if (nodesReversed) {
                                    assertThat(it.startNodeId).isEqualTo(snappedLink.endNodeId)
                                    assertThat(it.endNodeId).isEqualTo(snappedLink.startNodeId)
                                } else {
                                    assertThat(it.startNodeId).isEqualTo(snappedLink.startNodeId)
                                    assertThat(it.endNodeId).isEqualTo(snappedLink.endNodeId)
                                }
                            }
                    }
            }

            fun expectVisitNodesOfSingleLinkBidirectionally(input: VisitedNodesResolverParams) {
                assertThat(createOutput(input))
                    .isInstanceOfSatisfying(VisitNodesOnMultipleLinks::class.java) { visit ->

                        assertThat(visit.nodesToVisitOnStartLink)
                            .isInstanceOfSatisfying(VisitNodesOfSingleLinkBidirectionally::class.java) {

                                val snappedLink: SnappedLinkState = input.startLink

                                assertThat(it.firstNodeId).isEqualTo(snappedLink.startNodeId)
                                assertThat(it.secondNodeId).isEqualTo(snappedLink.endNodeId)
                            }
                    }
            }

            @Test
            fun whenStartLinkHasNonDiscreteNodes() {
                forAll(withStartLink(DISCRETE_LINKS,
                                     TerminusLinkProperties.NON_DISCRETE_NODES,
                                     ViaNodeGenerationScheme.ANY))
                    .checkAssert { expectVisitSingleNode(it) }
            }

            @Nested
            @DisplayName("When start link has discrete nodes")
            inner class WhenStartLinkHasDiscreteNodes {

                @Test
                fun whenSnappedToStartNodeOfStartLink() {
                    forAll(withStartLink(DISCRETE_LINKS,
                                         TerminusLinkProperties(DISCRETE_NODES,
                                                                LinkDirection.ANY,
                                                                AT_START),
                                         ViaNodeGenerationScheme.ANY))
                        .checkAssert { expectVisitSingleNode(it) }
                }

                @Test
                fun whenSnappedToEndNodeOfStartLink() {
                    forAll(withStartLink(DISCRETE_LINKS,
                                         TerminusLinkProperties(DISCRETE_NODES,
                                                                LinkDirection.ANY,
                                                                AT_END),
                                         ViaNodeGenerationScheme.ANY))
                        .checkAssert { expectVisitSingleNode(it) }
                }

                @Nested
                @DisplayName("When further node from snapped start link location appears at start of via nodes")
                inner class WhenFurtherNodeFromSnappedStartLinkLocationAppearsAtStartOfViaNodes {

                    fun forInputs(linkDirection: LinkDirection, snapPointLocation: SnapPointLocation)
                        : TheoryBuilder<VisitedNodesResolverParams> {

                        val genParams: Gen<VisitedNodesResolverParams> =
                            withStartLink(DISCRETE_LINKS_UNCONNECTED,
                                          TerminusLinkProperties(DISCRETE_NODES, linkDirection, snapPointLocation),
                                          VIA_NODES_NOT_STARTING_OR_ENDING_WITH_NODES_OF_TERMINUS_LINKS)
                                .map { params ->
                                    params.run {
                                        withViaNodeIds(listOf(startLink.furtherNodeId) + viaNodeIds)
                                    }
                                }
                                .describedAs { prettyPrint(it) }

                        return forAll(genParams)
                    }

                    @Test
                    fun whenStartLinkIsBidirectional() {
                        forInputs(BIDIRECTIONAL,
                                  BETWEEN_ENDPOINTS_EXCLUSIVE)
                            .checkAssert { expectVisitSingleNode(it) }
                    }

                    @Nested
                    @DisplayName("When start link is one-way and direction is along digitised direction")
                    inner class WhenStartLinkIsAlongDigitisedDirection {

                        fun forInputs(snapPointLocation: SnapPointLocation): TheoryBuilder<VisitedNodesResolverParams> =
                            forInputs(ONE_WAY_ALONG_DIGITISED_DIRECTION,
                                      snapPointLocation)

                        @Test
                        fun whenSnapPointLocationIsCloserToStartNode() {
                            forInputs(CLOSE_TO_START)
                                .checkAssert { expectVisitSingleNode(it) }
                        }

                        @Test
                        fun whenSnapPointLocationIsAtMidpoint() {
                            forInputs(AT_MIDPOINT)
                                .checkAssert { expectVisitSingleNode(it) }
                        }

                        @Test
                        fun whenSnapPointLocationIsCloserToEndNode() {
                            forInputs(CLOSE_TO_END)
                                .checkAssert { expectVisitNodesOfSingleLinkUnidirectionally(it) }
                        }
                    }

                    @Nested
                    @DisplayName("When start link is one-way and direction is against digitised direction")
                    inner class WhenStartLinkIsAgainstDigitisedDirection {

                        fun forInputs(snapPointLocation: SnapPointLocation): TheoryBuilder<VisitedNodesResolverParams> =
                            forInputs(ONE_WAY_AGAINST_DIGITISED_DIRECTION,
                                      snapPointLocation)

                        @Test
                        fun whenSnapPointLocationIsCloserToStartNode() {
                            forInputs(CLOSE_TO_START)
                                .checkAssert { expectVisitNodesOfSingleLinkUnidirectionally(it, nodesReversed = true) }
                        }

                        @Test
                        fun whenSnapPointLocationIsAtMidpoint() {
                            forInputs(AT_MIDPOINT)
                                .checkAssert { expectVisitNodesOfSingleLinkUnidirectionally(it, nodesReversed = true) }
                        }

                        @Test
                        fun whenSnapPointLocationIsCloserToEndNode() {
                            forInputs(CLOSE_TO_END)
                                .checkAssert { expectVisitSingleNode(it) }
                        }
                    }
                }

                @Nested
                @DisplayName("When further node from snapped start link location DOES NOT appear at start of via nodes")
                inner class WhenFurtherNodeFromSnappedStartLinkLocationDoesNotAppearAtStartOfViaNodes {

                    fun forInputs(linkDirection: LinkDirection): TheoryBuilder<VisitedNodesResolverParams> {
                        return forAll(withStartLink(DISCRETE_LINKS,
                                                    TerminusLinkProperties(DISCRETE_NODES,
                                                                           linkDirection,
                                                                           BETWEEN_ENDPOINTS_EXCLUSIVE),
                                                    VIA_NODES_NOT_STARTING_OR_ENDING_WITH_NODES_OF_TERMINUS_LINKS))
                    }

                    @Test
                    fun whenStartLinkIsBidirectional() {
                        forInputs(BIDIRECTIONAL)
                            .checkAssert { expectVisitNodesOfSingleLinkBidirectionally(it) }
                    }

                    @Test
                    fun whenStartLinkIsAlongDigitisedDirection() {
                        forInputs(ONE_WAY_ALONG_DIGITISED_DIRECTION)
                            .checkAssert { expectVisitNodesOfSingleLinkUnidirectionally(it) }
                    }

                    @Test
                    fun whenStartLinkIsAgainstDigitisedDirection() {
                        forInputs(ONE_WAY_AGAINST_DIGITISED_DIRECTION)
                            .checkAssert { expectVisitNodesOfSingleLinkUnidirectionally(it, nodesReversed = true) }
                    }
                }
            }
        }

        @Nested
        @DisplayName("Verify visited nodes on end link")
        inner class VerifyVisitedNodesOnEndLink {

            fun expectVisitSingleNode(input: VisitedNodesResolverParams) {
                assertThat(createOutput(input))
                    .isInstanceOfSatisfying(VisitNodesOnMultipleLinks::class.java) { visit ->

                        assertThat(visit.nodesToVisitOnEndLink)
                            .isInstanceOfSatisfying(VisitSingleNode::class.java) {

                                assertThat(it.nodeId)
                                    .isEqualTo(input.endLink.closerNodeId)
                            }
                    }
            }

            fun expectVisitNodesOfSingleLinkUnidirectionally(input: VisitedNodesResolverParams,
                                                             nodesReversed: Boolean = false) {

                assertThat(createOutput(input))
                    .isInstanceOfSatisfying(VisitNodesOnMultipleLinks::class.java) { visit ->

                        assertThat(visit.nodesToVisitOnEndLink)
                            .isInstanceOfSatisfying(VisitNodesOfSingleLinkUnidirectionally::class.java) {

                                val snappedLink: SnappedLinkState = input.endLink

                                if (nodesReversed) {
                                    assertThat(it.startNodeId).isEqualTo(snappedLink.endNodeId)
                                    assertThat(it.endNodeId).isEqualTo(snappedLink.startNodeId)
                                } else {
                                    assertThat(it.startNodeId).isEqualTo(snappedLink.startNodeId)
                                    assertThat(it.endNodeId).isEqualTo(snappedLink.endNodeId)
                                }
                            }
                    }
            }

            fun expectVisitNodesOfSingleLinkBidirectionally(input: VisitedNodesResolverParams) {
                assertThat(createOutput(input))
                    .isInstanceOfSatisfying(VisitNodesOnMultipleLinks::class.java) { visit ->

                        assertThat(visit.nodesToVisitOnEndLink)
                            .isInstanceOfSatisfying(VisitNodesOfSingleLinkBidirectionally::class.java) {

                                val snappedLink: SnappedLinkState = input.endLink

                                assertThat(it.firstNodeId).isEqualTo(snappedLink.startNodeId)
                                assertThat(it.secondNodeId).isEqualTo(snappedLink.endNodeId)
                            }
                    }
            }

            @Test
            fun whenEndLinkHasNonDiscreteNodes() {
                forAll(withEndLink(DISCRETE_LINKS,
                                   TerminusLinkProperties.NON_DISCRETE_NODES,
                                   ViaNodeGenerationScheme.ANY))
                    .checkAssert { expectVisitSingleNode(it) }
            }

            @Nested
            @DisplayName("When end link has discrete nodes")
            inner class WhenEndLinkHasDiscreteNodes {

                @Test
                fun whenSnappedToStartNodeOfEndLink() {
                    forAll(withEndLink(DISCRETE_LINKS,
                                       TerminusLinkProperties(DISCRETE_NODES,
                                                              LinkDirection.ANY,
                                                              AT_START),
                                       ViaNodeGenerationScheme.ANY))
                        .checkAssert { expectVisitSingleNode(it) }
                }

                @Test
                fun whenSnappedToEndNodeOfEndLink() {
                    forAll(withEndLink(DISCRETE_LINKS,
                                       TerminusLinkProperties(DISCRETE_NODES,
                                                              LinkDirection.ANY,
                                                              AT_END),
                                       ViaNodeGenerationScheme.ANY))
                        .checkAssert { expectVisitSingleNode(it) }
                }

                @Nested
                @DisplayName("When further node from snapped end link location appears at end of via nodes")
                inner class WhenFurtherNodeFromSnappedEndLinkLocationAppearsAtEndOfViaNodes {

                    fun forInputs(linkDirection: LinkDirection, snapPointLocation: SnapPointLocation)
                        : TheoryBuilder<VisitedNodesResolverParams> {

                        val genParams: Gen<VisitedNodesResolverParams> =
                            withEndLink(DISCRETE_LINKS_UNCONNECTED,
                                        TerminusLinkProperties(DISCRETE_NODES, linkDirection, snapPointLocation),
                                        VIA_NODES_NOT_STARTING_OR_ENDING_WITH_NODES_OF_TERMINUS_LINKS)
                                .map { params ->
                                    params.run {
                                        withViaNodeIds(viaNodeIds + endLink.furtherNodeId)
                                    }
                                }
                                .describedAs { prettyPrint(it) }

                        return forAll(genParams)
                    }

                    @Test
                    fun whenEndLinkIsBidirectional() {
                        forInputs(BIDIRECTIONAL,
                                  BETWEEN_ENDPOINTS_EXCLUSIVE)
                            .checkAssert { expectVisitSingleNode(it) }
                    }

                    @Nested
                    @DisplayName("When end link is one-way and direction is along digitised direction")
                    inner class WhenEndLinkIsAlongDigitisedDirection {

                        fun forInputs(snapPointLocation: SnapPointLocation): TheoryBuilder<VisitedNodesResolverParams> =
                            forInputs(ONE_WAY_ALONG_DIGITISED_DIRECTION,
                                      snapPointLocation)

                        @Test
                        fun whenSnapPointLocationIsCloserToStartNode() {
                            forInputs(CLOSE_TO_START)
                                .checkAssert { expectVisitNodesOfSingleLinkUnidirectionally(it) }
                        }

                        @Test
                        fun whenSnapPointLocationIsAtMidpoint() {
                            forInputs(AT_MIDPOINT)
                                .checkAssert { expectVisitNodesOfSingleLinkUnidirectionally(it) }
                        }

                        @Test
                        fun whenSnapPointLocationIsCloserToEndNode() {
                            forInputs(CLOSE_TO_END)
                                .checkAssert { expectVisitSingleNode(it) }
                        }
                    }

                    @Nested
                    @DisplayName("When end link is one-way and direction is against digitised direction")
                    inner class WhenEndLinkIsAgainstDigitisedDirection {

                        fun forInputs(snapPointLocation: SnapPointLocation): TheoryBuilder<VisitedNodesResolverParams> =
                            forInputs(ONE_WAY_AGAINST_DIGITISED_DIRECTION,
                                      snapPointLocation)

                        @Test
                        fun whenSnapPointLocationIsCloserToStartNode() {
                            forInputs(CLOSE_TO_START)
                                .checkAssert { expectVisitSingleNode(it) }
                        }

                        @Test
                        fun whenSnapPointLocationIsAtMidpoint() {
                            forInputs(AT_MIDPOINT)
                                .checkAssert { expectVisitSingleNode(it) }
                        }

                        @Test
                        fun whenSnapPointLocationIsCloserToEndNode() {
                            forInputs(CLOSE_TO_END)
                                .checkAssert { expectVisitNodesOfSingleLinkUnidirectionally(it, nodesReversed = true) }
                        }
                    }
                }

                @Nested
                @DisplayName("When further node from snapped end link location DOES NOT appear at end of via nodes")
                inner class WhenFurtherNodeFromSnappedEndLinkLocationDoesNotAppearAtEndOfViaNodes {

                    fun forInputs(linkDirection: LinkDirection): TheoryBuilder<VisitedNodesResolverParams> {
                        return forAll(withEndLink(DISCRETE_LINKS,
                                                  TerminusLinkProperties(DISCRETE_NODES,
                                                                         linkDirection,
                                                                         BETWEEN_ENDPOINTS_EXCLUSIVE),
                                                  VIA_NODES_NOT_STARTING_OR_ENDING_WITH_NODES_OF_TERMINUS_LINKS))
                    }

                    @Test
                    fun whenEndLinkIsBidirectional() {
                        forInputs(BIDIRECTIONAL)
                            .checkAssert { expectVisitNodesOfSingleLinkBidirectionally(it) }
                    }

                    @Test
                    fun whenEndLinkIsAlongDigitisedDirection() {
                        forInputs(ONE_WAY_ALONG_DIGITISED_DIRECTION)
                            .checkAssert { expectVisitNodesOfSingleLinkUnidirectionally(it) }
                    }

                    @Test
                    fun whenEndLinkIsAgainstDigitisedDirection() {
                        forInputs(ONE_WAY_AGAINST_DIGITISED_DIRECTION)
                            .checkAssert { expectVisitNodesOfSingleLinkUnidirectionally(it, nodesReversed = true) }
                    }
                }
            }
        }
    }

    companion object {

        private fun forAll(inputGen: Gen<VisitedNodesResolverParams>, randomnessSeed: Long = System.nanoTime())
            : TheoryBuilder<VisitedNodesResolverParams> {

            return qt()
                .withFixedSeed(randomnessSeed)
                .forAll(inputGen)
        }

        private fun createInput(terminusLinkRelation: TerminusLinkRelation,
                                startLinkProperties: TerminusLinkProperties,
                                endLinkProperties: TerminusLinkProperties,
                                viaNodeGenerationScheme: ViaNodeGenerationScheme)
            : Gen<VisitedNodesResolverParams> {

            return VisitedNodesResolverParamsGenerator
                .builder()
                .withTerminusLinkRelation(terminusLinkRelation)
                .withStartLinkProperties(startLinkProperties)
                .withEndLinkProperties(endLinkProperties)
                .withViaNodeGenerationScheme(viaNodeGenerationScheme)
                .build()
                .describedAs(this::prettyPrint)
        }

        private fun createInput(terminusLinkRelation: TerminusLinkRelation,
                                viaNodeGenerationScheme: ViaNodeGenerationScheme)
            : Gen<VisitedNodesResolverParams> {

            return createInput(terminusLinkRelation,
                               TerminusLinkProperties.ANY_VALUES,
                               TerminusLinkProperties.ANY_VALUES,
                               viaNodeGenerationScheme)
        }

        private fun withSingleLink(linkProperties: TerminusLinkProperties,
                                   viaNodeGenerationScheme: ViaNodeGenerationScheme)
            : Gen<VisitedNodesResolverParams> {

            return withTerminusLink(SAME_LINK, linkProperties, viaNodeGenerationScheme, true)
        }

        private fun withStartLink(terminusLinkRelation: TerminusLinkRelation,
                                  startLinkProperties: TerminusLinkProperties,
                                  viaNodeGenerationScheme: ViaNodeGenerationScheme)
            : Gen<VisitedNodesResolverParams> {

            require(terminusLinkRelation != SAME_LINK)

            return withTerminusLink(terminusLinkRelation, startLinkProperties, viaNodeGenerationScheme, true)
        }

        private fun withEndLink(terminusLinkRelation: TerminusLinkRelation,
                                endLinkProperties: TerminusLinkProperties,
                                viaNodeGenerationScheme: ViaNodeGenerationScheme)
            : Gen<VisitedNodesResolverParams> {

            require(terminusLinkRelation != SAME_LINK)

            return withTerminusLink(terminusLinkRelation, endLinkProperties, viaNodeGenerationScheme, false)
        }

        private fun withTerminusLink(terminusLinkRelation: TerminusLinkRelation,
                                     linkProperties: TerminusLinkProperties,
                                     viaNodeGenerationScheme: ViaNodeGenerationScheme,
                                     isStartLink: Boolean)
            : Gen<VisitedNodesResolverParams> {

            require(terminusLinkRelation != TerminusLinkRelation.ANY)

            return createInput(terminusLinkRelation,
                               if (isStartLink) linkProperties else TerminusLinkProperties.ANY_VALUES,
                               if (isStartLink) TerminusLinkProperties.ANY_VALUES else linkProperties,
                               viaNodeGenerationScheme)
        }

        private fun prettyPrint(params: VisitedNodesResolverParams): String = params.run {
            // Making assertion failures more readable.
            """
                {
                    startLink: {
                        id: ${startLink.infrastructureLinkId},
                        closestPointFractionalMeasure: ${startLink.closestPointFractionalMeasure},
                        trafficFlowDirectionType: ${startLink.trafficFlowDirectionType},
                        startNodeId: ${startLink.startNodeId},
                        endNodeId: ${startLink.endNodeId}
                    },
                    viaNodeIds: $viaNodeIds,
                    endLink: {
                        id: ${endLink.infrastructureLinkId},
                        closestPointFractionalMeasure: ${endLink.closestPointFractionalMeasure},
                        trafficFlowDirectionType: ${endLink.trafficFlowDirectionType},
                        startNodeId: ${endLink.startNodeId},
                        endNodeId: ${endLink.endNodeId}
                    }
                }
            """.trimIndent()
        }

        private fun createOutput(params: VisitedNodesResolverParams): VisitedNodes {
            return VisitedNodesResolver.resolve(params.startLink,
                                                params.viaNodeIds,
                                                params.endLink)
        }
    }
}
