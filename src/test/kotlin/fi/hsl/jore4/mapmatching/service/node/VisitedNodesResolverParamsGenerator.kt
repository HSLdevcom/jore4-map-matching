package fi.hsl.jore4.mapmatching.service.node

import fi.hsl.jore4.mapmatching.model.InfrastructureNodeId
import fi.hsl.jore4.mapmatching.model.TrafficFlowDirectionType
import fi.hsl.jore4.mapmatching.model.TrafficFlowDirectionType.AGAINST_DIGITISED_DIRECTION
import fi.hsl.jore4.mapmatching.model.TrafficFlowDirectionType.ALONG_DIGITISED_DIRECTION
import fi.hsl.jore4.mapmatching.repository.infrastructure.SnappedPointOnLink
import fi.hsl.jore4.mapmatching.service.node.VisitedNodesResolverParamsGenerator.LinkDirection.BIDIRECTIONAL
import fi.hsl.jore4.mapmatching.service.node.VisitedNodesResolverParamsGenerator.LinkDirection.ONE_WAY
import fi.hsl.jore4.mapmatching.service.node.VisitedNodesResolverParamsGenerator.LinkDirection.ONE_WAY_AGAINST_DIGITISED_DIRECTION
import fi.hsl.jore4.mapmatching.service.node.VisitedNodesResolverParamsGenerator.LinkDirection.ONE_WAY_ALONG_DIGITISED_DIRECTION
import fi.hsl.jore4.mapmatching.service.node.VisitedNodesResolverParamsGenerator.LinkEndpointDiscreteness.DISCRETE_NODES
import fi.hsl.jore4.mapmatching.service.node.VisitedNodesResolverParamsGenerator.LinkEndpointDiscreteness.NON_DISCRETE_NODES
import fi.hsl.jore4.mapmatching.service.node.VisitedNodesResolverParamsGenerator.SnapPointLocation.AT_END
import fi.hsl.jore4.mapmatching.service.node.VisitedNodesResolverParamsGenerator.SnapPointLocation.AT_MIDPOINT
import fi.hsl.jore4.mapmatching.service.node.VisitedNodesResolverParamsGenerator.SnapPointLocation.AT_OR_CLOSE_TO_END
import fi.hsl.jore4.mapmatching.service.node.VisitedNodesResolverParamsGenerator.SnapPointLocation.AT_OR_CLOSE_TO_START
import fi.hsl.jore4.mapmatching.service.node.VisitedNodesResolverParamsGenerator.SnapPointLocation.AT_START
import fi.hsl.jore4.mapmatching.service.node.VisitedNodesResolverParamsGenerator.SnapPointLocation.BETWEEN_ENDPOINTS_EXCLUSIVE
import fi.hsl.jore4.mapmatching.service.node.VisitedNodesResolverParamsGenerator.SnapPointLocation.CLOSE_TO_END
import fi.hsl.jore4.mapmatching.service.node.VisitedNodesResolverParamsGenerator.SnapPointLocation.CLOSE_TO_START
import fi.hsl.jore4.mapmatching.service.node.VisitedNodesResolverParamsGenerator.TerminusLinkRelation.DISCRETE_LINKS
import fi.hsl.jore4.mapmatching.service.node.VisitedNodesResolverParamsGenerator.TerminusLinkRelation.DISCRETE_LINKS_CONNECTED
import fi.hsl.jore4.mapmatching.service.node.VisitedNodesResolverParamsGenerator.TerminusLinkRelation.DISCRETE_LINKS_UNCONNECTED
import fi.hsl.jore4.mapmatching.service.node.VisitedNodesResolverParamsGenerator.TerminusLinkRelation.SAME_LINK
import fi.hsl.jore4.mapmatching.service.node.VisitedNodesResolverParamsGenerator.ViaNodeGenerationScheme.VIA_NODES_EMPTY
import fi.hsl.jore4.mapmatching.service.node.VisitedNodesResolverParamsGenerator.ViaNodeGenerationScheme.VIA_NODES_FULLY_REDUNDANT
import fi.hsl.jore4.mapmatching.service.node.VisitedNodesResolverParamsGenerator.ViaNodeGenerationScheme.VIA_NODES_NON_REDUNDANT
import fi.hsl.jore4.mapmatching.service.node.VisitedNodesResolverParamsGenerator.ViaNodeGenerationScheme.VIA_NODES_NOT_STARTING_OR_ENDING_WITH_NODES_OF_TERMINUS_LINKS
import fi.hsl.jore4.mapmatching.service.node.VisitedNodesResolverParamsGenerator.ViaNodeGenerationScheme.VIA_NODES_RANDOM
import fi.hsl.jore4.mapmatching.test.generators.EnumGenerators.locationAlongLinkType
import fi.hsl.jore4.mapmatching.test.generators.EnumGenerators.trafficFlowDirectionType
import fi.hsl.jore4.mapmatching.test.generators.Retry
import fi.hsl.jore4.mapmatching.test.generators.SnapPointLocationAlongLinkFilter
import fi.hsl.jore4.mapmatching.test.generators.SnappedPointOnLinkGenerator.SnappedPointOnLinkParams
import fi.hsl.jore4.mapmatching.test.generators.SnappedPointOnLinkGenerator.snapSingleLinkTwice
import fi.hsl.jore4.mapmatching.test.generators.SnappedPointOnLinkGenerator.snapTwoConnectedLinks
import fi.hsl.jore4.mapmatching.test.generators.SnappedPointOnLinkGenerator.snapTwoUnconnectedLinks
import org.quicktheories.core.Gen
import org.quicktheories.generators.Generate.booleans
import org.quicktheories.generators.Generate.constant
import org.quicktheories.generators.Generate.oneOf
import org.quicktheories.generators.Generate.pick
import org.quicktheories.generators.SourceDSL.integers
import org.quicktheories.generators.SourceDSL.lists
import fi.hsl.jore4.mapmatching.test.generators.InfrastructureNodeIdGenerator.infrastructureNodeId as nodeId

object VisitedNodesResolverParamsGenerator {
    private const val MAX_NUMBER_OF_VIA_NODES: Int = 6

    /**
     * Describes how start and end link on a route relate to each other. Basically, dictates how IDs and nodes
     * are populated into startLink/endLink in NodeResolutionParams.
     */
    enum class TerminusLinkRelation {
        /**
         * startLinkId == endLinkId
         */
        SAME_LINK,

        /**
         * startLinkId != endLinkId; links may or may not be connected to each other via nodes
         */
        DISCRETE_LINKS,

        /**
         * startLinkId != endLinkId; start and end link share a common node i.e. they are connected to each other
         */
        DISCRETE_LINKS_CONNECTED,

        /**
         * startLinkId != endLinkId; start and end link do not have a common node i.e. they are unconnected
         */
        DISCRETE_LINKS_UNCONNECTED,

        ANY
    }

    /**
     * Determines whether infrastructure link should have discrete endpoint nodes.
     */
    enum class LinkEndpointDiscreteness {
        DISCRETE_NODES,

        // A single infrastructure node appears as both a start and end node of an infrastructure link.
        NON_DISCRETE_NODES,

        ANY
    }

    /**
     * Determines direction of traffic flow on infrastructure link.
     */
    enum class LinkDirection {
        BIDIRECTIONAL,

        ONE_WAY,
        ONE_WAY_ALONG_DIGITISED_DIRECTION,
        ONE_WAY_AGAINST_DIGITISED_DIRECTION,

        ANY
    }

    enum class SnapPointLocation {
        /**
         * Snap point on link is at start node of infrastructure link.
         */
        AT_START,

        /**
         * Snap point on link is at end node of infrastructure link.
         */
        AT_END,

        /**
         * Snap point on link is either at or closer to start node of infrastructure link.
         */
        AT_OR_CLOSE_TO_START,

        /**
         * Snap point on link is either at or closer to end node of infrastructure link.
         */
        AT_OR_CLOSE_TO_END,

        /**
         * Snap point on link is closer to start node of infrastructure link.
         */
        CLOSE_TO_START,

        /**
         * Snap point on link is closer to end node of infrastructure link.
         */
        CLOSE_TO_END,

        /**
         * Snap point on link is at midpoint of link between start and end node.
         */
        AT_MIDPOINT,

        BETWEEN_ENDPOINTS_EXCLUSIVE,

        ANY
    }

    /**
     * Determines what kind of via nodes are populated into NodeResolutionParams.
     */
    enum class ViaNodeGenerationScheme {
        /**
         * An empty set of via nodes is generated.
         */
        VIA_NODES_EMPTY,

        VIA_NODES_RANDOM,

        /**
         * Generate sequence of via node IDs starting with a subsequence of duplicated node ID that belongs to closer
         * node from the snapped location on start link and ending with a subsequence of duplicated node ID that belongs
         * to closer node from the snapped location on end link.
         */
        VIA_NODES_FULLY_REDUNDANT,

        /**
         * Generate a sequence of via node IDs which does not start or end with a node ID that belongs to closer node
         * from either of two snapped locations on terminus links.
         */
        VIA_NODES_NON_REDUNDANT,

        /**
         * Generate a sequence of via node IDs which does not start or end with a node ID that belongs to start or
         * end link.
         */
        VIA_NODES_NOT_STARTING_OR_ENDING_WITH_NODES_OF_TERMINUS_LINKS,

        ANY
    }

    private fun generateLinkEndpointDiscreteness(endpointDiscreteness: LinkEndpointDiscreteness): Gen<Boolean> =
        when (endpointDiscreteness) {
            DISCRETE_NODES -> constant(true)
            NON_DISCRETE_NODES -> constant(false)
            LinkEndpointDiscreteness.ANY -> booleans()
        }

    private fun generateTrafficFlowDirectionType(direction: LinkDirection): Gen<TrafficFlowDirectionType> =
        when (direction) {
            BIDIRECTIONAL -> constant(TrafficFlowDirectionType.BIDIRECTIONAL)
            ONE_WAY -> integers().between(3, 4).map(TrafficFlowDirectionType::from)
            ONE_WAY_ALONG_DIGITISED_DIRECTION -> constant(ALONG_DIGITISED_DIRECTION)
            ONE_WAY_AGAINST_DIGITISED_DIRECTION -> constant(AGAINST_DIGITISED_DIRECTION)
            LinkDirection.ANY -> trafficFlowDirectionType()
        }

    private fun generateSnapPointLocationFilter(
        snapPointLocation: SnapPointLocation
    ): Gen<SnapPointLocationAlongLinkFilter> =
        when (snapPointLocation) {
            AT_START -> constant(SnapPointLocationAlongLinkFilter.AT_START)
            AT_END -> constant(SnapPointLocationAlongLinkFilter.AT_END)

            AT_OR_CLOSE_TO_START -> constant(SnapPointLocationAlongLinkFilter.AT_OR_CLOSE_TO_START)
            AT_OR_CLOSE_TO_END -> constant(SnapPointLocationAlongLinkFilter.AT_OR_CLOSE_TO_END)

            CLOSE_TO_START -> constant(SnapPointLocationAlongLinkFilter.CLOSE_TO_START)
            CLOSE_TO_END -> constant(SnapPointLocationAlongLinkFilter.CLOSE_TO_END)

            AT_MIDPOINT -> constant(SnapPointLocationAlongLinkFilter.AT_MIDPOINT)
            BETWEEN_ENDPOINTS_EXCLUSIVE -> constant(SnapPointLocationAlongLinkFilter.BETWEEN_ENDPOINTS_EXCLUSIVE)

            SnapPointLocation.ANY -> locationAlongLinkType()
        }

    private fun generateList(
        source: Gen<InfrastructureNodeId>,
        amount: Int
    ): Gen<List<InfrastructureNodeId>> = lists().of(source).ofSize(amount)

    private fun generateNonRedundantViaNodes(
        pointOnStartLink: SnappedPointOnLink,
        pointOnEndLink: SnappedPointOnLink
    ): Gen<List<InfrastructureNodeId>> =
        integers().between(1, MAX_NUMBER_OF_VIA_NODES).flatMap { numViaNodes ->

            val genFirstNodeId: Gen<InfrastructureNodeId> =
                Retry(nodeId()) {
                    it != pointOnStartLink.closerNodeId &&
                        (
                            it != pointOnEndLink.closerNodeId ||
                                !pointOnStartLink.isOnSameLinkAs(
                                    pointOnEndLink
                                ) &&
                                numViaNodes > 1
                        )
                }

            val genLastNodeId: Gen<InfrastructureNodeId> = Retry(nodeId()) { it != pointOnEndLink.closerNodeId }

            generateViaNodes(numViaNodes, genFirstNodeId, genLastNodeId)
        }

    private fun generateViaNodesNotStartingOrEndingWithNodesOfTerminusLinks(
        pointOnStartLink: SnappedPointOnLink,
        pointOnEndLink: SnappedPointOnLink
    ): Gen<List<InfrastructureNodeId>> {
        val genTerminusNodeId: Gen<InfrastructureNodeId> =
            Retry(nodeId()) {
                !pointOnStartLink.isOnLinkTerminatedByNode(it) && !pointOnEndLink.isOnLinkTerminatedByNode(it)
            }

        return integers()
            .between(1, MAX_NUMBER_OF_VIA_NODES)
            .flatMap { numViaNodes ->
                generateViaNodes(numViaNodes, genTerminusNodeId, genTerminusNodeId)
            }
    }

    private fun generateViaNodes(
        numberOfNodeIds: Int,
        genFirstNodeId: Gen<InfrastructureNodeId>,
        genLastNodeId: Gen<InfrastructureNodeId>
    ): Gen<List<InfrastructureNodeId>> =
        when (numberOfNodeIds) {
            1 -> genFirstNodeId.map { node -> listOf(node) }
            2 -> genFirstNodeId.zip(genLastNodeId) { firstNode, secondNode -> listOf(firstNode, secondNode) }
            else -> {
                val genMiddleNodeIds: Gen<List<InfrastructureNodeId>> = generateList(nodeId(), numberOfNodeIds - 2)

                genFirstNodeId.zip(
                    genMiddleNodeIds,
                    genLastNodeId
                ) { firstNode, middleNodes, lastNode ->

                    listOf(firstNode) + middleNodes + lastNode
                }
            }
        }

    private fun generateFullyRedundantViaNodes(
        pointOnStartLink: SnappedPointOnLink,
        pointOnEndLink: SnappedPointOnLink
    ): Gen<List<InfrastructureNodeId>> =
        integers().between(1, MAX_NUMBER_OF_VIA_NODES).flatMap { numViaNodes ->

            integers().between(0, numViaNodes).flatMap { sizeOfFirstSequence ->

                val sizeOfSecondSequence = numViaNodes - sizeOfFirstSequence

                val firstSequence: Gen<List<InfrastructureNodeId>> =
                    generateList(constant(pointOnStartLink.closerNodeId), sizeOfFirstSequence)

                val secondSequence: Gen<List<InfrastructureNodeId>> =
                    generateList(constant(pointOnEndLink.closerNodeId), sizeOfSecondSequence)

                firstSequence.zip(secondSequence, Collection<InfrastructureNodeId>::plus)
            }
        }

    data class TerminusLinkProperties(
        val endpointDiscreteness: LinkEndpointDiscreteness,
        val direction: LinkDirection,
        val snapPointLocation: SnapPointLocation
    ) {
        companion object {
            val ANY_VALUES =
                TerminusLinkProperties(
                    LinkEndpointDiscreteness.ANY,
                    LinkDirection.ANY,
                    SnapPointLocation.ANY
                )

            val NON_DISCRETE_NODES = from(LinkEndpointDiscreteness.NON_DISCRETE_NODES)

            fun from(endpointDiscreteness: LinkEndpointDiscreteness) =
                TerminusLinkProperties(
                    endpointDiscreteness,
                    LinkDirection.ANY,
                    SnapPointLocation.ANY
                )
        }
    }

    fun builder() = Builder()

    class Builder {
        private var terminusLinkRelation: TerminusLinkRelation = TerminusLinkRelation.ANY
        private var startLinkProperties: TerminusLinkProperties = TerminusLinkProperties.ANY_VALUES
        private var endLinkProperties: TerminusLinkProperties = TerminusLinkProperties.ANY_VALUES
        private var viaNodeScheme: ViaNodeGenerationScheme = ViaNodeGenerationScheme.ANY

        fun withTerminusLinkRelation(value: TerminusLinkRelation): Builder {
            terminusLinkRelation = value
            return this
        }

        fun withStartLinkProperties(properties: TerminusLinkProperties): Builder {
            startLinkProperties = properties
            return this
        }

        fun withEndLinkProperties(properties: TerminusLinkProperties): Builder {
            endLinkProperties = properties
            return this
        }

        fun withViaNodeGenerationScheme(value: ViaNodeGenerationScheme): Builder {
            viaNodeScheme = value
            return this
        }

        fun build(): Gen<VisitedNodesResolverParams> =
            generatePointsOnTerminusLinks().flatMap { (pointOnStartLink, pointOnEndLink) ->

                fun generateRandomViaNodeIds(): Gen<List<InfrastructureNodeId>> =
                    lists()
                        .of(nodeId())
                        .ofSizeBetween(1, MAX_NUMBER_OF_VIA_NODES)

                fun generateEmptyViaNodeIds(): Gen<List<InfrastructureNodeId>> = constant(emptyList())

                val genViaNodeIds: Gen<List<InfrastructureNodeId>> =
                    when (viaNodeScheme) {
                        VIA_NODES_EMPTY -> generateEmptyViaNodeIds()
                        VIA_NODES_RANDOM -> generateRandomViaNodeIds()
                        VIA_NODES_FULLY_REDUNDANT -> generateFullyRedundantViaNodes(pointOnStartLink, pointOnEndLink)
                        VIA_NODES_NON_REDUNDANT -> generateNonRedundantViaNodes(pointOnStartLink, pointOnEndLink)
                        VIA_NODES_NOT_STARTING_OR_ENDING_WITH_NODES_OF_TERMINUS_LINKS -> {
                            oneOf(
                                generateEmptyViaNodeIds(),
                                generateViaNodesNotStartingOrEndingWithNodesOfTerminusLinks(
                                    pointOnStartLink,
                                    pointOnEndLink
                                )
                            )
                        }

                        ViaNodeGenerationScheme.ANY -> oneOf(generateEmptyViaNodeIds(), generateRandomViaNodeIds())
                    }

                genViaNodeIds.map { viaNodeIds ->
                    VisitedNodesResolverParams(pointOnStartLink, viaNodeIds, pointOnEndLink)
                }
            }

        private fun generatePointsOnTerminusLinks(): Gen<Pair<SnappedPointOnLink, SnappedPointOnLink>> =
            unwrapTerminusLinkRelation().flatMap { terminusLinkRelation ->

                if (terminusLinkRelation == SAME_LINK) {
                    snapSingleLinkTwice()
                } else {
                    snapTwoDiscreteLinks(terminusLinkRelation)
                }
            }

        private fun unwrapTerminusLinkRelation(): Gen<TerminusLinkRelation> {
            val unwrapAny: Gen<TerminusLinkRelation> =
                when (terminusLinkRelation) {
                    TerminusLinkRelation.ANY -> pick(listOf(SAME_LINK, DISCRETE_LINKS))
                    else -> constant(terminusLinkRelation)
                }

            return unwrapAny.flatMap { linkRel ->
                when (linkRel) {
                    DISCRETE_LINKS ->
                        pick(
                            listOf(
                                DISCRETE_LINKS_CONNECTED,
                                DISCRETE_LINKS_UNCONNECTED
                            )
                        )

                    else -> constant(linkRel)
                }
            }
        }

        private fun snapSingleLinkTwice(): Gen<Pair<SnappedPointOnLink, SnappedPointOnLink>> =
            generateParamsForPointOnStartLink().flatMap { pointOnStartLinkParams ->

                snapSingleLinkTwice(
                    pointOnStartLinkParams.hasDiscreteNodes,
                    pointOnStartLinkParams.trafficFlowDirectionType,
                    pointOnStartLinkParams.snapPointLocationFilter,
                    SnapPointLocationAlongLinkFilter.AT_MIDPOINT
                )
            }

        private fun snapTwoDiscreteLinks(
            terminusLinkRelation: TerminusLinkRelation
        ): Gen<Pair<SnappedPointOnLink, SnappedPointOnLink>> =
            generateParamsForPointOnStartLink().flatMap { pointOnStartLinkParams ->
                generateParamsForPointOnEndLink().flatMap { pointOnEndLinkParams ->

                    when (terminusLinkRelation) {
                        DISCRETE_LINKS_CONNECTED ->
                            snapTwoConnectedLinks(pointOnStartLinkParams, pointOnEndLinkParams)

                        DISCRETE_LINKS_UNCONNECTED ->
                            snapTwoUnconnectedLinks(pointOnStartLinkParams, pointOnEndLinkParams)

                        else -> throw IllegalStateException("Should not end up here")
                    }
                }
            }

        private fun generateParamsForPointOnStartLink(): Gen<SnappedPointOnLinkParams> =
            generateSnappedPointOnLinkParams(startLinkProperties)

        private fun generateParamsForPointOnEndLink(): Gen<SnappedPointOnLinkParams> =
            generateSnappedPointOnLinkParams(endLinkProperties)

        private fun generateSnappedPointOnLinkParams(
            linkProperties: TerminusLinkProperties
        ): Gen<SnappedPointOnLinkParams> =
            generateLinkEndpointDiscreteness(linkProperties.endpointDiscreteness)
                .flatMap { hasDiscreteNodes ->

                    generateTrafficFlowDirectionType(linkProperties.direction)
                        .flatMap { trafficFlowDirectionType ->

                            generateSnapPointLocationFilter(linkProperties.snapPointLocation)
                                .map { snapPointLocationFilter ->

                                    SnappedPointOnLinkParams(
                                        hasDiscreteNodes,
                                        trafficFlowDirectionType,
                                        snapPointLocationFilter
                                    )
                                }
                        }
                }
    }
}
