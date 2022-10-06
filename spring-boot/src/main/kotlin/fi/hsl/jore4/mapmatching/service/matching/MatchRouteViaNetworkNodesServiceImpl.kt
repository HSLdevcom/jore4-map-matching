package fi.hsl.jore4.mapmatching.service.matching

import arrow.core.Either
import arrow.core.Either.Left
import arrow.core.Either.Right
import fi.hsl.jore4.mapmatching.model.InfrastructureLinkId
import fi.hsl.jore4.mapmatching.model.NodeIdSequence
import fi.hsl.jore4.mapmatching.model.NodeProximity
import fi.hsl.jore4.mapmatching.model.VehicleType
import fi.hsl.jore4.mapmatching.model.matching.RouteJunctionPoint
import fi.hsl.jore4.mapmatching.model.matching.RoutePoint
import fi.hsl.jore4.mapmatching.model.matching.RouteStopPoint
import fi.hsl.jore4.mapmatching.repository.infrastructure.SnapStopToLinkDTO
import fi.hsl.jore4.mapmatching.repository.infrastructure.SnappedLinkState
import fi.hsl.jore4.mapmatching.repository.routing.BufferAreaRestriction
import fi.hsl.jore4.mapmatching.repository.routing.RouteLinkDTO
import fi.hsl.jore4.mapmatching.service.common.IRoutingServiceInternal
import fi.hsl.jore4.mapmatching.service.common.response.RoutingResponse
import fi.hsl.jore4.mapmatching.service.common.response.RoutingResponseCreator
import fi.hsl.jore4.mapmatching.service.matching.MatchingServiceHelper.getSourceRouteTerminusPoint
import fi.hsl.jore4.mapmatching.service.node.INodeServiceInternal
import fi.hsl.jore4.mapmatching.service.node.NodeSequenceCandidatesBetweenSnappedLinks
import fi.hsl.jore4.mapmatching.service.node.NodeSequenceResolutionFailed
import fi.hsl.jore4.mapmatching.service.node.NodeSequenceResolutionResult
import fi.hsl.jore4.mapmatching.service.node.NodeSequenceResolutionSucceeded
import fi.hsl.jore4.mapmatching.util.GeolatteUtils.toPoint
import fi.hsl.jore4.mapmatching.util.InternalService
import fi.hsl.jore4.mapmatching.util.LogUtils.joinToLogString
import mu.KotlinLogging
import org.geolatte.geom.G2D
import org.geolatte.geom.LineString
import org.geolatte.geom.Point
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.transaction.annotation.Transactional

private val LOGGER = KotlinLogging.logger {}

@InternalService
class MatchRouteViaNetworkNodesServiceImpl @Autowired constructor(
    val closestTerminusLinksResolver: IClosestTerminusLinksResolver,
    val publicTransportStopMatcher: IPublicTransportStopMatcher,
    val roadJunctionMatcher: IRoadJunctionMatcher,
    val nodeService: INodeServiceInternal,
    val routingService: IRoutingServiceInternal
) : IMatchRouteViaNetworkNodesService {

    internal data class InfrastructureLinksOnRoute(val startLinkCandidates: List<TerminusLinkCandidate>,
                                                   val endLinkCandidates: List<TerminusLinkCandidate>,
                                                   val viaLinksIndexedByRoutePointOrdering: Map<Int, SnappedLinkState>)

    @Transactional(readOnly = true, noRollbackFor = [RuntimeException::class])
    override fun findMatchForPublicTransportRoute(sourceRouteGeometry: LineString<G2D>,
                                                  sourceRoutePoints: List<RoutePoint>,
                                                  vehicleType: VehicleType,
                                                  matchingParameters: PublicTransportRouteMatchingParameters)
        : RoutingResponse {

        val terminusLinkSelectionInput: TerminusLinkSelectionInput = try {
            resolveTerminusLinkSelectionInput(sourceRouteGeometry,
                                              sourceRoutePoints,
                                              vehicleType,
                                              matchingParameters.terminusLinkQueryDistance,
                                              matchingParameters.terminusLinkQueryLimit)
        } catch (ex: RuntimeException) {
            val errMessage: String = ex.message ?: "Failed to find closest terminus links on either end of route"
            return RoutingResponse.noSegment(errMessage)
        }

        val nodeSequenceCandidates: List<NodeSequenceCandidatesBetweenSnappedLinks> = try {
            resolveNodeSequenceCandidates(sourceRoutePoints,
                                          vehicleType,
                                          terminusLinkSelectionInput,
                                          matchingParameters)
        } catch (ex: RuntimeException) {
            val errMessage: String =
                ex.message ?: "Could not resolve node sequence candidates while map-matching via nodes (graph vertices)"
            return RoutingResponse.noSegment(errMessage)
        }

        val nodeSeqResult: NodeSequenceResolutionResult =
            nodeService.resolveNodeIdSequence(nodeSequenceCandidates,
                                              vehicleType,
                                              BufferAreaRestriction(sourceRouteGeometry,
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
                                       BufferAreaRestriction.from(sourceRouteGeometry,
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

            is NodeSequenceResolutionFailed -> RoutingResponse.noSegment(nodeSeqResult.message)
        }
    }

    /**
     * @throws [IllegalStateException] if no links are found for one or both of the two endpoints
     * of the route
     */
    internal fun resolveTerminusLinkSelectionInput(sourceRouteGeometry: LineString<G2D>,
                                                   sourceRoutePoints: List<RoutePoint>,
                                                   vehicleType: VehicleType,
                                                   terminusLinkQueryDistance: Double,
                                                   terminusLinkQueryLimit: Int)
        : TerminusLinkSelectionInput {

        // The terminus locations are extracted from the LineString geometry of the source route
        // instead of the route point entities (mostly stop point instances) since in this context
        // we are interested in the start/end coordinates of the source route line.
        val startLocation: Point<G2D> = toPoint(sourceRouteGeometry.startPosition)
        val endLocation: Point<G2D> = toPoint(sourceRouteGeometry.endPosition)

        val (closestStartLinks: List<SnappedLinkState>, closestEndLinks: List<SnappedLinkState>) =
            closestTerminusLinksResolver.findClosestInfrastructureLinksForRouteEndpoints(startLocation,
                                                                                         endLocation,
                                                                                         vehicleType,
                                                                                         terminusLinkQueryDistance,
                                                                                         terminusLinkQueryLimit)

        return TerminusLinkSelectionInput(getSourceRouteTerminusPoint(sourceRoutePoints.first(), startLocation, true),
                                          closestStartLinks,
                                          getSourceRouteTerminusPoint(sourceRoutePoints.last(), endLocation, false),
                                          closestEndLinks)
    }

    /**
     * @throws [IllegalStateException]
     */
    internal fun resolveNodeSequenceCandidates(sourceRoutePoints: List<RoutePoint>,
                                               vehicleType: VehicleType,
                                               TerminusLinkSelectionInput: TerminusLinkSelectionInput,
                                               matchingParams: PublicTransportRouteMatchingParameters)
        : List<NodeSequenceCandidatesBetweenSnappedLinks> {

        // Resolve infrastructure links to visit on route derived from the given geometry and route points.
        val (
            startLinkCandidates: List<TerminusLinkCandidate>,
            endLinkCandidates: List<TerminusLinkCandidate>,
            fromRouteStopPointIndexToInfrastructureLink: Map<Int, SnappedLinkState?>
        ) = resolveInfrastructureLinksOnRoute(sourceRoutePoints,
                                              TerminusLinkSelectionInput,
                                              matchingParams.maxStopLocationDeviation)

        // Resolve infrastructure network nodes to visit on route derived from the given route points.
        val fromRoutePointIndexToRoadJunctionNode: Map<Int, NodeProximity?> = matchingParams.roadJunctionMatching
            ?.let { (matchDistance, clearingDistance) ->
                roadJunctionMatcher.findInfrastructureNodesMatchingRoadJunctions(sourceRoutePoints,
                                                                                 vehicleType,
                                                                                 matchDistance,
                                                                                 clearingDistance)
            }
            ?: emptyMap()

        val viaNodeHolders: List<Either<SnappedLinkState, NodeProximity>> = sourceRoutePoints
            .withIndex()
            .drop(1)
            .dropLast(1)
            .mapNotNull { (sourceRoutePointIndex: Int, sourceRoutePoint: RoutePoint) ->

                when (sourceRoutePoint) {
                    is RouteStopPoint -> fromRouteStopPointIndexToInfrastructureLink[sourceRoutePointIndex]?.let(::Left)
                    is RouteJunctionPoint -> fromRoutePointIndexToRoadJunctionNode[sourceRoutePointIndex]?.let(::Right)
                    else -> null
                }
            }

        return MatchingServiceHelper.getSortedNodeSequenceCandidates(startLinkCandidates,
                                                                     endLinkCandidates,
                                                                     viaNodeHolders)
    }

    internal fun resolveInfrastructureLinksOnRoute(sourceRoutePoints: List<RoutePoint>,
                                                   terminusLinkSelectionInput: TerminusLinkSelectionInput,
                                                   maxStopLocationDeviation: Double)
        : InfrastructureLinksOnRoute {

        val fromRoutePointIndexToSnappedLinkOfMatchedStop: Map<Int, SnapStopToLinkDTO> = publicTransportStopMatcher
            .findStopPointsByNationalIdsAndIndexByRoutePointOrdering(sourceRoutePoints, maxStopLocationDeviation)

        val fromStopNationalIdToInfrastructureLinkId: Map<Int, InfrastructureLinkId> =
            fromRoutePointIndexToSnappedLinkOfMatchedStop
                .values
                .associateBy(SnapStopToLinkDTO::stopNationalId) { it.link.infrastructureLinkId }

        val (startLinkCandidates: List<TerminusLinkCandidate>, endLinkCandidates: List<TerminusLinkCandidate>) =
            MatchingServiceHelper.resolveTerminusLinkCandidates(terminusLinkSelectionInput,
                                                                fromStopNationalIdToInfrastructureLinkId)

        val fromRouteStopPointIndexToPointOnLink: Map<Int, SnappedLinkState> =
            fromRoutePointIndexToSnappedLinkOfMatchedStop.mapValues { (_, snap) -> snap.link }

        return InfrastructureLinksOnRoute(startLinkCandidates,
                                          endLinkCandidates,
                                          fromRouteStopPointIndexToPointOnLink)
    }
}
