package fi.hsl.jore4.mapmatching.service.node

import fi.hsl.jore4.mapmatching.model.NodeProximity
import fi.hsl.jore4.mapmatching.model.TrafficFlowDirectionType
import fi.hsl.jore4.mapmatching.model.TrafficFlowDirectionType.AGAINST_DIGITISED_DIRECTION
import fi.hsl.jore4.mapmatching.model.TrafficFlowDirectionType.ALONG_DIGITISED_DIRECTION
import fi.hsl.jore4.mapmatching.repository.infrastructure.SnappedLinkState
import fi.hsl.jore4.mapmatching.service.node.NodeResolutionParamsGenerator.LinkDirection.BIDIRECTIONAL
import fi.hsl.jore4.mapmatching.service.node.NodeResolutionParamsGenerator.LinkDirection.ONE_WAY
import fi.hsl.jore4.mapmatching.service.node.NodeResolutionParamsGenerator.LinkDirection.ONE_WAY_AGAINST_DIGITISED_DIRECTION
import fi.hsl.jore4.mapmatching.service.node.NodeResolutionParamsGenerator.LinkDirection.ONE_WAY_ALONG_DIGITISED_DIRECTION
import fi.hsl.jore4.mapmatching.service.node.NodeResolutionParamsGenerator.LinkEndpointDiscreteness.DISCRETE_NODES
import fi.hsl.jore4.mapmatching.service.node.NodeResolutionParamsGenerator.LinkEndpointDiscreteness.NON_DISCRETE_NODES
import fi.hsl.jore4.mapmatching.service.node.NodeResolutionParamsGenerator.TerminusLinkRelation.DISCRETE_LINKS
import fi.hsl.jore4.mapmatching.service.node.NodeResolutionParamsGenerator.TerminusLinkRelation.DISCRETE_LINKS_CONNECTED
import fi.hsl.jore4.mapmatching.service.node.NodeResolutionParamsGenerator.TerminusLinkRelation.DISCRETE_LINKS_UNCONNECTED
import fi.hsl.jore4.mapmatching.service.node.NodeResolutionParamsGenerator.TerminusLinkRelation.SAME_LINK
import fi.hsl.jore4.mapmatching.service.node.NodeResolutionParamsGenerator.TerminusLinkRelation.SAME_LINK_SAME_SNAP_POINT_LOCATIONS
import fi.hsl.jore4.mapmatching.service.node.NodeResolutionParamsGenerator.TerminusLinkRelation.SAME_LINK_SNAP_FIRST_POINT_CLOSER_TO_END_NODE
import fi.hsl.jore4.mapmatching.service.node.NodeResolutionParamsGenerator.TerminusLinkRelation.SAME_LINK_SNAP_FIRST_POINT_CLOSER_TO_START_NODE
import fi.hsl.jore4.mapmatching.service.node.NodeResolutionParamsGenerator.ViaNodeGenerationScheme.EMPTY
import fi.hsl.jore4.mapmatching.service.node.NodeResolutionParamsGenerator.ViaNodeGenerationScheme.FULLY_REDUNDANT_WITH_TERMINUS_LINKS
import fi.hsl.jore4.mapmatching.service.node.NodeResolutionParamsGenerator.ViaNodeGenerationScheme.NON_REDUNDANT_WITH_TERMINUS_LINKS
import fi.hsl.jore4.mapmatching.service.node.NodeResolutionParamsGenerator.ViaNodeGenerationScheme.RANDOM
import fi.hsl.jore4.mapmatching.test.generators.CommonGenerators.trafficFlowDirectionType
import fi.hsl.jore4.mapmatching.test.generators.NodeProximityGenerator.node
import fi.hsl.jore4.mapmatching.test.generators.Retry
import fi.hsl.jore4.mapmatching.test.generators.SnappedLinkStateGenerator.snapSingleLinkTwice
import fi.hsl.jore4.mapmatching.test.generators.SnappedLinkStateGenerator.snapTwoConnectedLinks
import fi.hsl.jore4.mapmatching.test.generators.SnappedLinkStateGenerator.snapTwoUnconnectedLinks
import org.quicktheories.core.Gen
import org.quicktheories.generators.Generate.booleans
import org.quicktheories.generators.Generate.constant
import org.quicktheories.generators.Generate.pick
import org.quicktheories.generators.SourceDSL.integers
import org.quicktheories.generators.SourceDSL.lists
import fi.hsl.jore4.mapmatching.test.generators.NodeProximityGenerator.node as randomNode

object NodeResolutionParamsGenerator {

    private const val MAX_NUMBER_OF_VIA_NODES: Int = 5

    /**
     * Describes how start and end link on a route relate to each other. Basically, dictates how IDs and nodes
     * are populated into startLink/endLink in NodeResolutionParams.
     */
    enum class TerminusLinkRelation(val onlyOneLinkInvolved: Boolean) {

        /**
         * startLinkId == endLinkId
         */
        SAME_LINK(true),

        /**
         * startLinkId == endLinkId; first snap point on link is closer to the start node of the infrastructure link
         */
        SAME_LINK_SNAP_FIRST_POINT_CLOSER_TO_START_NODE(true),

        /**
         * startLinkId == endLinkId; first snap point on link is closer to the end node of the infrastructure link
         */
        SAME_LINK_SNAP_FIRST_POINT_CLOSER_TO_END_NODE(true),

        /**
         * startLinkId == endLinkId; first snap point on link is closer to the start node of the infrastructure link
         */
        SAME_LINK_SAME_SNAP_POINT_LOCATIONS(true),

        /**
         * startLinkId != endLinkId; links may or may not be connected to each other via nodes
         */
        DISCRETE_LINKS(false),

        /**
         * startLinkId != endLinkId; start and end link share a common node i.e. they are connected to each other
         */
        DISCRETE_LINKS_CONNECTED(false),

        /**
         * startLinkId != endLinkId; start and end link do not have a common node i.e. they are unconnected
         */
        DISCRETE_LINKS_UNCONNECTED(false),

        ANY(false)
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

    /**
     * Determines what kind of via nodes are populated into NodeResolutionParams.
     */
    enum class ViaNodeGenerationScheme {

        /**
         * An empty set of via nodes is generated.
         */
        EMPTY,

        RANDOM,

        /**
         * Generate via node sequence which does not start or end with a node that is closer node of either snapped
         * terminus link.
         */
        NON_REDUNDANT_WITH_TERMINUS_LINKS,

        /**
         * Generate via node sequence which starts with a duplicated sequence of single node that is closer node of
         * snapped start link and ends with a duplicated sequence of single node that is closer node of snapped end
         * link.
         */
        FULLY_REDUNDANT_WITH_TERMINUS_LINKS,

        ANY
    }

    private val EMPTY_VIA_NODE_LIST: Gen<List<NodeProximity>> = constant(emptyList())

    private fun generateTrafficFlowDirectionType(direction: LinkDirection): Gen<TrafficFlowDirectionType> {
        return when (direction) {
            BIDIRECTIONAL -> constant(TrafficFlowDirectionType.BIDIRECTIONAL)
            ONE_WAY -> integers().between(3, 4).map(TrafficFlowDirectionType::from)
            ONE_WAY_ALONG_DIGITISED_DIRECTION -> constant(ALONG_DIGITISED_DIRECTION)
            ONE_WAY_AGAINST_DIGITISED_DIRECTION -> constant(AGAINST_DIGITISED_DIRECTION)
            LinkDirection.ANY -> trafficFlowDirectionType()
        }
    }

    private fun generateLinkEndpointDiscreteness(endpointDiscreteness: LinkEndpointDiscreteness): Gen<Boolean> {
        return when (endpointDiscreteness) {
            DISCRETE_NODES -> constant(true)
            NON_DISCRETE_NODES -> constant(false)
            LinkEndpointDiscreteness.ANY -> booleans()
        }
    }

    private fun generateViaNodes(nodeSource: Gen<NodeProximity>): Gen<List<NodeProximity>> {
        return lists()
            .of(nodeSource)
            .ofSizeBetween(1, MAX_NUMBER_OF_VIA_NODES)
    }

    private fun generateNonRedundantViaNodesWithRegardToTerminusLinks(startLink: SnappedLinkState,
                                                                      endLink: SnappedLinkState)
        : Gen<List<NodeProximity>> {

        return integers().between(1, MAX_NUMBER_OF_VIA_NODES).flatMap { numViaNodes ->

            val genFirstNode: Gen<NodeProximity> = Retry(randomNode()) { node ->
                node.id != startLink.closerNodeId
                    && (node.id != endLink.closerNodeId || !startLink.isOnSameLinkAs(endLink) && numViaNodes > 1)
            }

            val genLastNode: Gen<NodeProximity> = Retry(randomNode()) { node ->
                node.id != endLink.closerNodeId
            }

            when (numViaNodes) {
                1 -> genFirstNode.map { node -> listOf(node) }
                2 -> genFirstNode.zip(genLastNode) { firstNode, secondNode -> listOf(firstNode, secondNode) }
                else -> {
                    val genMiddleNodes: Gen<List<NodeProximity>> = lists().of(randomNode()).ofSize(numViaNodes - 2)

                    genFirstNode.zip(genMiddleNodes,
                                     genLastNode) { firstNode, middleNodes, lastNode ->

                        listOf(firstNode) + middleNodes + lastNode
                    }
                }
            }
        }
    }

    private fun generateFullyRedundantViaNodesWithRegardToTerminusLinks(startLink: SnappedLinkState,
                                                                        endLink: SnappedLinkState)
        : Gen<List<NodeProximity>> {

        return integers().between(1, MAX_NUMBER_OF_VIA_NODES).flatMap { numViaNodes ->

            integers().between(0, numViaNodes).flatMap { sizeOfFirstSequence ->

                val sizeOfSecondSequence = numViaNodes - sizeOfFirstSequence

                val firstSequence: Gen<List<NodeProximity>> = lists()
                    .of(node(startLink.closerNodeId))
                    .ofSize(sizeOfFirstSequence)

                val secondSequence: Gen<List<NodeProximity>> = lists()
                    .of(node(endLink.closerNodeId))
                    .ofSize(sizeOfSecondSequence)

                firstSequence.zip(secondSequence, Collection<NodeProximity>::plus)
            }
        }
    }

    fun builder() = Builder()

    class Builder {

        private var terminusLinkRelation: TerminusLinkRelation = TerminusLinkRelation.ANY

        private var startLinkEndpointDiscreteness: LinkEndpointDiscreteness = LinkEndpointDiscreteness.ANY
        private var startLinkDirection: LinkDirection = LinkDirection.ANY

        private var endLinkEndpointDiscreteness: LinkEndpointDiscreteness = LinkEndpointDiscreteness.ANY
        private var endLinkDirection: LinkDirection = LinkDirection.ANY

        private var viaNodeScheme: ViaNodeGenerationScheme = ViaNodeGenerationScheme.ANY

        fun withTerminusLinkRelation(value: TerminusLinkRelation): Builder {
            terminusLinkRelation = value
            return this
        }

        fun withStartLinkEndpointDiscreteness(value: LinkEndpointDiscreteness): Builder {
            startLinkEndpointDiscreteness = value
            return this
        }

        fun withStartLinkDirection(value: LinkDirection): Builder {
            startLinkDirection = value
            return this
        }

        fun withEndLinkEndpointDiscreteness(value: LinkEndpointDiscreteness): Builder {
            endLinkEndpointDiscreteness = value
            return this
        }

        fun withEndLinkDirection(value: LinkDirection): Builder {
            endLinkDirection = value
            return this
        }

        fun withViaNodeGenerationScheme(value: ViaNodeGenerationScheme): Builder {
            viaNodeScheme = value
            return this
        }

        fun build(): Gen<NodeResolutionParams> {
            return generateTerminusLinks().flatMap { (startLink, endLink) ->

                val genViaNodeList: Gen<List<NodeProximity>> = when (viaNodeScheme) {
                    EMPTY -> EMPTY_VIA_NODE_LIST
                    RANDOM -> generateViaNodes(randomNode())
                    NON_REDUNDANT_WITH_TERMINUS_LINKS -> {
                        generateNonRedundantViaNodesWithRegardToTerminusLinks(startLink, endLink)
                    }
                    FULLY_REDUNDANT_WITH_TERMINUS_LINKS -> {
                        generateFullyRedundantViaNodesWithRegardToTerminusLinks(startLink, endLink)
                    }
                    ViaNodeGenerationScheme.ANY -> EMPTY_VIA_NODE_LIST.mix(generateViaNodes(randomNode()), 50)
                }

                genViaNodeList.map { viaNodeList -> NodeResolutionParams(startLink, viaNodeList, endLink) }
            }
        }

        private fun generateTerminusLinks(): Gen<Pair<SnappedLinkState, SnappedLinkState>> {
            return unwrapTerminusLinkRelation().flatMap { terminusLinkRelation ->

                if (terminusLinkRelation.onlyOneLinkInvolved)
                    snapSingleLinkTwice(terminusLinkRelation)
                else
                    snapTwoDiscreteLinks(terminusLinkRelation)
            }
        }

        private fun unwrapTerminusLinkRelation(): Gen<TerminusLinkRelation> {
            val unwrapAny: Gen<TerminusLinkRelation> = when (terminusLinkRelation) {
                TerminusLinkRelation.ANY -> pick(listOf(SAME_LINK, DISCRETE_LINKS))
                else -> constant(terminusLinkRelation)
            }

            return unwrapAny.flatMap { linkRel ->
                when (linkRel) {
                    SAME_LINK -> pick(listOf(SAME_LINK_SNAP_FIRST_POINT_CLOSER_TO_START_NODE,
                                             SAME_LINK_SNAP_FIRST_POINT_CLOSER_TO_END_NODE,
                                             SAME_LINK_SAME_SNAP_POINT_LOCATIONS))

                    DISCRETE_LINKS -> pick(listOf(DISCRETE_LINKS_CONNECTED,
                                                  DISCRETE_LINKS_UNCONNECTED))

                    else -> constant(linkRel)
                }
            }
        }

        private fun snapSingleLinkTwice(terminusLinkRelation: TerminusLinkRelation)
            : Gen<Pair<SnappedLinkState, SnappedLinkState>> {

            val snapPointComparison: Int = when (terminusLinkRelation) {
                SAME_LINK_SNAP_FIRST_POINT_CLOSER_TO_START_NODE -> -1
                SAME_LINK_SNAP_FIRST_POINT_CLOSER_TO_END_NODE -> 1
                SAME_LINK_SAME_SNAP_POINT_LOCATIONS -> 0
                else -> throw IllegalStateException("Should not end up here")
            }

            return generateLinkEndpointDiscreteness(startLinkEndpointDiscreteness).flatMap { discreteNodes ->

                generateTrafficFlowDirectionType(startLinkDirection).flatMap { trafficFlowDirectionType ->

                    snapSingleLinkTwice(discreteNodes, trafficFlowDirectionType, snapPointComparison)
                }
            }
        }

        private fun snapTwoDiscreteLinks(terminusLinkRelation: TerminusLinkRelation)
            : Gen<Pair<SnappedLinkState, SnappedLinkState>> {

            return generateLinkEndpointDiscreteness(startLinkEndpointDiscreteness).flatMap { discreteNodesOnStartLink ->

                generateLinkEndpointDiscreteness(endLinkEndpointDiscreteness).flatMap { discreteNodesOnEndLink ->

                    generateTrafficFlowDirectionType(startLinkDirection).flatMap { directionOnStartLink ->

                        generateTrafficFlowDirectionType(endLinkDirection).flatMap { directionOnEndLink ->

                            when (terminusLinkRelation) {
                                DISCRETE_LINKS_CONNECTED -> snapTwoConnectedLinks(discreteNodesOnStartLink,
                                                                                  discreteNodesOnEndLink,
                                                                                  directionOnStartLink,
                                                                                  directionOnEndLink)

                                DISCRETE_LINKS_UNCONNECTED -> snapTwoUnconnectedLinks(discreteNodesOnStartLink,
                                                                                      discreteNodesOnEndLink,
                                                                                      directionOnStartLink,
                                                                                      directionOnEndLink)

                                else -> throw IllegalStateException("Should not end up here")
                            }
                        }
                    }
                }
            }
        }
    }
}
