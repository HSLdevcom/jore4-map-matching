package fi.hsl.jore4.mapmatching.test.generators

import fi.hsl.jore4.mapmatching.model.InfrastructureLinkId
import fi.hsl.jore4.mapmatching.model.NodeProximity
import fi.hsl.jore4.mapmatching.repository.infrastructure.SnappedLinkState
import fi.hsl.jore4.mapmatching.test.generators.CommonGenerators.ZERO_DOUBLE
import fi.hsl.jore4.mapmatching.test.generators.DistanceGenerator.distancePair
import fi.hsl.jore4.mapmatching.test.generators.InfrastructureLinkIdGenerator.infrastructureLinkId
import fi.hsl.jore4.mapmatching.test.generators.InfrastructureLinkIdGenerator.infrastructureLinkIdPair
import fi.hsl.jore4.mapmatching.test.generators.LinkEndpointsProximityFilter.END_NODE_CLOSER
import fi.hsl.jore4.mapmatching.test.generators.LinkEndpointsProximityFilter.END_NODE_CLOSER_OR_EQUAL_DISTANCE
import fi.hsl.jore4.mapmatching.test.generators.LinkEndpointsProximityFilter.NODES_AT_EQUAL_DISTANCE
import fi.hsl.jore4.mapmatching.test.generators.LinkEndpointsProximityFilter.START_NODE_CLOSER
import fi.hsl.jore4.mapmatching.test.generators.LinkEndpointsProximityFilter.START_NODE_CLOSER_OR_EQUAL_DISTANCE
import fi.hsl.jore4.mapmatching.test.generators.NodeProximityGenerator.distinctNodePair
import fi.hsl.jore4.mapmatching.test.generators.NodeProximityGenerator.distinctNodeQuadruple
import fi.hsl.jore4.mapmatching.test.generators.NodeProximityGenerator.distinctNodeTriple
import fi.hsl.jore4.mapmatching.test.generators.NodeProximityGenerator.endpointNodesOfInfrastructureLink
import org.quicktheories.core.Gen
import org.quicktheories.generators.Generate.booleans
import org.quicktheories.generators.SourceDSL.doubles
import kotlin.math.min

object SnappedLinkStateGenerator {

    // random distances from arbitrary point to the closest link
    private val POSITIVE_DISTANCE: Gen<Double> = doubles().between(0.5, 50.0)

    // mix 10% zeros
    private val NON_NEGATIVE_DISTANCE: Gen<Double> = POSITIVE_DISTANCE.mix(ZERO_DOUBLE, 10)

    private val DISTANCE_PAIR: Gen<Pair<Double, Double>> = distancePair(NON_NEGATIVE_DISTANCE, POSITIVE_DISTANCE)

    private val SNAPPED_LINK_HAVING_START_NODE_CLOSER: Gen<SnappedLinkState> =
        generateSnappedLinkState(START_NODE_CLOSER)
    private val SNAPPED_LINK_HAVING_END_NODE_CLOSER: Gen<SnappedLinkState> = generateSnappedLinkState(END_NODE_CLOSER)
    private val SNAPPED_LINK_HAVING_BOTH_NODES_AT_EQUAL_DISTANCE: Gen<SnappedLinkState> =
        generateSnappedLinkState(NODES_AT_EQUAL_DISTANCE)

    private val MIXED_SNAPPED_LINK: Gen<SnappedLinkState> = SNAPPED_LINK_HAVING_START_NODE_CLOSER
        .mix(SNAPPED_LINK_HAVING_END_NODE_CLOSER, 50)
        .mix(SNAPPED_LINK_HAVING_BOTH_NODES_AT_EQUAL_DISTANCE, 5)

    private val SNAP_SINGLE_LINK_TWICE: Gen<Pair<SnappedLinkState, SnappedLinkState>> =
        infrastructureLinkId().zip(DISTANCE_PAIR, distinctNodePair(), booleans()) { linkId,
                                                                                    (distance1, distance2),
                                                                                    (node1, node2),
                                                                                    flipOrder ->
            val nodesOfFirstLink = Pair(node1, node2)

            val nodesOfSecondLink: Pair<NodeProximity, NodeProximity> = when (flipOrder) {
                true -> nodesOfFirstLink
                else -> Pair(node2, node1)
            }

            Pair(createSnappedLinkState(linkId, distance1, nodesOfFirstLink),
                 createSnappedLinkState(linkId, distance2, nodesOfSecondLink))
        }

    // Generate pairs of links having common node.
    private val SNAP_TWO_CONNECTED_LINKS: Gen<Pair<SnappedLinkState, SnappedLinkState>> =
        infrastructureLinkIdPair().zip(DISTANCE_PAIR, distinctNodeTriple()) { (firstLinkId, secondLinkId),
                                                                              (distanceToFirstLink, distanceToSecondLink),
                                                                              (node1, node2, node3) ->

            Pair(createSnappedLinkState(firstLinkId, distanceToFirstLink, node1, node2),
                 createSnappedLinkState(secondLinkId, distanceToSecondLink, node2, node3))
        }

    // Generate pairs of links that do not have a common node.
    private val SNAP_TWO_UNCONNECTED_LINKS: Gen<Pair<SnappedLinkState, SnappedLinkState>> =
        infrastructureLinkIdPair()
            .zip(DISTANCE_PAIR, distinctNodeQuadruple()) { (firstLinkId, secondLinkId),
                                                           (distanceToFirstLink, distanceToSecondLink),
                                                           (node1, node2, node3, node4) ->

                Pair(createSnappedLinkState(firstLinkId, distanceToFirstLink, node1, node2),
                     createSnappedLinkState(secondLinkId, distanceToSecondLink, node3, node4))
            }

    fun snapLink(): Gen<SnappedLinkState> = MIXED_SNAPPED_LINK

    fun snapLink(nodeProximityFilter: LinkEndpointsProximityFilter): Gen<SnappedLinkState> {
        return when (nodeProximityFilter) {
            START_NODE_CLOSER -> SNAPPED_LINK_HAVING_START_NODE_CLOSER
            END_NODE_CLOSER -> SNAPPED_LINK_HAVING_END_NODE_CLOSER
            NODES_AT_EQUAL_DISTANCE -> SNAPPED_LINK_HAVING_BOTH_NODES_AT_EQUAL_DISTANCE
            START_NODE_CLOSER_OR_EQUAL_DISTANCE -> {
                SNAPPED_LINK_HAVING_START_NODE_CLOSER.mix(SNAPPED_LINK_HAVING_BOTH_NODES_AT_EQUAL_DISTANCE, 50)
            }
            END_NODE_CLOSER_OR_EQUAL_DISTANCE -> {
                SNAPPED_LINK_HAVING_END_NODE_CLOSER.mix(SNAPPED_LINK_HAVING_BOTH_NODES_AT_EQUAL_DISTANCE, 50)
            }
        }
    }

    fun snapSingleLinkTwice(): Gen<Pair<SnappedLinkState, SnappedLinkState>> = SNAP_SINGLE_LINK_TWICE

    // Connected links have one common node.
    fun snapTwoConnectedLinks(): Gen<Pair<SnappedLinkState, SnappedLinkState>> = SNAP_TWO_CONNECTED_LINKS

    fun snapTwoUnconnectedLinks(): Gen<Pair<SnappedLinkState, SnappedLinkState>> = SNAP_TWO_UNCONNECTED_LINKS

    private fun generateSnappedLinkState(nodeProximityFilter: LinkEndpointsProximityFilter): Gen<SnappedLinkState> {
        return infrastructureLinkId()
            .zip(
                NON_NEGATIVE_DISTANCE, endpointNodesOfInfrastructureLink(nodeProximityFilter),
            ) { infrastructureLinkId, distanceToLink, (startNode, endNode) ->

                createSnappedLinkState(infrastructureLinkId, distanceToLink, startNode, endNode)
            }
    }

    private fun createSnappedLinkState(infrastructureLinkId: InfrastructureLinkId,
                                       generatedDistanceValue: Double,
                                       startNode: NodeProximity,
                                       endNode: NodeProximity): SnappedLinkState {

        val closestDistanceToLink: Double =
            min(generatedDistanceValue, min(startNode.distanceToNode, endNode.distanceToNode))

        return SnappedLinkState(infrastructureLinkId, closestDistanceToLink, startNode, endNode)
    }

    private fun createSnappedLinkState(infrastructureLinkId: InfrastructureLinkId,
                                       generatedDistanceValue: Double,
                                       nodes: Pair<NodeProximity, NodeProximity>): SnappedLinkState {

        return createSnappedLinkState(infrastructureLinkId, generatedDistanceValue, nodes.first, nodes.second)
    }
}