package fi.hsl.jore4.mapmatching.service.matching

import arrow.core.Either
import arrow.core.Either.Left
import arrow.core.Either.Right
import fi.hsl.jore4.mapmatching.model.NodeIdSequence
import fi.hsl.jore4.mapmatching.model.NodeProximity
import fi.hsl.jore4.mapmatching.model.VehicleType
import fi.hsl.jore4.mapmatching.model.matching.RouteJunctionPoint
import fi.hsl.jore4.mapmatching.model.matching.RoutePoint
import fi.hsl.jore4.mapmatching.model.matching.RouteStopPoint
import fi.hsl.jore4.mapmatching.repository.infrastructure.IStopRepository
import fi.hsl.jore4.mapmatching.repository.infrastructure.PublicTransportStopMatchParameters
import fi.hsl.jore4.mapmatching.repository.infrastructure.SnapStopToLinkDTO
import fi.hsl.jore4.mapmatching.repository.infrastructure.SnappedLinkState
import fi.hsl.jore4.mapmatching.repository.routing.BufferAreaRestriction
import fi.hsl.jore4.mapmatching.repository.routing.RouteLinkDTO
import fi.hsl.jore4.mapmatching.service.common.IRoutingServiceInternal
import fi.hsl.jore4.mapmatching.service.common.response.RoutingResponse
import fi.hsl.jore4.mapmatching.service.common.response.RoutingResponseCreator
import fi.hsl.jore4.mapmatching.service.matching.MatchingServiceHelper.getSourceRouteTerminusPoint
import fi.hsl.jore4.mapmatching.service.matching.MatchingServiceHelper.validateInputForRouteMatching
import fi.hsl.jore4.mapmatching.service.node.INodeServiceInternal
import fi.hsl.jore4.mapmatching.service.node.NodeSequenceCandidatesBetweenSnappedLinks
import fi.hsl.jore4.mapmatching.service.node.NodeSequenceResolutionFailed
import fi.hsl.jore4.mapmatching.service.node.NodeSequenceResolutionResult
import fi.hsl.jore4.mapmatching.service.node.NodeSequenceResolutionSucceeded
import fi.hsl.jore4.mapmatching.util.GeolatteUtils.toPoint
import fi.hsl.jore4.mapmatching.util.LogUtils.joinToLogString
import mu.KotlinLogging
import org.geolatte.geom.G2D
import org.geolatte.geom.LineString
import org.geolatte.geom.Point
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

private val LOGGER = KotlinLogging.logger {}

@Service
class MatchingServiceImpl @Autowired constructor(val stopRepository: IStopRepository,
                                                 val closestTerminusLinksResolver: IClosestTerminusLinksResolver,
                                                 val roadJunctionMatcher: IRoadJunctionMatcher,
                                                 val nodeService: INodeServiceInternal,
                                                 val routingService: IRoutingServiceInternal)
    : IMatchingService {

    internal data class InfrastructureLinksOnRoute(val startLinkCandidates: List<TerminusLinkCandidate>,
                                                   val endLinkCandidates: List<TerminusLinkCandidate>,
                                                   val viaLinksIndexedByRoutePointOrdering: Map<Int, SnappedLinkState>)

    @Transactional(readOnly = true)
    override fun findMatchForPublicTransportRoute(routeGeometry: LineString<G2D>,
                                                  routePoints: List<RoutePoint>,
                                                  vehicleType: VehicleType,
                                                  matchingParameters: PublicTransportRouteMatchingParameters)
        : RoutingResponse {

        validateInputForRouteMatching(routePoints, vehicleType)?.let { validationError ->
            return RoutingResponse.invalidValue(validationError)
        }

        val terminusLinkSelectionInput: TerminusLinkSelectionInput = try {
            resolveTerminusLinkSelectionInput(routeGeometry,
                                              routePoints,
                                              vehicleType,
                                              matchingParameters.terminusLinkQueryDistance,
                                              matchingParameters.terminusLinkQueryLimit)
        } catch (ex: RuntimeException) {
            val errMessage: String = ex.message ?: "Failed to find closest terminus links on either end of route"
            LOGGER.warn(errMessage)
            return RoutingResponse.noSegment(errMessage)
        }

        val nodeSequenceCandidates: List<NodeSequenceCandidatesBetweenSnappedLinks> = try {
            resolveNodeSequenceCandidates(routePoints,
                                          vehicleType,
                                          terminusLinkSelectionInput,
                                          matchingParameters)
        } catch (ex: RuntimeException) {
            val errMessage: String =
                ex.message ?: "Could not resolve node sequence candidates while map-matching via nodes (graph vertices)"
            LOGGER.warn(errMessage)
            return RoutingResponse.noSegment(errMessage)
        }

        val nodeSeqResult: NodeSequenceResolutionResult =
            nodeService.resolveNodeIdSequence(nodeSequenceCandidates,
                                              vehicleType,
                                              BufferAreaRestriction(routeGeometry,
                                                                    matchingParameters.bufferRadiusInMeters))

        return when (nodeSeqResult) {
            is NodeSequenceResolutionSucceeded -> {

                val nodeIdSequence: NodeIdSequence = nodeSeqResult.nodeIdSequence

                LOGGER.debug { "Resolved node ID sequence: $nodeIdSequence" }

                val startLink: SnappedLinkState = nodeSeqResult.startLink
                val endLink: SnappedLinkState = nodeSeqResult.endLink

                val routeLinks: List<RouteLinkDTO> = routingService
                    .findRouteViaNodes(nodeIdSequence,
                                       vehicleType,
                                       startLink.closestPointFractionalMeasure,
                                       endLink.closestPointFractionalMeasure,
                                       BufferAreaRestriction.from(routeGeometry,
                                                                  matchingParameters.bufferRadiusInMeters,
                                                                  startLink,
                                                                  endLink))
                    .also { routeLinks: List<RouteLinkDTO> ->
                        if (routeLinks.isNotEmpty()) {
                            LOGGER.debug { "Got route links: ${joinToLogString(routeLinks)}" }
                        }
                    }

                RoutingResponseCreator.create(routeLinks)
            }

            is NodeSequenceResolutionFailed -> {
                LOGGER.warn(nodeSeqResult.message)
                RoutingResponse.noSegment(nodeSeqResult.message)
            }
        }
    }

    /**
     * @throws [IllegalStateException] if no links are found for one or both of the two endpoints
     * of the route
     */
    internal fun resolveTerminusLinkSelectionInput(routeGeometry: LineString<G2D>,
                                                   routePoints: List<RoutePoint>,
                                                   vehicleType: VehicleType,
                                                   terminusLinkQueryDistance: Double,
                                                   terminusLinkQueryLimit: Int)
        : TerminusLinkSelectionInput {

        // The terminus locations are extracted from the LineString geometry of the source route
        // instead of the route point entities (mostly stop point instances) since in this context
        // we are interested in the start/end coordinates of the source route line.
        val startLocation: Point<G2D> = toPoint(routeGeometry.startPosition)
        val endLocation: Point<G2D> = toPoint(routeGeometry.endPosition)

        val (closestStartLinks: List<SnappedLinkState>, closestEndLinks: List<SnappedLinkState>) =
            closestTerminusLinksResolver.findClosestInfrastructureLinksForRouteEndpoints(startLocation,
                                                                                         endLocation,
                                                                                         vehicleType,
                                                                                         terminusLinkQueryDistance,
                                                                                         terminusLinkQueryLimit)

        return TerminusLinkSelectionInput(getSourceRouteTerminusPoint(routePoints.first(), startLocation, true),
                                          closestStartLinks,
                                          getSourceRouteTerminusPoint(routePoints.last(), endLocation, false),
                                          closestEndLinks)
    }

    /**
     * @throws [IllegalStateException]
     */
    internal fun resolveNodeSequenceCandidates(routePoints: List<RoutePoint>,
                                               vehicleType: VehicleType,
                                               TerminusLinkSelectionInput: TerminusLinkSelectionInput,
                                               matchingParams: PublicTransportRouteMatchingParameters)
        : List<NodeSequenceCandidatesBetweenSnappedLinks> {

        // Resolve infrastructure links to visit on route derived from the given geometry and route points.
        val (
            startLinkCandidates: List<TerminusLinkCandidate>,
            endLinkCandidates: List<TerminusLinkCandidate>,
            fromRouteStopPointIndexToInfrastructureLink: Map<Int, SnappedLinkState?>
        ) = resolveInfrastructureLinksOnRoute(routePoints,
                                              TerminusLinkSelectionInput,
                                              matchingParams.maxStopLocationDeviation)

        // Resolve infrastructure network nodes to visit on route derived from the given route points.
        val fromRoutePointIndexToRoadJunctionNode: Map<Int, NodeProximity?> = matchingParams.roadJunctionMatching
            ?.let { (matchDistance, clearingDistance) ->
                roadJunctionMatcher.findInfrastructureNodesMatchingRoadJunctions(routePoints,
                                                                                 vehicleType,
                                                                                 matchDistance,
                                                                                 clearingDistance)
            }
            ?: emptyMap()

        val viaNodeResolvers: List<Either<SnappedLinkState, NodeProximity>> = routePoints
            .withIndex()
            .drop(1)
            .dropLast(1)
            .mapNotNull { (routePointIndex: Int, routePoint: RoutePoint) ->

                when (routePoint) {
                    is RouteStopPoint -> fromRouteStopPointIndexToInfrastructureLink[routePointIndex]?.let(::Left)
                    is RouteJunctionPoint -> fromRoutePointIndexToRoadJunctionNode[routePointIndex]?.let(::Right)
                    else -> null
                }
            }

        return MatchingServiceHelper.getSortedNodeSequenceCandidates(startLinkCandidates,
                                                                     endLinkCandidates,
                                                                     viaNodeResolvers)
    }

    internal fun resolveInfrastructureLinksOnRoute(routePoints: List<RoutePoint>,
                                                   terminusLinkSelectionInput: TerminusLinkSelectionInput,
                                                   maxStopLocationDeviation: Double)
        : InfrastructureLinksOnRoute {

        val fromRoutePointIndexToStopMatchParams: Map<Int, PublicTransportStopMatchParameters> =
            MatchingServiceHelper.getMappingFromRoutePointIndexesToStopMatchParameters(routePoints)

        val snappedLinksFromStops: List<SnapStopToLinkDTO> =
            stopRepository.findStopsAndSnapToInfrastructureLinks(fromRoutePointIndexToStopMatchParams.values,
                                                                 maxStopLocationDeviation)

        val fromStopNationalIdToInfrastructureLink: Map<Int, SnappedLinkState> =
            snappedLinksFromStops.associateBy(SnapStopToLinkDTO::stopNationalId, SnapStopToLinkDTO::link)

        val fromRoutePointIndexToMatchedStopNationalId: Map<Int, Int> = fromRoutePointIndexToStopMatchParams
            .mapValues { mapEntry -> mapEntry.value.nationalId }
            .filterValues(fromStopNationalIdToInfrastructureLink::containsKey)

        LOGGER.debug {
            "Matched following public transport stop points from source route points: ${
                joinToLogString(fromRoutePointIndexToMatchedStopNationalId.toSortedMap().entries) {
                    "Route point #${it.key + 1}: nationalId=${it.value}"
                }
            }"
        }

        val (startLinkCandidates: List<TerminusLinkCandidate>, endLinkCandidates: List<TerminusLinkCandidate>) =
            MatchingServiceHelper.resolveTerminusLinkCandidates(
                terminusLinkSelectionInput,
                fromStopNationalIdToInfrastructureLink.mapValues { it.value.infrastructureLinkId })

        val fromRouteStopPointIndexToInfrastructureLink: Map<Int, SnappedLinkState> =
            fromRoutePointIndexToMatchedStopNationalId.mapValues { (_, stopNationalId: Int) ->
                fromStopNationalIdToInfrastructureLink[stopNationalId]!!
            }

        return InfrastructureLinksOnRoute(startLinkCandidates,
                                          endLinkCandidates,
                                          fromRouteStopPointIndexToInfrastructureLink)
    }
}
