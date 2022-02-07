package fi.hsl.jore4.mapmatching.service.node

import fi.hsl.jore4.mapmatching.model.NodeProximity
import fi.hsl.jore4.mapmatching.repository.infrastructure.SnappedLinkState
import fi.hsl.jore4.mapmatching.service.node.NodeResolutionParamsGenerator.TerminusLinkEndpointDiscreteness.DISCRETE_ENDPOINT_NODES
import fi.hsl.jore4.mapmatching.service.node.NodeResolutionParamsGenerator.TerminusLinkEndpointDiscreteness.NON_DISCRETE_ENDPOINT_NODES
import fi.hsl.jore4.mapmatching.service.node.NodeResolutionParamsGenerator.TerminusLinkEndpointDiscreteness.NON_DISCRETE_ENDPOINT_NODES_ON_EITHER_TERMINUS_LINK
import fi.hsl.jore4.mapmatching.service.node.NodeResolutionParamsGenerator.TerminusLinkRelation.DISCRETE_LINKS
import fi.hsl.jore4.mapmatching.service.node.NodeResolutionParamsGenerator.TerminusLinkRelation.DISCRETE_LINKS_CONNECTED
import fi.hsl.jore4.mapmatching.service.node.NodeResolutionParamsGenerator.TerminusLinkRelation.DISCRETE_LINKS_UNCONNECTED
import fi.hsl.jore4.mapmatching.service.node.NodeResolutionParamsGenerator.TerminusLinkRelation.SAME
import fi.hsl.jore4.mapmatching.service.node.NodeResolutionParamsGenerator.ViaNodeGenerationScheme.EMPTY
import fi.hsl.jore4.mapmatching.service.node.NodeResolutionParamsGenerator.ViaNodeGenerationScheme.FULLY_REDUNDANT_WITH_TERMINUS_LINKS
import fi.hsl.jore4.mapmatching.service.node.NodeResolutionParamsGenerator.ViaNodeGenerationScheme.NON_REDUNDANT_WITH_TERMINUS_LINKS
import fi.hsl.jore4.mapmatching.service.node.NodeResolutionParamsGenerator.ViaNodeGenerationScheme.RANDOM
import fi.hsl.jore4.mapmatching.test.generators.NodeProximityGenerator.node
import fi.hsl.jore4.mapmatching.test.generators.Retry
import fi.hsl.jore4.mapmatching.test.generators.SnappedLinkStateGenerator.snapSingleLinkTwice
import fi.hsl.jore4.mapmatching.test.generators.SnappedLinkStateGenerator.snapTwoConnectedLinks
import fi.hsl.jore4.mapmatching.test.generators.SnappedLinkStateGenerator.snapTwoUnconnectedLinks
import org.quicktheories.core.Gen
import org.quicktheories.generators.Generate.constant
import org.quicktheories.generators.SourceDSL.integers
import org.quicktheories.generators.SourceDSL.lists
import fi.hsl.jore4.mapmatching.test.generators.NodeProximityGenerator.node as randomNode

object NodeResolutionParamsGenerator {

    private const val MAX_NUMBER_OF_VIA_NODES: Int = 5

    // Determines how to set IDs and nodes for startLink/endLink in NodeResolutionParams.
    enum class TerminusLinkRelation {

        // startLinkId == endLinkId
        SAME,

        // startLinkId != endLinkId; links may or may not be connected to each other via nodes
        DISCRETE_LINKS,

        // startLinkId != endLinkId; start and end link share a common node i.e. they are connected to each other
        DISCRETE_LINKS_CONNECTED,

        // startLinkId != endLinkId; start and end link do not have a common node i.e. they are unconnected
        DISCRETE_LINKS_UNCONNECTED,

        ANY
    }

    // Determines whether terminus links have discrete endpoint nodes.
    enum class TerminusLinkEndpointDiscreteness {

        // Both links have discrete endpoints.
        DISCRETE_ENDPOINT_NODES,

        // For one (and only one) terminus link on route, a single infrastructure node appears as both start and end
        // node. In case a single link appears at both endpoints of route, this yields non-discrete endpoints on that
        // link.
        NON_DISCRETE_ENDPOINT_NODES_ON_EITHER_TERMINUS_LINK,

        // For both terminus links, a single infrastructure node appears as both a start and end node.
        NON_DISCRETE_ENDPOINT_NODES,

        ANY
    }

    // Determines what kind of via nodes to populate to NodeResolutionParams.
    enum class ViaNodeGenerationScheme {

        // generate empty set of via nodes
        EMPTY,

        RANDOM,

        // Generate via node sequence which does not start or end with a node that is closer node of either snapped
        // terminus link.
        NON_REDUNDANT_WITH_TERMINUS_LINKS,

        // Generate via node sequence which starts with a duplicated sequence of single node that is closer node of
        // snapped start link and ends with a duplicated sequence of single node that is closer node of snapped end
        // link.
        FULLY_REDUNDANT_WITH_TERMINUS_LINKS,

        ANY
    }

    private val EMPTY_VIA_NODE_LIST: Gen<List<NodeProximity>> = constant(emptyList())

    private fun generateViaNodes(nodeSource: Gen<NodeProximity>): Gen<List<NodeProximity>> =
        lists().of(nodeSource).ofSizeBetween(1, MAX_NUMBER_OF_VIA_NODES)

    private fun generateNonRedundantViaNodesWithRegardToTerminusLinks(startLink: SnappedLinkState,
                                                                      endLink: SnappedLinkState)
        : Gen<List<NodeProximity>> {

        return integers().between(1, MAX_NUMBER_OF_VIA_NODES).flatMap { numViaNodes ->

            val firstNodeGen: Gen<NodeProximity> = Retry(randomNode()) { node ->
                node.id != startLink.closerNodeId
                    && (node.id != endLink.closerNodeId || !startLink.isOnSameLinkAs(endLink) && numViaNodes > 1)
            }

            val lastNodeGen: Gen<NodeProximity> = Retry(randomNode()) { node ->
                node.id != endLink.closerNodeId
            }

            when (numViaNodes) {
                1 -> firstNodeGen.map { node -> listOf(node) }
                2 -> firstNodeGen.zip(lastNodeGen) { firstNode, secondNode -> listOf(firstNode, secondNode) }
                else -> {
                    val middleNodesGen: Gen<List<NodeProximity>> = lists().of(randomNode()).ofSize(numViaNodes - 2)

                    firstNodeGen.zip(middleNodesGen,
                                     lastNodeGen) { firstNode, middleNodes, lastNode ->

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

                val firstSequence: Gen<List<NodeProximity>> =
                    lists().of(node(startLink.closerNodeId)).ofSize(sizeOfFirstSequence)

                val secondSequence: Gen<List<NodeProximity>> =
                    lists().of(node(endLink.closerNodeId)).ofSize(sizeOfSecondSequence)

                firstSequence.zip(secondSequence, Collection<NodeProximity>::plus)
            }
        }
    }

    fun builder() = Builder()

    class Builder {

        private var terminusLinkRelation = TerminusLinkRelation.ANY
        private var terminusLinkEndpointDiscreteness = TerminusLinkEndpointDiscreteness.ANY
        private var viaNodeScheme = ViaNodeGenerationScheme.ANY

        fun withTerminusLinkRelation(value: TerminusLinkRelation): Builder {
            terminusLinkRelation = value
            return this
        }

        fun withTerminusLinkEndpointDiscreteness(value: TerminusLinkEndpointDiscreteness): Builder {
            terminusLinkEndpointDiscreteness = value
            return this
        }

        fun withViaNodeGenerationScheme(value: ViaNodeGenerationScheme): Builder {
            viaNodeScheme = value
            return this
        }

        fun build(): Gen<NodeResolutionParams> {
            val terminusLinkGen: Gen<Pair<SnappedLinkState, SnappedLinkState>> = when (terminusLinkRelation) {
                SAME -> when (terminusLinkEndpointDiscreteness) {
                    DISCRETE_ENDPOINT_NODES -> snapSingleLinkTwice(withDiscreteEndpoints = true)
                    TerminusLinkEndpointDiscreteness.ANY -> snapSingleLinkTwice()
                    else -> snapSingleLinkTwice(withDiscreteEndpoints = false)
                }
                DISCRETE_LINKS -> when (terminusLinkEndpointDiscreteness) {
                    DISCRETE_ENDPOINT_NODES -> {
                        snapTwoConnectedLinks(3)
                            .mix(snapTwoUnconnectedLinks(4), 50)
                    }
                    NON_DISCRETE_ENDPOINT_NODES_ON_EITHER_TERMINUS_LINK -> {
                        snapTwoConnectedLinks(2)
                            .mix(snapTwoUnconnectedLinks(3), 50)
                    }
                    NON_DISCRETE_ENDPOINT_NODES -> {
                        snapTwoConnectedLinks(1)
                            .mix(snapTwoUnconnectedLinks(2), 50)
                    }
                    TerminusLinkEndpointDiscreteness.ANY -> {
                        snapTwoConnectedLinks()
                            .mix(snapTwoUnconnectedLinks(), 50)
                    }
                }
                DISCRETE_LINKS_CONNECTED -> when (terminusLinkEndpointDiscreteness) {
                    DISCRETE_ENDPOINT_NODES -> snapTwoConnectedLinks(3)
                    NON_DISCRETE_ENDPOINT_NODES_ON_EITHER_TERMINUS_LINK -> snapTwoConnectedLinks(2)
                    NON_DISCRETE_ENDPOINT_NODES -> snapTwoConnectedLinks(1)
                    TerminusLinkEndpointDiscreteness.ANY -> snapTwoConnectedLinks()
                }
                DISCRETE_LINKS_UNCONNECTED -> when (terminusLinkEndpointDiscreteness) {
                    DISCRETE_ENDPOINT_NODES -> snapTwoUnconnectedLinks(4)
                    NON_DISCRETE_ENDPOINT_NODES_ON_EITHER_TERMINUS_LINK -> snapTwoUnconnectedLinks(3)
                    NON_DISCRETE_ENDPOINT_NODES -> snapTwoUnconnectedLinks(2)
                    TerminusLinkEndpointDiscreteness.ANY -> snapTwoUnconnectedLinks()
                }
                TerminusLinkRelation.ANY -> when (terminusLinkEndpointDiscreteness) {
                    DISCRETE_ENDPOINT_NODES -> {
                        snapSingleLinkTwice(withDiscreteEndpoints = true)
                            .mix(snapTwoConnectedLinks(3), 50)
                            .mix(snapTwoUnconnectedLinks(4), 33)
                    }
                    NON_DISCRETE_ENDPOINT_NODES_ON_EITHER_TERMINUS_LINK -> {
                        snapSingleLinkTwice(withDiscreteEndpoints = false)
                            .mix(snapTwoConnectedLinks(2), 50)
                            .mix(snapTwoUnconnectedLinks(3), 33)
                    }
                    NON_DISCRETE_ENDPOINT_NODES -> {
                        snapSingleLinkTwice(withDiscreteEndpoints = false)
                            .mix(snapTwoConnectedLinks(1), 50)
                            .mix(snapTwoUnconnectedLinks(2), 33)
                    }
                    TerminusLinkEndpointDiscreteness.ANY -> {
                        snapSingleLinkTwice()
                            .mix(snapTwoConnectedLinks(), 50)
                            .mix(snapTwoUnconnectedLinks(), 33)
                    }
                }
            }

            return terminusLinkGen.flatMap { (startLink, endLink) ->

                val viaNodeListGen: Gen<List<NodeProximity>> = when (viaNodeScheme) {
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

                viaNodeListGen.map { viaNodeList -> NodeResolutionParams(startLink, viaNodeList, endLink) }
            }
        }
    }
}
