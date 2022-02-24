package fi.hsl.jore4.mapmatching.test.generators

import fi.hsl.jore4.mapmatching.model.InfrastructureLinkId
import fi.hsl.jore4.mapmatching.model.InfrastructureNodeId
import fi.hsl.jore4.mapmatching.model.TrafficFlowDirectionType
import fi.hsl.jore4.mapmatching.repository.infrastructure.SnappedLinkState
import fi.hsl.jore4.mapmatching.test.generators.CommonGenerators.ZERO_DOUBLE
import fi.hsl.jore4.mapmatching.test.generators.CommonGenerators.duplicate
import fi.hsl.jore4.mapmatching.test.generators.CommonGenerators.pair
import fi.hsl.jore4.mapmatching.test.generators.CommonGenerators.shuffledPair
import fi.hsl.jore4.mapmatching.test.generators.CommonGenerators.trafficFlowDirectionType
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

    private val SNAP_POINT_LOCATION_FRACTION: Gen<Double> = doubles().between(0.0, 1.0)

    private val LINK_LENGTH: Gen<Double> = doubles().between(2.0, 5_000.0)

    fun snapLink(): Gen<SnappedLinkState> = booleans().flatMap(this::snapLink)

    fun snapLink(withDiscreteEndpoints: Boolean): Gen<SnappedLinkState> {
        return trafficFlowDirectionType().flatMap { trafficFlowDirectionType ->
            enumValues(LinkEndpointsProximityFilter::class.java).flatMap { proximityFilter ->

                snapLink(withDiscreteEndpoints, trafficFlowDirectionType, proximityFilter)
            }
        }
    }

    fun snapLink(withDiscreteEndpoints: Boolean,
                 trafficFlowDirectionType: TrafficFlowDirectionType,
                 nodeProximityFilter: LinkEndpointsProximityFilter? = null)
        : Gen<SnappedLinkState> {

        val getSnappedLinkState: (LinkEndpointsProximityFilter?) -> Gen<SnappedLinkState> = { proximityFilter ->
            if (withDiscreteEndpoints)
                generateSnappedLinkStateFromTwoNodes(trafficFlowDirectionType, proximityFilter)
            else
                generateSnappedLinkStateFromOneNode(trafficFlowDirectionType, proximityFilter)
        }

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
            else -> getSnappedLinkState(null)
        }
    }

    fun snapSingleLinkTwice(withDiscreteEndpoints: Boolean?,
                            trafficFlowDirectionType: TrafficFlowDirectionType,
                            snapPointComparison: Int)
        : Gen<Pair<SnappedLinkState, SnappedLinkState>> {

        require(snapPointComparison in -1..1) { "snapPointComparison must be in range -1..1" }

        val genSnapPointLocationFraction: Gen<Pair<Double, Double>> = when (snapPointComparison) {
            -1 -> {
                doubles().between(0.0, 1.0 - DOUBLE_TOLERANCE)
                    .flatMap { firstFrac ->
                        doubles().between(firstFrac + DOUBLE_TOLERANCE, 1.0)
                            .map { secondFrac -> firstFrac to secondFrac }
                    }
            }
            0 -> duplicate(SNAP_POINT_LOCATION_FRACTION)
            1 -> {
                doubles().between(DOUBLE_TOLERANCE, 1.0)
                    .flatMap { firstFrac ->
                        doubles().between(0.0, firstFrac - DOUBLE_TOLERANCE)
                            .map { secondFrac -> firstFrac to secondFrac }
                    }
            }
            // null case
            else -> pair(SNAP_POINT_LOCATION_FRACTION)
        }

        val genNumberOfDiscreteNodes: Gen<Int> = when (withDiscreteEndpoints) {
            true -> constant(2)
            false -> constant(1)
            else -> integers().between(1, 2)
        }

        return genNumberOfDiscreteNodes.flatMap { numDiscreteNodes ->

            snapTwoLinks(trafficFlowDirectionType,
                         trafficFlowDirectionType,
                         duplicate(infrastructureLinkId()),
                         genSnapPointLocationFraction,
                         nodeIdQuadruple(numDiscreteNodes, discreteNodesBetweenHalves = false))
        }
    }

    // Generate pairs of links having common node.
    fun snapTwoConnectedLinks(): Gen<Pair<SnappedLinkState, SnappedLinkState>> {
        return trafficFlowDirectionType().flatMap { firstLinkDirection ->
            trafficFlowDirectionType().flatMap { secondLinkDirection ->

                snapTwoConnectedLinks(firstLinkDirection, secondLinkDirection)
            }
        }
    }

    // Generate pairs of links having common node.
    fun snapTwoConnectedLinks(firstLinkDirection: TrafficFlowDirectionType,
                              secondLinkDirection: TrafficFlowDirectionType)
        : Gen<Pair<SnappedLinkState, SnappedLinkState>> {

        return integers()
            .between(1, 3)
            .flatMap { numberOfDiscreteNodes ->
                snapTwoConnectedLinks(numberOfDiscreteNodes, firstLinkDirection, secondLinkDirection)
            }
    }

    // Generate pairs of links having common node.
    fun snapTwoConnectedLinks(numberOfDiscreteNodes: Int,
                              firstLinkDirection: TrafficFlowDirectionType,
                              secondLinkDirection: TrafficFlowDirectionType)
        : Gen<Pair<SnappedLinkState, SnappedLinkState>> {

        require(numberOfDiscreteNodes in 1..3) {
            "numberOfDiscreteNodes must be in range 1..3, but was: $numberOfDiscreteNodes"
        }

        return snapTwoLinks(firstLinkDirection,
                            secondLinkDirection,
                            infrastructureLinkIdPair(),
                            pair(SNAP_POINT_LOCATION_FRACTION),
                            nodeIdQuadruple(numberOfDiscreteNodes, discreteNodesBetweenHalves = false))
    }

    // Generate pairs of links that do not have a common node.
    fun snapTwoUnconnectedLinks(): Gen<Pair<SnappedLinkState, SnappedLinkState>> {
        return trafficFlowDirectionType().flatMap { firstLinkDirection ->
            trafficFlowDirectionType().flatMap { secondLinkDirection ->

                snapTwoUnconnectedLinks(firstLinkDirection, secondLinkDirection)
            }
        }
    }

    // Generate pairs of links that do not have a common node.
    fun snapTwoUnconnectedLinks(firstLinkDirection: TrafficFlowDirectionType,
                                secondLinkDirection: TrafficFlowDirectionType)
        : Gen<Pair<SnappedLinkState, SnappedLinkState>> {

        return integers()
            .between(2, 4)
            .flatMap { numberOfDiscreteNodes ->
                snapTwoUnconnectedLinks(numberOfDiscreteNodes, firstLinkDirection, secondLinkDirection)
            }
    }

    // Generate pairs of links that do not have a common node.
    fun snapTwoUnconnectedLinks(numberOfDiscreteNodes: Int,
                                firstLinkDirection: TrafficFlowDirectionType,
                                secondLinkDirection: TrafficFlowDirectionType)
        : Gen<Pair<SnappedLinkState, SnappedLinkState>> {

        require(numberOfDiscreteNodes in 1..4) {
            "numberOfDiscreteNodes must be in range 1..4, but was: $numberOfDiscreteNodes"
        }

        return snapTwoLinks(firstLinkDirection,
                            secondLinkDirection,
                            infrastructureLinkIdPair(),
                            pair(SNAP_POINT_LOCATION_FRACTION),
                            nodeIdQuadruple(numberOfDiscreteNodes, discreteNodesBetweenHalves = true))
    }

    private fun snapTwoLinks(linkDirection1: TrafficFlowDirectionType,
                             linkDirection2: TrafficFlowDirectionType,
                             genLinkIdPair: Gen<Pair<InfrastructureLinkId, InfrastructureLinkId>>,
                             genSnapPointLocationFractionPair: Gen<Pair<Double, Double>>,
                             genNodeIds: Gen<Quadruple<InfrastructureNodeId, InfrastructureNodeId, InfrastructureNodeId, InfrastructureNodeId>>)
        : Gen<Pair<SnappedLinkState, SnappedLinkState>> {

        return genLinkIdPair.zip(DISTANCE_PAIR,
                                 genSnapPointLocationFractionPair,
                                 pair(LINK_LENGTH),
                                 genNodeIds) { (linkId1, linkId2),
                                               (distanceToLink1, distanceToLink2),
                                               (snapPointLocationFraction1, snapPointLocationFraction2),
                                               (linkLength1, linkLength2),
                                               (nodeId1, nodeId2, nodeId3, nodeId4) ->

            Pair(createSnappedLinkState(linkId1,
                                        distanceToLink1,
                                        snapPointLocationFraction1,
                                        linkDirection1,
                                        linkLength1,
                                        nodeId1, nodeId2),
                 createSnappedLinkState(linkId2,
                                        distanceToLink2,
                                        snapPointLocationFraction2,
                                        linkDirection2,
                                        linkLength2,
                                        nodeId3, nodeId4))
        }
    }

    private fun generateSnappedLinkStateFromOneNode(trafficFlowDirectionType: TrafficFlowDirectionType,
                                                    nodeProximityFilter: LinkEndpointsProximityFilter?)
        : Gen<SnappedLinkState> {

        val genSnapPointLocationFraction: Gen<Double> = nodeProximityFilter
            ?.let { generateSnapPointLocationFraction(it) }
            ?: SNAP_POINT_LOCATION_FRACTION

        return generateSnappedLinkState(trafficFlowDirectionType,
                                        genSnapPointLocationFraction,
                                        nodeIdPair(false))
    }

    private fun generateSnappedLinkStateFromTwoNodes(trafficFlowDirectionType: TrafficFlowDirectionType,
                                                     nodeProximityFilter: LinkEndpointsProximityFilter?)
        : Gen<SnappedLinkState> {

        val genSnapPointLocationFraction: Gen<Double> = nodeProximityFilter
            ?.let { generateSnapPointLocationFraction(it) }
            ?: SNAP_POINT_LOCATION_FRACTION

        return generateSnappedLinkState(trafficFlowDirectionType,
                                        genSnapPointLocationFraction,
                                        discreteNodeIdPair())
    }

    private fun generateSnappedLinkState(trafficFlowDirectionType: TrafficFlowDirectionType,
                                         genSnapPointLocationFraction: Gen<Double>,
                                         genNodeIdPair: Gen<Pair<InfrastructureNodeId, InfrastructureNodeId>>)
        : Gen<SnappedLinkState> {

        return infrastructureLinkId().zip(NON_NEGATIVE_DISTANCE,
                                          genSnapPointLocationFraction,
                                          LINK_LENGTH,
                                          genNodeIdPair) { infrastructureLinkId,
                                                           distanceToLink,
                                                           snapPointLocationFraction,
                                                           linkLength,
                                                           (startNodeId, endNodeId) ->

            createSnappedLinkState(infrastructureLinkId,
                                   distanceToLink,
                                   snapPointLocationFraction,
                                   trafficFlowDirectionType,
                                   linkLength,
                                   startNodeId, endNodeId)
        }
    }

    private fun generateSnapPointLocationFraction(nodeProximityFilter: LinkEndpointsProximityFilter): Gen<Double> =
        when (nodeProximityFilter) {
            START_NODE_CLOSER -> doubles().between(0.0, 0.5 - DOUBLE_TOLERANCE)
            END_NODE_CLOSER -> doubles().between(0.5 + DOUBLE_TOLERANCE, 1.0)
            NODES_AT_EQUAL_DISTANCE -> constant(0.5)
            START_NODE_CLOSER_OR_EQUAL_DISTANCE -> doubles().between(0.0, 0.5)
            END_NODE_CLOSER_OR_EQUAL_DISTANCE -> doubles().between(0.5, 1.0)
        }

    private fun createSnappedLinkState(infrastructureLinkId: InfrastructureLinkId,
                                       distanceToLink: Double,
                                       snapPointLocationFraction: Double,
                                       trafficFlowDirectionType: TrafficFlowDirectionType,
                                       linkLength: Double,
                                       startNodeId: InfrastructureNodeId,
                                       endNodeId: InfrastructureNodeId)
        : SnappedLinkState {

        val snappedToEndpointNode: Boolean =
            snapPointLocationFraction.isWithinTolerance(0.0) || snapPointLocationFraction.isWithinTolerance(1.0)

        val closestDistanceToLink: Double = if (snappedToEndpointNode) 0.0 else distanceToLink

        return SnappedLinkState(infrastructureLinkId,
                                closestDistanceToLink,
                                snapPointLocationFraction,
                                trafficFlowDirectionType,
                                linkLength,
                                startNodeId, endNodeId)
    }
}
