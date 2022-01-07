package fi.hsl.jore4.mapmatching.service.node

import fi.hsl.jore4.mapmatching.model.NodeProximity
import fi.hsl.jore4.mapmatching.repository.infrastructure.SnappedLinkState
import fi.hsl.jore4.mapmatching.service.node.NodeResolutionParamsGenerator.TerminusLinkRelation.CONNECTED
import fi.hsl.jore4.mapmatching.service.node.NodeResolutionParamsGenerator.TerminusLinkRelation.DISTINCT
import fi.hsl.jore4.mapmatching.service.node.NodeResolutionParamsGenerator.TerminusLinkRelation.SAME
import fi.hsl.jore4.mapmatching.service.node.NodeResolutionParamsGenerator.TerminusLinkRelation.UNCONNECTED
import fi.hsl.jore4.mapmatching.service.node.NodeResolutionParamsGenerator.ViaNodeGenerationScheme.EMPTY
import fi.hsl.jore4.mapmatching.service.node.NodeResolutionParamsGenerator.ViaNodeGenerationScheme.FULLY_REDUNDANT_WITH_TERMINUS_LINKS
import fi.hsl.jore4.mapmatching.service.node.NodeResolutionParamsGenerator.ViaNodeGenerationScheme.NON_REDUNDANT_WITH_TERMINUS_LINKS
import fi.hsl.jore4.mapmatching.service.node.NodeResolutionParamsGenerator.ViaNodeGenerationScheme.RANDOM
import fi.hsl.jore4.mapmatching.test.generators.Retry
import fi.hsl.jore4.mapmatching.test.generators.SnappedLinkStateGenerator.snapSingleLinkTwice
import fi.hsl.jore4.mapmatching.test.generators.SnappedLinkStateGenerator.snapTwoConnectedLinks
import fi.hsl.jore4.mapmatching.test.generators.SnappedLinkStateGenerator.snapTwoUnconnectedLinks
import org.quicktheories.core.Gen
import org.quicktheories.generators.Generate.constant
import org.quicktheories.generators.Generate.oneOf
import org.quicktheories.generators.SourceDSL.arbitrary
import org.quicktheories.generators.SourceDSL.integers
import org.quicktheories.generators.SourceDSL.lists
import fi.hsl.jore4.mapmatching.test.generators.NodeProximityGenerator.node as randomNode

object NodeResolutionParamsGenerator {

    private const val MAX_NUMBER_OF_VIA_NODES: Int = 5

    // Determines how to populate startLinkId and endLinkId to NodeResolutionParams.
    enum class TerminusLinkRelation {

        // startLinkId == endLinkId
        SAME,

        // startLinkId != endLinkId; links may be connected to each other or not
        DISTINCT,

        // start and end link share a common node i.e. they are connected to each other
        CONNECTED,

        // start and end link do not have a common node i.e. they are unconnected
        UNCONNECTED,

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
    private val RANDOM_VIA_NODES: Gen<List<NodeProximity>> = generateViaNodes(randomNode())

    private fun generateViaNodes(nodeSource: Gen<NodeProximity>): Gen<List<NodeProximity>> =
        lists().of(nodeSource).ofSizeBetween(1, MAX_NUMBER_OF_VIA_NODES)

    private fun pickExcluding(list: List<NodeProximity>, excludedIds: Set<Long>): Gen<NodeProximity> {
        val filtered: List<NodeProximity> = list.filter { !excludedIds.contains(it.id) }

        return arbitrary().pick(filtered)
    }

    private fun generateNonRedundantViaNodesWithRegardToTerminusLinks(startLink: SnappedLinkState,
                                                                      endLink: SnappedLinkState)
        : Gen<List<NodeProximity>> {

        val randomNodeGen = Retry(randomNode()) { node ->
            node.id != startLink.closerNodeId && node.id != endLink.closerNodeId
        }

        return integers().between(1, MAX_NUMBER_OF_VIA_NODES).flatMap { numViaNodes ->

            val allNodesOfTerminusLinks: List<NodeProximity> =
                listOf(startLink.closerNode, startLink.furtherNode, endLink.closerNode, endLink.furtherNode)

            val idsExcludedFromFirstNode: Set<Long> =
                if (numViaNodes == 1)
                    setOf(startLink.closerNodeId, endLink.closerNodeId)
                else
                    setOf(startLink.closerNodeId)

            val firstNodeGen: Gen<NodeProximity> = oneOf(randomNodeGen,
                                                         pickExcluding(allNodesOfTerminusLinks,
                                                                       idsExcludedFromFirstNode))

            val lastNodeGen: Gen<NodeProximity> = oneOf(randomNodeGen,
                                                        pickExcluding(allNodesOfTerminusLinks,
                                                                      setOf(endLink.closerNodeId)))

            when (numViaNodes) {
                1 -> firstNodeGen.map { node -> listOf(node) }
                2 -> firstNodeGen.zip(lastNodeGen) { firstNode, secondNode -> listOf(firstNode, secondNode) }
                else -> {
                    val middleViaNodeGen: Gen<NodeProximity> =
                        oneOf(randomNodeGen, arbitrary().pick(allNodesOfTerminusLinks))

                    val middleViaNodeListGen: Gen<List<NodeProximity>> =
                        lists().of(middleViaNodeGen).ofSize(numViaNodes - 2)

                    firstNodeGen
                        .zip(middleViaNodeListGen, lastNodeGen) { firstNode, middleNodes, lastNode ->
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
                    lists().of(constant(startLink.closerNode)).ofSize(sizeOfFirstSequence)

                val secondSequence: Gen<List<NodeProximity>> =
                    lists().of(constant(endLink.closerNode)).ofSize(sizeOfSecondSequence)

                firstSequence.zip(secondSequence) { first, second -> first + second }
            }
        }
    }

    fun builder() = Builder()

    class Builder {

        private var terminusLinkRelation = TerminusLinkRelation.ANY
        private var viaNodeScheme = ViaNodeGenerationScheme.ANY

        fun withStartLinkRelatedToEndLink(value: TerminusLinkRelation): Builder {
            terminusLinkRelation = value
            return this
        }

        fun withViaNodeGenerationScheme(value: ViaNodeGenerationScheme): Builder {
            viaNodeScheme = value
            return this
        }

        fun build(): Gen<NodeResolutionParams> {
            val terminusLinkGen: Gen<Pair<SnappedLinkState, SnappedLinkState>> = when (terminusLinkRelation) {
                SAME -> snapSingleLinkTwice()
                DISTINCT -> oneOf(snapTwoConnectedLinks(), snapTwoUnconnectedLinks())
                CONNECTED -> snapTwoConnectedLinks()
                UNCONNECTED -> snapTwoUnconnectedLinks()
                TerminusLinkRelation.ANY -> oneOf(snapSingleLinkTwice(),
                                                  snapTwoConnectedLinks(),
                                                  snapTwoUnconnectedLinks())
            }

            return terminusLinkGen.flatMap { (startLink, endLink) ->

                val viaNodeListGen: Gen<List<NodeProximity>> = when (viaNodeScheme) {
                    EMPTY -> EMPTY_VIA_NODE_LIST
                    RANDOM -> RANDOM_VIA_NODES
                    NON_REDUNDANT_WITH_TERMINUS_LINKS -> {
                        generateNonRedundantViaNodesWithRegardToTerminusLinks(startLink, endLink)
                    }
                    FULLY_REDUNDANT_WITH_TERMINUS_LINKS -> {
                        generateFullyRedundantViaNodesWithRegardToTerminusLinks(startLink, endLink)
                    }
                    ViaNodeGenerationScheme.ANY -> oneOf(EMPTY_VIA_NODE_LIST, RANDOM_VIA_NODES)
                }

                viaNodeListGen.map { viaNodeList -> NodeResolutionParams(startLink, viaNodeList, endLink) }
            }
        }
    }
}
