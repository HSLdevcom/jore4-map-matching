package fi.hsl.jore4.mapmatching.test.generators

import fi.hsl.jore4.mapmatching.model.InfrastructureLinkId
import fi.hsl.jore4.mapmatching.model.InfrastructureNodeId
import fi.hsl.jore4.mapmatching.model.TrafficFlowDirectionType
import fi.hsl.jore4.mapmatching.repository.infrastructure.SnappedPointOnLink
import fi.hsl.jore4.mapmatching.test.generators.CommonGenerators.ZERO_DOUBLE
import fi.hsl.jore4.mapmatching.test.generators.CommonGenerators.duplicate
import fi.hsl.jore4.mapmatching.test.generators.CommonGenerators.pair
import fi.hsl.jore4.mapmatching.test.generators.CommonGenerators.shuffledPair
import fi.hsl.jore4.mapmatching.test.generators.EnumGenerators.locationAlongLinkType
import fi.hsl.jore4.mapmatching.test.generators.EnumGenerators.trafficFlowDirectionType
import fi.hsl.jore4.mapmatching.test.generators.InfrastructureLinkIdGenerator.infrastructureLinkId
import fi.hsl.jore4.mapmatching.test.generators.InfrastructureLinkIdGenerator.infrastructureLinkIdPair
import fi.hsl.jore4.mapmatching.test.generators.InfrastructureNodeIdGenerator.discreteNodeIdPair
import fi.hsl.jore4.mapmatching.test.generators.InfrastructureNodeIdGenerator.nodeIdPair
import fi.hsl.jore4.mapmatching.test.generators.InfrastructureNodeIdGenerator.nodeIdQuadrupleForSingleLink
import fi.hsl.jore4.mapmatching.test.generators.InfrastructureNodeIdGenerator.nodeIdQuadrupleForTwoLinks
import fi.hsl.jore4.mapmatching.test.generators.SnapPointLocationAlongLinkFilter.AT_END
import fi.hsl.jore4.mapmatching.test.generators.SnapPointLocationAlongLinkFilter.AT_MIDPOINT
import fi.hsl.jore4.mapmatching.test.generators.SnapPointLocationAlongLinkFilter.AT_OR_CLOSE_TO_END
import fi.hsl.jore4.mapmatching.test.generators.SnapPointLocationAlongLinkFilter.AT_OR_CLOSE_TO_START
import fi.hsl.jore4.mapmatching.test.generators.SnapPointLocationAlongLinkFilter.AT_START
import fi.hsl.jore4.mapmatching.test.generators.SnapPointLocationAlongLinkFilter.BETWEEN_ENDPOINTS_EXCLUSIVE
import fi.hsl.jore4.mapmatching.test.generators.SnapPointLocationAlongLinkFilter.CLOSE_TO_END
import fi.hsl.jore4.mapmatching.test.generators.SnapPointLocationAlongLinkFilter.CLOSE_TO_START
import fi.hsl.jore4.mapmatching.test.generators.SnapPointLocationAlongLinkFilter.IN_FIRST_HALF
import fi.hsl.jore4.mapmatching.test.generators.SnapPointLocationAlongLinkFilter.IN_SECOND_HALF
import fi.hsl.jore4.mapmatching.test.generators.SnapPointLocationAlongLinkFilter.NOT_AT_END
import fi.hsl.jore4.mapmatching.test.generators.SnapPointLocationAlongLinkFilter.NOT_AT_START
import fi.hsl.jore4.mapmatching.test.util.Quadruple
import fi.hsl.jore4.mapmatching.util.MathUtils.DEFAULT_DOUBLE_TOLERANCE
import org.quicktheories.core.Gen
import org.quicktheories.generators.Generate.booleans
import org.quicktheories.generators.Generate.constant
import org.quicktheories.generators.Generate.oneOf
import org.quicktheories.generators.SourceDSL.doubles

object SnappedPointOnLinkGenerator {
    data class SnappedPointOnLinkParams(
        val hasDiscreteNodes: Boolean,
        val trafficFlowDirectionType: TrafficFlowDirectionType,
        val snapPointLocationFilter: SnapPointLocationAlongLinkFilter
    )

    // random distances from arbitrary point to the closest link
    private val POSITIVE_DISTANCE: Gen<Double> = doubles().between(0.5, 50.0)

    // mix 10% zeros
    private val NON_NEGATIVE_DISTANCE: Gen<Double> = POSITIVE_DISTANCE.mix(ZERO_DOUBLE, 10)

    private val DISTANCE_PAIR: Gen<Pair<Double, Double>> = shuffledPair(NON_NEGATIVE_DISTANCE, POSITIVE_DISTANCE)

    private val SNAP_POINT_LOCATION_FRACTION: Gen<Double> = doubles().between(0.0, 1.0)

    private val LINK_LENGTH: Gen<Double> = doubles().between(2.0, 5_000.0)

    fun snapLink(): Gen<SnappedPointOnLink> = booleans().flatMap(this::snapLink)

    fun snapLink(hasDiscreteEndpoints: Boolean): Gen<SnappedPointOnLink> {
        return locationAlongLinkType()
            .flatMap { snapPointLocationFilter ->
                snapLink(hasDiscreteEndpoints, snapPointLocationFilter)
            }
    }

    fun snapLink(
        hasDiscreteEndpoints: Boolean,
        snapPointLocationFilter: SnapPointLocationAlongLinkFilter
    ): Gen<SnappedPointOnLink> {
        return trafficFlowDirectionType()
            .flatMap { trafficFlowDirectionType ->
                snapLink(hasDiscreteEndpoints, trafficFlowDirectionType, snapPointLocationFilter)
            }
    }

    fun snapLink(
        hasDiscreteEndpoints: Boolean,
        trafficFlowDirectionType: TrafficFlowDirectionType,
        snapPointLocationFilter: SnapPointLocationAlongLinkFilter
    ): Gen<SnappedPointOnLink> {
        val createSnappedPointOnLink: (
            SnapPointLocationAlongLinkFilter
        ) -> Gen<SnappedPointOnLink> = { locationFilter ->
            if (hasDiscreteEndpoints) {
                generateSnappedPointOnLinkFromTwoNodes(trafficFlowDirectionType, locationFilter)
            } else {
                generateSnappedPointOnLinkFromOneNode(trafficFlowDirectionType, locationFilter)
            }
        }

        return when (snapPointLocationFilter) {
            IN_FIRST_HALF ->
                oneOf(
                    createSnappedPointOnLink(AT_START),
                    createSnappedPointOnLink(CLOSE_TO_START),
                    createSnappedPointOnLink(AT_MIDPOINT)
                )

            IN_SECOND_HALF ->
                oneOf(
                    createSnappedPointOnLink(AT_END),
                    createSnappedPointOnLink(CLOSE_TO_END),
                    createSnappedPointOnLink(AT_MIDPOINT)
                )

            else -> createSnappedPointOnLink(snapPointLocationFilter)
        }
    }

    fun snapSingleLinkTwice(
        withDiscreteEndpoints: Boolean,
        trafficFlowDirectionType: TrafficFlowDirectionType,
        firstSnapPointLocationFilter: SnapPointLocationAlongLinkFilter,
        secondSnapPointLocationFilter: SnapPointLocationAlongLinkFilter
    ): Gen<Pair<SnappedPointOnLink, SnappedPointOnLink>> {
        return snapTwoLinks(
            trafficFlowDirectionType,
            trafficFlowDirectionType,
            duplicate(infrastructureLinkId()),
            generatePairOfSnapPointLocationFractions(
                firstSnapPointLocationFilter,
                secondSnapPointLocationFilter
            ),
            nodeIdQuadrupleForSingleLink(withDiscreteEndpoints)
        )
    }

    // Generate two links having common node.
    fun snapTwoConnectedLinks(): Gen<Pair<SnappedPointOnLink, SnappedPointOnLink>> {
        return generateSnappedPointOnLinkParams().flatMap { firstLinkParams ->
            generateSnappedPointOnLinkParams().flatMap { secondLinkParams ->

                snapTwoConnectedLinks(firstLinkParams, secondLinkParams)
            }
        }
    }

    // Generate two links having common node.
    fun snapTwoConnectedLinks(
        firstLinkParams: SnappedPointOnLinkParams,
        secondLinkParams: SnappedPointOnLinkParams
    ): Gen<Pair<SnappedPointOnLink, SnappedPointOnLink>> {
        return snapTwoLinks(firstLinkParams, secondLinkParams, true)
    }

    // Generate pairs of links that do not have a common node.
    fun snapTwoUnconnectedLinks(): Gen<Pair<SnappedPointOnLink, SnappedPointOnLink>> {
        return generateSnappedPointOnLinkParams().flatMap { firstLinkParams ->
            generateSnappedPointOnLinkParams().flatMap { secondLinkParams ->

                snapTwoUnconnectedLinks(firstLinkParams, secondLinkParams)
            }
        }
    }

    // Generate pairs of links that do not have a common node.
    fun snapTwoUnconnectedLinks(
        firstLinkParams: SnappedPointOnLinkParams,
        secondLinkParams: SnappedPointOnLinkParams
    ): Gen<Pair<SnappedPointOnLink, SnappedPointOnLink>> {
        return snapTwoLinks(firstLinkParams, secondLinkParams, false)
    }

    private fun snapTwoLinks(
        firstLinkParams: SnappedPointOnLinkParams,
        secondLinkParams: SnappedPointOnLinkParams,
        linksConnected: Boolean
    ): Gen<Pair<SnappedPointOnLink, SnappedPointOnLink>> {
        return snapTwoLinks(
            firstLinkParams.trafficFlowDirectionType,
            secondLinkParams.trafficFlowDirectionType,
            infrastructureLinkIdPair(),
            generatePairOfSnapPointLocationFractions(
                firstLinkParams.snapPointLocationFilter,
                secondLinkParams.snapPointLocationFilter
            ),
            nodeIdQuadrupleForTwoLinks(
                firstLinkParams.hasDiscreteNodes,
                secondLinkParams.hasDiscreteNodes,
                linksConnected
            )
        )
    }

    private fun snapTwoLinks(
        linkDirection1: TrafficFlowDirectionType,
        linkDirection2: TrafficFlowDirectionType,
        genLinkIdPair: Gen<Pair<InfrastructureLinkId, InfrastructureLinkId>>,
        genSnapPointLocationFractionPair: Gen<Pair<Double, Double>>,
        genNodeIds:
            Gen<Quadruple<InfrastructureNodeId, InfrastructureNodeId, InfrastructureNodeId, InfrastructureNodeId>>
    ): Gen<Pair<SnappedPointOnLink, SnappedPointOnLink>> {
        return genLinkIdPair.zip(
            DISTANCE_PAIR,
            genSnapPointLocationFractionPair,
            pair(LINK_LENGTH),
            genNodeIds
        ) { (linkId1, linkId2),
            (distanceToLink1, distanceToLink2),
            (snapPointLocationFraction1, snapPointLocationFraction2),
            (linkLength1, linkLength2),
            (nodeId1, nodeId2, nodeId3, nodeId4) ->

            Pair(
                SnappedPointOnLink(
                    linkId1,
                    distanceToLink1,
                    snapPointLocationFraction1,
                    linkDirection1,
                    linkLength1,
                    nodeId1,
                    nodeId2
                ),
                SnappedPointOnLink(
                    linkId2,
                    distanceToLink2,
                    snapPointLocationFraction2,
                    linkDirection2,
                    linkLength2,
                    nodeId3,
                    nodeId4
                )
            )
        }
    }

    private fun generateSnappedPointOnLinkFromOneNode(
        trafficFlowDirectionType: TrafficFlowDirectionType,
        snapPointLocationFilter: SnapPointLocationAlongLinkFilter?
    ): Gen<SnappedPointOnLink> {
        return generateSnappedPointOnLink(
            trafficFlowDirectionType,
            snapPointLocationFilter,
            nodeIdPair(false)
        )
    }

    private fun generateSnappedPointOnLinkFromTwoNodes(
        trafficFlowDirectionType: TrafficFlowDirectionType,
        snapPointLocationFilter: SnapPointLocationAlongLinkFilter?
    ): Gen<SnappedPointOnLink> {
        return generateSnappedPointOnLink(
            trafficFlowDirectionType,
            snapPointLocationFilter,
            discreteNodeIdPair()
        )
    }

    private fun generateSnappedPointOnLink(
        trafficFlowDirectionType: TrafficFlowDirectionType,
        snapPointLocationFilter: SnapPointLocationAlongLinkFilter?,
        genNodeIdPair: Gen<Pair<InfrastructureNodeId, InfrastructureNodeId>>
    ): Gen<SnappedPointOnLink> {
        val genSnapPointLocationFraction: Gen<Double> =
            snapPointLocationFilter
                ?.let { generateSnapPointLocationFraction(it) }
                ?: SNAP_POINT_LOCATION_FRACTION

        return generateSnappedPointOnLink(
            trafficFlowDirectionType,
            genSnapPointLocationFraction,
            genNodeIdPair
        )
    }

    private fun generateSnappedPointOnLink(
        trafficFlowDirectionType: TrafficFlowDirectionType,
        genSnapPointLocationFraction: Gen<Double>,
        genNodeIdPair: Gen<Pair<InfrastructureNodeId, InfrastructureNodeId>>
    ): Gen<SnappedPointOnLink> {
        return infrastructureLinkId().zip(
            NON_NEGATIVE_DISTANCE,
            genSnapPointLocationFraction,
            LINK_LENGTH,
            genNodeIdPair
        ) { infrastructureLinkId,
            distanceToLink,
            snapPointLocationFraction,
            linkLength,
            (startNodeId, endNodeId) ->

            SnappedPointOnLink(
                infrastructureLinkId,
                distanceToLink,
                snapPointLocationFraction,
                trafficFlowDirectionType,
                linkLength,
                startNodeId,
                endNodeId
            )
        }
    }

    private fun generateSnapPointLocationFraction(
        snapPointLocationFilter: SnapPointLocationAlongLinkFilter
    ): Gen<Double> {
        return when (snapPointLocationFilter) {
            IN_FIRST_HALF -> doubles().between(0.0, 0.5)
            IN_SECOND_HALF -> doubles().between(0.5, 1.0)

            AT_MIDPOINT -> constant(0.5)
            BETWEEN_ENDPOINTS_EXCLUSIVE ->
                doubles().between(
                    DEFAULT_DOUBLE_TOLERANCE,
                    1.0 - DEFAULT_DOUBLE_TOLERANCE
                )

            AT_START ->
                doubles().between(
                    0.0,
                    0.95 * DEFAULT_DOUBLE_TOLERANCE
                )

            AT_END ->
                doubles().between(
                    1.0 - 0.95 * DEFAULT_DOUBLE_TOLERANCE,
                    1.0
                )

            AT_OR_CLOSE_TO_START ->
                doubles().between(
                    0.0,
                    0.5 - DEFAULT_DOUBLE_TOLERANCE
                )

            AT_OR_CLOSE_TO_END ->
                doubles().between(
                    0.5 + DEFAULT_DOUBLE_TOLERANCE,
                    1.0
                )

            CLOSE_TO_START ->
                doubles().between(
                    DEFAULT_DOUBLE_TOLERANCE,
                    0.5 - DEFAULT_DOUBLE_TOLERANCE
                )

            CLOSE_TO_END ->
                doubles().between(
                    0.5 + DEFAULT_DOUBLE_TOLERANCE,
                    1.0 - DEFAULT_DOUBLE_TOLERANCE
                )

            NOT_AT_START ->
                doubles().between(
                    DEFAULT_DOUBLE_TOLERANCE,
                    1.0
                )

            NOT_AT_END ->
                doubles().between(
                    0.0,
                    1.0 - DEFAULT_DOUBLE_TOLERANCE
                )
        }
    }

    private fun generatePairOfSnapPointLocationFractions(
        snapPointLocationFilter1: SnapPointLocationAlongLinkFilter,
        snapPointLocationFilter2: SnapPointLocationAlongLinkFilter
    ): Gen<Pair<Double, Double>> {
        return generateSnapPointLocationFraction(snapPointLocationFilter1)
            .flatMap { locationFraction1: Double ->
                generateSnapPointLocationFraction(snapPointLocationFilter2)
                    .map { locationFraction2: Double ->

                        locationFraction1 to locationFraction2
                    }
            }
    }

    private fun generateSnappedPointOnLinkParams(): Gen<SnappedPointOnLinkParams> {
        return booleans()
            .flatMap { discreteNodes ->
                trafficFlowDirectionType()
                    .flatMap { trafficFlowDirectionType ->
                        locationAlongLinkType()
                            .map { snapPointLocationFilter ->
                                SnappedPointOnLinkParams(
                                    discreteNodes,
                                    trafficFlowDirectionType,
                                    snapPointLocationFilter
                                )
                            }
                    }
            }
    }
}
