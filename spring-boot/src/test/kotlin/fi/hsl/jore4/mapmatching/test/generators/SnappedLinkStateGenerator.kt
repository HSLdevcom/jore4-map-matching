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
import fi.hsl.jore4.mapmatching.test.generators.NodeProximityGenerator.discreteEndpointNodesForInfrastructureLink
import fi.hsl.jore4.mapmatching.test.generators.NodeProximityGenerator.discreteNodePair
import fi.hsl.jore4.mapmatching.test.generators.NodeProximityGenerator.nodeQuadruple
import fi.hsl.jore4.mapmatching.test.generators.NodeProximityGenerator.nodeTriple
import fi.hsl.jore4.mapmatching.test.generators.NodeProximityGenerator.oneNodeAsBothEndpointsForInfrastructureLink
import fi.hsl.jore4.mapmatching.test.generators.NodeProximityGenerator.singleNodeAtTwoDistances
import org.quicktheories.core.Gen
import org.quicktheories.generators.Generate.booleans
import org.quicktheories.generators.Generate.enumValues
import org.quicktheories.generators.SourceDSL.doubles
import org.quicktheories.generators.SourceDSL.integers
import kotlin.math.min

object SnappedLinkStateGenerator {

    // random distances from arbitrary point to the closest link
    private val POSITIVE_DISTANCE: Gen<Double> = doubles().between(0.5, 50.0)

    // mix 10% zeros
    private val NON_NEGATIVE_DISTANCE: Gen<Double> = POSITIVE_DISTANCE.mix(ZERO_DOUBLE, 10)

    private val DISTANCE_PAIR: Gen<Pair<Double, Double>> = distancePair(NON_NEGATIVE_DISTANCE, POSITIVE_DISTANCE)

    fun snapLink(): Gen<SnappedLinkState> = booleans().flatMap(this::snapLink)

    fun snapLink(withDiscreteEndpoints: Boolean): Gen<SnappedLinkState> {
        return enumValues(LinkEndpointsProximityFilter::class.java)
            .flatMap { proximityFilter ->
                snapLink(proximityFilter, withDiscreteEndpoints)
            }
    }

    fun snapLink(nodeProximityFilter: LinkEndpointsProximityFilter,
                 withDiscreteEndpoints: Boolean = true)
        : Gen<SnappedLinkState> {

        val getSnappedLinkState: (LinkEndpointsProximityFilter) -> Gen<SnappedLinkState> =
            if (withDiscreteEndpoints)
                this::generateSnappedLinkStateFromTwoNodes
            else
                this::generateSnappedLinkStateFromOneNode

        return when (nodeProximityFilter) {
            START_NODE_CLOSER -> getSnappedLinkState(START_NODE_CLOSER)
            END_NODE_CLOSER -> getSnappedLinkState(END_NODE_CLOSER)
            NODES_AT_EQUAL_DISTANCE -> getSnappedLinkState(NODES_AT_EQUAL_DISTANCE)
            START_NODE_CLOSER_OR_EQUAL_DISTANCE -> {
                getSnappedLinkState(START_NODE_CLOSER)
                    .mix(getSnappedLinkState(NODES_AT_EQUAL_DISTANCE), 50)
            }
            END_NODE_CLOSER_OR_EQUAL_DISTANCE -> {
                getSnappedLinkState(END_NODE_CLOSER)
                    .mix(getSnappedLinkState(NODES_AT_EQUAL_DISTANCE), 50)
            }
        }
    }

    fun snapSingleLinkTwice(withDiscreteEndpoints: Boolean? = null): Gen<Pair<SnappedLinkState, SnappedLinkState>> {
        val nodePair: Gen<Pair<NodeProximity, NodeProximity>> = when (withDiscreteEndpoints) {
            true -> discreteNodePair()
            false -> singleNodeAtTwoDistances()
            null -> discreteNodePair().mix(singleNodeAtTwoDistances(), 50)
        }

        return infrastructureLinkId().zip(DISTANCE_PAIR,
                                          nodePair,
                                          booleans()) { linkId,
                                                        (distanceToLink1, distanceToLink2),
                                                        (node1, node2),
                                                        flipOrder ->
            val nodesOfFirstLink = Pair(node1, node2)

            val nodesOfSecondLink: Pair<NodeProximity, NodeProximity> = when (flipOrder) {
                false -> nodesOfFirstLink
                else -> Pair(node2, node1)
            }

            Pair(createSnappedLinkState(linkId, distanceToLink1, nodesOfFirstLink),
                 createSnappedLinkState(linkId, distanceToLink2, nodesOfSecondLink))
        }
    }

    // Generate pairs of links having a common node.
    fun snapTwoConnectedLinks(): Gen<Pair<SnappedLinkState, SnappedLinkState>> {
        return integers().between(1, 3).flatMap(this::snapTwoConnectedLinks)
    }

    // Generate pairs of links having a common node.
    fun snapTwoConnectedLinks(numberOfDiscreteNodes: Int): Gen<Pair<SnappedLinkState, SnappedLinkState>> {
        if (numberOfDiscreteNodes !in 1..3) {
            throw IllegalArgumentException("numberOfDiscreteNodes should be in range 1..3, but was: $numberOfDiscreteNodes")
        }

        return infrastructureLinkIdPair()
            .zip(DISTANCE_PAIR,
                 nodeTriple(numberOfDiscreteNodes)) { (firstLinkId, secondLinkId),
                                                      (distanceToFirstLink, distanceToSecondLink),
                                                      (node1, node2, node3) ->

                Pair(createSnappedLinkState(firstLinkId, distanceToFirstLink, node1, node2),
                     createSnappedLinkState(secondLinkId, distanceToSecondLink, node2, node3))
            }
    }

    // Generate pairs of links that do not have a common node.
    fun snapTwoUnconnectedLinks(): Gen<Pair<SnappedLinkState, SnappedLinkState>> {
        return integers().between(2, 4).flatMap(this::snapTwoUnconnectedLinks)
    }

    // Generate pairs of links that do not have a common node.
    fun snapTwoUnconnectedLinks(numberOfDiscreteNodes: Int): Gen<Pair<SnappedLinkState, SnappedLinkState>> {
        if (numberOfDiscreteNodes !in 2..4) {
            throw IllegalArgumentException("numberOfDiscreteNodes should be in range 2..4, but was: $numberOfDiscreteNodes")
        }

        return infrastructureLinkIdPair()
            .zip(DISTANCE_PAIR,
                 nodeQuadruple(numberOfDiscreteNodes)) { (firstLinkId, secondLinkId),
                                                         (distanceToFirstLink, distanceToSecondLink),
                                                         (node1, node2, node3, node4) ->

                Pair(createSnappedLinkState(firstLinkId, distanceToFirstLink, node1, node2),
                     createSnappedLinkState(secondLinkId, distanceToSecondLink, node3, node4))
            }
    }

    private fun generateSnappedLinkStateFromOneNode(nodeProximityFilter: LinkEndpointsProximityFilter): Gen<SnappedLinkState> {
        return infrastructureLinkId()
            .zip(NON_NEGATIVE_DISTANCE,
                 oneNodeAsBothEndpointsForInfrastructureLink(nodeProximityFilter)) { infrastructureLinkId,
                                                                                     distanceToLink,
                                                                                     (startNode, endNode) ->

                createSnappedLinkState(infrastructureLinkId, distanceToLink, startNode, endNode)
            }
    }

    private fun generateSnappedLinkStateFromTwoNodes(nodeProximityFilter: LinkEndpointsProximityFilter): Gen<SnappedLinkState> {
        return infrastructureLinkId()
            .zip(NON_NEGATIVE_DISTANCE,
                 discreteEndpointNodesForInfrastructureLink(nodeProximityFilter)) { infrastructureLinkId,
                                                                                    distanceToLink,
                                                                                    (startNode, endNode) ->

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
