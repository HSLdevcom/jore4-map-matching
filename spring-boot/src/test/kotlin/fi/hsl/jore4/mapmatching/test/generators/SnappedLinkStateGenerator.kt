package fi.hsl.jore4.mapmatching.test.generators

import fi.hsl.jore4.mapmatching.model.InfrastructureLinkId
import fi.hsl.jore4.mapmatching.model.InfrastructureNodeId
import fi.hsl.jore4.mapmatching.repository.infrastructure.SnappedLinkState
import fi.hsl.jore4.mapmatching.test.generators.CommonGenerators.ZERO_DOUBLE
import fi.hsl.jore4.mapmatching.test.generators.CommonGenerators.duplicate
import fi.hsl.jore4.mapmatching.test.generators.CommonGenerators.pair
import fi.hsl.jore4.mapmatching.test.generators.CommonGenerators.shuffledPair
import fi.hsl.jore4.mapmatching.test.generators.InfrastructureLinkIdGenerator.infrastructureLinkId
import fi.hsl.jore4.mapmatching.test.generators.InfrastructureLinkIdGenerator.infrastructureLinkIdPair
import fi.hsl.jore4.mapmatching.test.generators.InfrastructureNodeIdGenerator.discreteNodeIdPair
import fi.hsl.jore4.mapmatching.test.generators.InfrastructureNodeIdGenerator.nodeIdPair
import fi.hsl.jore4.mapmatching.test.generators.InfrastructureNodeIdGenerator.nodeIdQuadruple
import fi.hsl.jore4.mapmatching.test.generators.LinkEndpointsProximityFilter.END_NODE_CLOSER
import fi.hsl.jore4.mapmatching.test.generators.LinkEndpointsProximityFilter.END_NODE_CLOSER_OR_EQUAL_DISTANCE
import fi.hsl.jore4.mapmatching.test.generators.LinkEndpointsProximityFilter.NODES_AT_EQUAL_DISTANCE
import fi.hsl.jore4.mapmatching.test.generators.LinkEndpointsProximityFilter.START_NODE_CLOSER
import fi.hsl.jore4.mapmatching.test.generators.LinkEndpointsProximityFilter.START_NODE_CLOSER_OR_EQUAL_DISTANCE
import fi.hsl.jore4.mapmatching.test.util.Quadruple
import fi.hsl.jore4.mapmatching.util.MathUtils.DOUBLE_TOLERANCE
import fi.hsl.jore4.mapmatching.util.MathUtils.isWithinTolerance
import org.quicktheories.core.Gen
import org.quicktheories.generators.Generate.booleans
import org.quicktheories.generators.Generate.constant
import org.quicktheories.generators.Generate.enumValues
import org.quicktheories.generators.SourceDSL.doubles
import org.quicktheories.generators.SourceDSL.integers

object SnappedLinkStateGenerator {

    // random distances from arbitrary point to the closest link
    private val POSITIVE_DISTANCE: Gen<Double> = doubles().between(0.5, 50.0)

    // mix 10% zeros
    private val NON_NEGATIVE_DISTANCE: Gen<Double> = POSITIVE_DISTANCE.mix(ZERO_DOUBLE, 10)

    private val DISTANCE_PAIR: Gen<Pair<Double, Double>> = shuffledPair(NON_NEGATIVE_DISTANCE, POSITIVE_DISTANCE)

    private val SNAP_FRACTIONAL: Gen<Double> = doubles().between(0.0, 1.0)

    private val LINK_LENGTH: Gen<Double> = doubles().between(2.0, 5_000.0)

    fun snapLink(): Gen<SnappedLinkState> = booleans().flatMap(this::snapLink)

    fun snapLink(withDiscreteEndpoints: Boolean): Gen<SnappedLinkState> {
        return enumValues(LinkEndpointsProximityFilter::class.java)
            .flatMap { proximityFilter ->
                snapLink(proximityFilter, withDiscreteEndpoints)
            }
    }

    fun snapLink(nodeProximityFilter: LinkEndpointsProximityFilter, withDiscreteEndpoints: Boolean = true)
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
        val numberOfDiscreteNodesGen: Gen<Int> = when (withDiscreteEndpoints) {
            true -> constant(2)
            false -> constant(1)
            else -> integers().between(1, 2)
        }

        return snapTwoLinks(duplicate(infrastructureLinkId()),
                            numberOfDiscreteNodesGen.flatMap { nodeIdQuadruple(it, discreteNodesBetweenHalves = false) })
    }

    // Generate pairs of links having a common node.
    fun snapTwoConnectedLinks(): Gen<Pair<SnappedLinkState, SnappedLinkState>> = integers()
        .between(1, 3)
        .flatMap(this::snapTwoConnectedLinks)

    // Generate pairs of links having a common node.
    fun snapTwoConnectedLinks(numberOfDiscreteNodes: Int): Gen<Pair<SnappedLinkState, SnappedLinkState>> {
        require(numberOfDiscreteNodes in 1..3) {
            "numberOfDiscreteNodes must be in range 1..3, but was: $numberOfDiscreteNodes"
        }

        return snapTwoLinks(infrastructureLinkIdPair(),
                            nodeIdQuadruple(numberOfDiscreteNodes, discreteNodesBetweenHalves = false))
    }

    // Generate pairs of links that do not have a common node.
    fun snapTwoUnconnectedLinks(): Gen<Pair<SnappedLinkState, SnappedLinkState>> = integers()
        .between(2, 4)
        .flatMap(this::snapTwoUnconnectedLinks)

    // Generate pairs of links that do not have a common node.
    fun snapTwoUnconnectedLinks(numberOfDiscreteNodes: Int): Gen<Pair<SnappedLinkState, SnappedLinkState>> {
        require(numberOfDiscreteNodes in 1..4) {
            "numberOfDiscreteNodes must be in range 1..4, but was: $numberOfDiscreteNodes"
        }
        return snapTwoLinks(infrastructureLinkIdPair(),
                            nodeIdQuadruple(numberOfDiscreteNodes, discreteNodesBetweenHalves = true))
    }

    fun snapTwoLinks(linkIdsGen: Gen<Pair<InfrastructureLinkId, InfrastructureLinkId>>,
                     nodeIdsGen: Gen<Quadruple<InfrastructureNodeId, InfrastructureNodeId, InfrastructureNodeId, InfrastructureNodeId>>)
        : Gen<Pair<SnappedLinkState, SnappedLinkState>> {

        return linkIdsGen.zip(DISTANCE_PAIR,
                              pair(SNAP_FRACTIONAL),
                              pair(LINK_LENGTH),
                              nodeIdsGen) { (linkId1, linkId2),
                                            (distanceToLink1, distanceToLink2),
                                            (snapFractional1, snapFractional2),
                                            (linkLength1, linkLength2),
                                            (nodeId1, nodeId2, nodeId3, nodeId4) ->

            Pair(createSnappedLinkState(linkId1, distanceToLink1, snapFractional1, linkLength1, nodeId1, nodeId2),
                 createSnappedLinkState(linkId2, distanceToLink2, snapFractional2, linkLength2, nodeId3, nodeId4))
        }
    }

    private fun generateSnappedLinkStateFromOneNode(nodeProximityFilter: LinkEndpointsProximityFilter): Gen<SnappedLinkState> =
        generateSnappedLinkState(generateSnapFractional(nodeProximityFilter), nodeIdPair(false))

    private fun generateSnappedLinkStateFromTwoNodes(nodeProximityFilter: LinkEndpointsProximityFilter): Gen<SnappedLinkState> =
        generateSnappedLinkState(generateSnapFractional(nodeProximityFilter), discreteNodeIdPair())

    private fun generateSnappedLinkState(snapFractionalGen: Gen<Double>,
                                         nodeIdPairGen: Gen<Pair<InfrastructureNodeId, InfrastructureNodeId>>)
        : Gen<SnappedLinkState> {

        return infrastructureLinkId().zip(NON_NEGATIVE_DISTANCE,
                                          snapFractionalGen,
                                          LINK_LENGTH,
                                          nodeIdPairGen) { infrastructureLinkId,
                                                           distanceToLink,
                                                           snapFractional,
                                                           linkLength,
                                                           (startNodeId, endNodeId) ->

            createSnappedLinkState(infrastructureLinkId,
                                   distanceToLink,
                                   snapFractional,
                                   linkLength,
                                   startNodeId,
                                   endNodeId)
        }
    }

    private fun generateSnapFractional(nodeProximityFilter: LinkEndpointsProximityFilter): Gen<Double> =
        when (nodeProximityFilter) {
            START_NODE_CLOSER -> doubles().between(0.0, 0.5 - DOUBLE_TOLERANCE)
            END_NODE_CLOSER -> doubles().between(0.5 + DOUBLE_TOLERANCE, 1.0)
            NODES_AT_EQUAL_DISTANCE -> constant(0.5)
            START_NODE_CLOSER_OR_EQUAL_DISTANCE -> doubles().between(0.0, 0.5)
            END_NODE_CLOSER_OR_EQUAL_DISTANCE -> doubles().between(0.5, 1.0)
        }

    private fun createSnappedLinkState(infrastructureLinkId: InfrastructureLinkId,
                                       distanceToLink: Double,
                                       snapPointFractional: Double,
                                       linkLength: Double,
                                       startNodeId: InfrastructureNodeId,
                                       endNodeId: InfrastructureNodeId)
        : SnappedLinkState {

        val snappedToEndpointNode: Boolean =
            snapPointFractional.isWithinTolerance(0.0) || snapPointFractional.isWithinTolerance(1.0)

        val closestDistanceToLink: Double = if (snappedToEndpointNode) 0.0 else distanceToLink

        return SnappedLinkState(infrastructureLinkId,
                                closestDistanceToLink,
                                snapPointFractional,
                                linkLength,
                                startNodeId,
                                endNodeId)
    }
}
