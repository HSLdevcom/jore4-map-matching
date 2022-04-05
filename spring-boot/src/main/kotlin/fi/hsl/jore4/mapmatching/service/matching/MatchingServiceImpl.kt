package fi.hsl.jore4.mapmatching.service.matching

import arrow.core.Either
import arrow.core.Either.Left
import arrow.core.Either.Right
import fi.hsl.jore4.mapmatching.Constants
import fi.hsl.jore4.mapmatching.model.HasInfrastructureNodeId
import fi.hsl.jore4.mapmatching.model.InfrastructureLinkId
import fi.hsl.jore4.mapmatching.model.InfrastructureNodeId
import fi.hsl.jore4.mapmatching.model.NodeIdSequence
import fi.hsl.jore4.mapmatching.model.NodeProximity
import fi.hsl.jore4.mapmatching.model.VehicleType
import fi.hsl.jore4.mapmatching.model.matching.RouteJunctionPoint
import fi.hsl.jore4.mapmatching.model.matching.RoutePoint
import fi.hsl.jore4.mapmatching.model.matching.RouteStopPoint
import fi.hsl.jore4.mapmatching.model.matching.TerminusType
import fi.hsl.jore4.mapmatching.model.matching.TerminusType.END
import fi.hsl.jore4.mapmatching.model.matching.TerminusType.START
import fi.hsl.jore4.mapmatching.repository.infrastructure.ILinkRepository
import fi.hsl.jore4.mapmatching.repository.infrastructure.IStopRepository
import fi.hsl.jore4.mapmatching.repository.infrastructure.PublicTransportStopMatchParameters
import fi.hsl.jore4.mapmatching.repository.infrastructure.SnapPointToLinksDTO
import fi.hsl.jore4.mapmatching.repository.infrastructure.SnapStopToLinkDTO
import fi.hsl.jore4.mapmatching.repository.infrastructure.SnappedLinkState
import fi.hsl.jore4.mapmatching.repository.routing.BufferAreaRestriction
import fi.hsl.jore4.mapmatching.repository.routing.INodeRepository
import fi.hsl.jore4.mapmatching.repository.routing.RouteDTO
import fi.hsl.jore4.mapmatching.repository.routing.SnapPointToNodesDTO
import fi.hsl.jore4.mapmatching.service.common.IRoutingServiceInternal
import fi.hsl.jore4.mapmatching.service.common.response.RoutingResponse
import fi.hsl.jore4.mapmatching.service.common.response.RoutingResponseCreator
import fi.hsl.jore4.mapmatching.service.matching.MatchingServiceHelper.resolveTerminusLinkIfStopPoint
import fi.hsl.jore4.mapmatching.service.matching.MatchingServiceHelper.validateInputForRouteMatching
import fi.hsl.jore4.mapmatching.service.matching.PublicTransportRouteMatchingParameters.JunctionMatchingParameters
import fi.hsl.jore4.mapmatching.service.node.CreateNodeSequenceCombinations
import fi.hsl.jore4.mapmatching.service.node.INodeServiceInternal
import fi.hsl.jore4.mapmatching.service.node.NodeSequenceCandidatesBetweenSnappedLinks
import fi.hsl.jore4.mapmatching.service.node.NodeSequenceResolutionFailed
import fi.hsl.jore4.mapmatching.service.node.NodeSequenceResolutionResult
import fi.hsl.jore4.mapmatching.service.node.NodeSequenceResolutionSucceeded
import fi.hsl.jore4.mapmatching.service.node.VisitedNodes
import fi.hsl.jore4.mapmatching.service.node.VisitedNodesResolver
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
                                                 val linkRepository: ILinkRepository,
                                                 val nodeRepository: INodeRepository,
                                                 val nodeService: INodeServiceInternal,
                                                 val routingService: IRoutingServiceInternal)
    : IMatchingService {

    /**
     * Models infrastructure link candidates as the first/last link on a route to be matched.
     *
     * @property candidates the closest link snapped from route terminus point
     * @property terminusStopLocatedOnInfrastructureLinkId the identifier of the infrastructure link
     * along which the stop point at route terminus is located. The link is resolved from Digiroad
     * dataset by matching it with national identifier. Most often route starts from or ends at a
     * public transport stop. This property is used in sorting candidates; a link that "owns" the
     * public transport stop at terminus point is preferred over the others.
     */
    internal data class TerminusLinkCandidates(val candidates: List<SnappedLinkState>,
                                               val terminusStopLocatedOnInfrastructureLinkId: InfrastructureLinkId?)

    internal data class InfrastructureLinksOnRoute(val startLinkCandidates: TerminusLinkCandidates,
                                                   val endLinkCandidates: TerminusLinkCandidates,
                                                   val viaLinksIndexedByRoutePointOrdering: Map<Int, SnappedLinkState>)

    internal data class PreProcessingResult(val startLinkCandidates: TerminusLinkCandidates,
                                            val endLinkCandidates: TerminusLinkCandidates,
                                            val viaNodeHolders: List<Either<SnappedLinkState, NodeProximity>>)

    @Transactional(readOnly = true)
    override fun findMatchForPublicTransportRoute(routeGeometry: LineString<G2D>,
                                                  routePoints: List<RoutePoint>,
                                                  vehicleType: VehicleType,
                                                  matchingParameters: PublicTransportRouteMatchingParameters)
        : RoutingResponse {

        validateInputForRouteMatching(routePoints, vehicleType)?.let { validationError ->
            return RoutingResponse.invalidValue(validationError)
        }

        val (startLinkCandidates: TerminusLinkCandidates,
            endLinkCandidates: TerminusLinkCandidates,
            viaNodeResolvers: List<Either<SnappedLinkState, NodeProximity>>) = try {

            findTerminusLinkCandidatesAndViaNodes(routeGeometry,
                                                  routePoints,
                                                  vehicleType,
                                                  matchingParameters.terminusLinkQueryDistance,
                                                  matchingParameters.terminusLinkQueryLimit,
                                                  matchingParameters.maxStopLocationDeviation,
                                                  matchingParameters.roadJunctionMatching)
        } catch (ex: RuntimeException) {
            val errMessage: String = ex.message ?: "Could not resolve terminus link candidates and via nodes"
            LOGGER.warn(errMessage)
            return RoutingResponse.noSegment(errMessage)
        }

        val nodeSequenceCandidates: List<NodeSequenceCandidatesBetweenSnappedLinks> = try {
            resolveNodeSequenceCandidates(startLinkCandidates, endLinkCandidates, viaNodeResolvers)
        } catch (ex: RuntimeException) {
            val errMessage: String = ex.message ?: "Could not resolve node sequence candidates"
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

                val route: RouteDTO =
                    routingService.findRoute(nodeIdSequence,
                                             vehicleType,
                                             startLink.closestPointFractionalMeasure,
                                             endLink.closestPointFractionalMeasure,
                                             BufferAreaRestriction.from(routeGeometry,
                                                                        matchingParameters.bufferRadiusInMeters,
                                                                        startLink,
                                                                        endLink))

                RoutingResponseCreator.create(route)
            }
            is NodeSequenceResolutionFailed -> {
                LOGGER.warn(nodeSeqResult.message)
                RoutingResponse.noSegment(nodeSeqResult.message)
            }
        }
    }

    /**
     * @throws [IllegalStateException]
     */
    internal fun findTerminusLinkCandidatesAndViaNodes(routeGeometry: LineString<G2D>,
                                                       routePoints: List<RoutePoint>,
                                                       vehicleType: VehicleType,
                                                       terminusLinkQueryDistance: Double,
                                                       terminusLinkQueryLimit: Int,
                                                       maxStopLocationDeviation: Double,
                                                       junctionMatchingParams: JunctionMatchingParameters?)
        : PreProcessingResult {

        // Resolve infrastructure links to visit on route derived from the given geometry and route points.
        val (
            startLinkCandidates: TerminusLinkCandidates,
            endLinkCandidates: TerminusLinkCandidates,
            fromRouteStopPointIndexToInfrastructureLink: Map<Int, SnappedLinkState?>
        ) = findInfrastructureLinksOnRoute(routeGeometry,
                                           routePoints,
                                           vehicleType,
                                           terminusLinkQueryDistance,
                                           terminusLinkQueryLimit,
                                           maxStopLocationDeviation)

        // Resolve infrastructure network nodes to visit on route derived from the given route points.
        val fromRoutePointIndexToRoadJunctionNode: Map<Int, NodeProximity?> = junctionMatchingParams
            ?.let { getInfrastructureNodesByJunctionMatchingIndexedByRoutePointOrdering(routePoints, vehicleType, it) }
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

        return PreProcessingResult(startLinkCandidates, endLinkCandidates, viaNodeResolvers)
    }

    /**
     * @throws [IllegalStateException]
     */
    internal fun findInfrastructureLinksOnRoute(routeGeometry: LineString<G2D>,
                                                routePoints: List<RoutePoint>,
                                                vehicleType: VehicleType,
                                                terminusLinkQueryDistance: Double,
                                                terminusLinkQueryLimit: Int,
                                                maxStopLocationDeviation: Double)
        : InfrastructureLinksOnRoute {

        val fromRoutePointIndexToStopMatchParams: Map<Int, PublicTransportStopMatchParameters> = routePoints
            .mapIndexedNotNull { index: Int, routePoint: RoutePoint ->
                when (routePoint) {
                    is RouteStopPoint -> routePoint.nationalId?.let { nationalId ->

                        // Prefer projected location because it is expected to be closer to
                        // public transport stop location when compared to Digiroad locations.
                        val sourceLocation: Point<G2D> = routePoint.projectedLocation ?: routePoint.location

                        index to PublicTransportStopMatchParameters(nationalId, sourceLocation)
                    }
                    else -> null
                }
            }
            .toMap()

        val stopSearchParams: Collection<PublicTransportStopMatchParameters> =
            fromRoutePointIndexToStopMatchParams.values

        val snappedLinksFromStops: List<SnapStopToLinkDTO> =
            stopRepository.findStopsAndSnapToInfrastructureLinks(stopSearchParams, maxStopLocationDeviation)

        val fromStopNationalIdToInfrastructureLink: Map<Int, SnappedLinkState> =
            snappedLinksFromStops.associateBy(SnapStopToLinkDTO::stopNationalId, SnapStopToLinkDTO::link)

        val fromRoutePointIndexToMatchedStopNationalId: Map<Int, Int> = fromRoutePointIndexToStopMatchParams
            .mapValues { mapEntry -> mapEntry.value.nationalId }
            .filterValues(fromStopNationalIdToInfrastructureLink::containsKey)

        LOGGER.debug {
            "Matched following stop points from route points: ${
                joinToLogString(fromRoutePointIndexToMatchedStopNationalId.toSortedMap().entries) {
                    "Route point #${it.key + 1}: nationalId=${it.value}"
                }
            }"
        }

        fun getRouteTerminusPoint(routePoint: RoutePoint, terminusType: TerminusType): RouteTerminusPoint {
            val location: Point<G2D> =
                toPoint(if (terminusType == START) routeGeometry.startPosition else routeGeometry.endPosition)

            return when (routePoint) {
                is RouteStopPoint -> RouteTerminusPoint(location, terminusType, true, routePoint.nationalId)
                else -> RouteTerminusPoint(location, terminusType, false, null)
            }
        }

        val (startLinkCandidates: TerminusLinkCandidates, endLinkCandidates: TerminusLinkCandidates) =
            findTerminusLinkCandidates(getRouteTerminusPoint(routePoints.first(), START),
                                       getRouteTerminusPoint(routePoints.last(), END),
                                       terminusLinkQueryDistance,
                                       terminusLinkQueryLimit,
                                       vehicleType,
                                       fromStopNationalIdToInfrastructureLink)

        val fromRouteStopPointIndexToInfrastructureLink: Map<Int, SnappedLinkState> =
            fromRoutePointIndexToMatchedStopNationalId.mapValues { (_, stopNationalId: Int) ->
                fromStopNationalIdToInfrastructureLink[stopNationalId]!!
            }

        return InfrastructureLinksOnRoute(startLinkCandidates,
                                          endLinkCandidates,
                                          fromRouteStopPointIndexToInfrastructureLink)
    }

    /**
     * @throws [IllegalStateException]
     */
    internal fun findTerminusLinkCandidates(routeStartPoint: RouteTerminusPoint,
                                            routeEndPoint: RouteTerminusPoint,
                                            linkQueryDistance: Double,
                                            linkQueryLimit: Int,
                                            vehicleType: VehicleType,
                                            fromStopNationalIdToInfrastructureLink: Map<Int, SnappedLinkState>)
        : Pair<TerminusLinkCandidates, TerminusLinkCandidates> {

        val routeStartLocation: Point<G2D> = routeStartPoint.location
        val routeEndLocation: Point<G2D> = routeEndPoint.location

        // `findNClosestLinks` returns one-based index.
        // The number of closest links considered as terminus link candidates is limited by `linkQueryLimit`.
        val linkSearchResults: Map<Int, SnapPointToLinksDTO> =
            linkRepository.findNClosestLinks(listOf(routeStartLocation, routeEndLocation),
                                             vehicleType,
                                             linkQueryDistance,
                                             linkQueryLimit)

        fun getExceptionIfCandidatesNotFound(routeTerminusPoint: RouteTerminusPoint) =
            IllegalStateException(
                "Could not find infrastructure links within $linkQueryDistance meter distance from route " +
                    "${routeTerminusPoint.terminusType} point (${routeTerminusPoint.location}) while applying " +
                    "vehicle type constraint '$vehicleType'")

        val startLinkCandidates: List<SnappedLinkState> = linkSearchResults[1]?.closestLinks
            ?: throw getExceptionIfCandidatesNotFound(routeStartPoint)

        val endLinkCandidates: List<SnappedLinkState> = linkSearchResults[2]?.closestLinks
            ?: throw getExceptionIfCandidatesNotFound(routeEndPoint)

        fun createTerminusLinkCandidates(linkCandidates: List<SnappedLinkState>,
                                         routeTerminusPoint: RouteTerminusPoint)
            : TerminusLinkCandidates {

            val trimmedLinkCandidates: List<SnappedLinkState> = linkCandidates.map {
                // Move snap point to link endpoint if within threshold distance.
                it.withSnappedToTerminusNode(Constants.SNAP_TO_LINK_ENDPOINT_DISTANCE_IN_METERS)
            }

            val infrastructureLinkIdOfStopPoint: InfrastructureLinkId? =
                resolveTerminusLinkIfStopPoint(routeTerminusPoint, fromStopNationalIdToInfrastructureLink)
                    ?.infrastructureLinkId

            return TerminusLinkCandidates(trimmedLinkCandidates, infrastructureLinkIdOfStopPoint)
        }

        return Pair(createTerminusLinkCandidates(startLinkCandidates, routeStartPoint),
                    createTerminusLinkCandidates(endLinkCandidates, routeEndPoint))
    }

    internal fun getInfrastructureNodesByJunctionMatchingIndexedByRoutePointOrdering(
        routePoints: List<RoutePoint>,
        vehicleType: VehicleType,
        roadJunctionMatching: JunctionMatchingParameters): Map<Int, NodeProximity?> {

        val matchDistance: Double = roadJunctionMatching.junctionNodeMatchDistance
        val clearingDistance: Double = roadJunctionMatching.junctionNodeClearingDistance

        val junctionPointsWithRoutePointOrdering: List<IndexedValue<RoutePoint>> = routePoints
            .withIndex()
            .filter { it.value is RouteJunctionPoint }

        val fromJunctionPointOneBasedIndexToRoutePointIndex: Map<Int, Int> = junctionPointsWithRoutePointOrdering
            .map(IndexedValue<*>::index)
            .withIndex()
            .associateBy(keySelector = { it.index + 1 }, valueTransform = IndexedValue<Int>::value)

        val pointCoordinates: List<Point<G2D>> = junctionPointsWithRoutePointOrdering.map { it.value.location }

        val nClosestNodes: Map<Int, SnapPointToNodesDTO> = nodeRepository.findNClosestNodes(pointCoordinates,
                                                                                            vehicleType,
                                                                                            clearingDistance)

        return nClosestNodes.entries
            .mapNotNull { (junctionPointOneBasedIndex: Int, snap: SnapPointToNodesDTO) ->

                val routePointIndex: Int = fromJunctionPointOneBasedIndexToRoutePointIndex[junctionPointOneBasedIndex]!!

                val nodes: List<NodeProximity> = snap.nodes

                when (nodes.size) {
                    1 -> {
                        val node: NodeProximity = nodes[0]

                        if (node.distanceToNode <= matchDistance)
                            routePointIndex to node
                        else
                            null
                    }
                    else -> null
                }
            }
            .also { routePointIndexToJunctionNode: List<Pair<Int, NodeProximity>> ->
                LOGGER.debug {
                    "Matched following road junction points from route points: ${
                        joinToLogString(routePointIndexToJunctionNode) {
                            "Route point #${it.first + 1}: ${it.second}"
                        }
                    }"
                }
            }
            .toMap()
    }

    // Returns sorted list of node sequence candidates.
    internal fun resolveNodeSequenceCandidates(startLinkCandidates: TerminusLinkCandidates,
                                               endLinkCandidates: TerminusLinkCandidates,
                                               viaNodeHolders: List<Either<SnappedLinkState, NodeProximity>>)
        : List<NodeSequenceCandidatesBetweenSnappedLinks> {

        val viaNodeIds: List<InfrastructureNodeId> = viaNodeHolders.map { either ->
            either.fold(HasInfrastructureNodeId::getInfrastructureNodeId,
                        HasInfrastructureNodeId::getInfrastructureNodeId)
        }

        val hashCodesOfNodeSequences: MutableSet<Int> = mutableSetOf()

        return startLinkCandidates.candidates
            .flatMap { startLink ->
                endLinkCandidates.candidates.mapNotNull { endLink ->

                    val nodesToVisit: VisitedNodes = VisitedNodesResolver.resolve(startLink, viaNodeIds, endLink)

                    // Filter out duplicate node sequences that are already encountered within
                    // previously processed pairs of terminus links. Duplicates may exist in case
                    // multiple links are snapped to their endpoint nodes. This optimisation
                    // eventually avoids unnecessary expensive SQL queries.

                    val nodeSequenceCombinations: List<NodeIdSequence> = CreateNodeSequenceCombinations
                        .create(nodesToVisit)
                        .filter { nodeIdSeq: NodeIdSequence ->
                            // Add hashcode of node sequence into set as side effect. Return false
                            // if the hashcode already exists in the set.
                            hashCodesOfNodeSequences.add(nodeIdSeq.list.hashCode())
                        }

                    if (nodeSequenceCombinations.isNotEmpty())
                        NodeSequenceCandidatesBetweenSnappedLinks(startLink,
                                                                  endLink,
                                                                  nodeSequenceCombinations)
                    else
                        null
                }
            }
            .filter(NodeSequenceCandidatesBetweenSnappedLinks::isRoutePossible)
            .sortedWith { choice1, choice2 ->
                // Sort node sequence candidates.

                fun getNumberOfTerminusStopsAlongLinks(choice: NodeSequenceCandidatesBetweenSnappedLinks): Int {
                    // Indicates whether the source route's start point (in case it is a stop point)
                    // is along the start link of this terminus link pair.
                    val isRouteStartStopAlong: Boolean = startLinkCandidates.terminusStopLocatedOnInfrastructureLinkId
                        ?.let { it == choice.startLink.infrastructureLinkId }
                        ?: false

                    // Indicates whether the source route's end point (in case it is a stop point)
                    // is along the end link of this terminus link pair.
                    val isRouteEndStopAlong: Boolean = endLinkCandidates.terminusStopLocatedOnInfrastructureLinkId
                        ?.let { it == choice.endLink.infrastructureLinkId }
                        ?: false

                    // Deduce amount of stops (along terminus links).
                    return if (isRouteStartStopAlong) {
                        if (isRouteEndStopAlong) 2 else 1
                    } else if (isRouteEndStopAlong) 1
                    else 0
                }

                val stopCount1: Int = getNumberOfTerminusStopsAlongLinks(choice1)
                val stopCount2: Int = getNumberOfTerminusStopsAlongLinks(choice2)

                // Prioritise terminus link pairs that are associated with public transport stops (sorted first).
                // The stops are matched with national ID from source route's first and last route point.

                if (stopCount1 > stopCount2)
                    -1
                else if (stopCount1 < stopCount2)
                    1
                else
                    choice1.compareTo(choice2) // delegate to closest distance comparison
            }
    }
}
