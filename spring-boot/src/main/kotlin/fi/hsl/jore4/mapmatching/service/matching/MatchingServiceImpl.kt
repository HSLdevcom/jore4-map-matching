package fi.hsl.jore4.mapmatching.service.matching

import fi.hsl.jore4.mapmatching.model.HasInfrastructureNodeId
import fi.hsl.jore4.mapmatching.model.InfrastructureLinkId
import fi.hsl.jore4.mapmatching.model.InfrastructureNodeId
import fi.hsl.jore4.mapmatching.model.NodeIdSequence
import fi.hsl.jore4.mapmatching.model.NodeProximity
import fi.hsl.jore4.mapmatching.model.VehicleType
import fi.hsl.jore4.mapmatching.model.matching.RouteJunctionPoint
import fi.hsl.jore4.mapmatching.model.matching.RoutePoint
import fi.hsl.jore4.mapmatching.model.matching.RouteStopPoint
import fi.hsl.jore4.mapmatching.model.matching.TerminusType.END
import fi.hsl.jore4.mapmatching.model.matching.TerminusType.START
import fi.hsl.jore4.mapmatching.model.tables.records.InfrastructureLinkRecord
import fi.hsl.jore4.mapmatching.model.tables.records.PublicTransportStopRecord
import fi.hsl.jore4.mapmatching.repository.infrastructure.ILinkRepository
import fi.hsl.jore4.mapmatching.repository.infrastructure.IStopRepository
import fi.hsl.jore4.mapmatching.repository.infrastructure.SnapPointToLinkDTO
import fi.hsl.jore4.mapmatching.repository.infrastructure.SnappedLinkState
import fi.hsl.jore4.mapmatching.repository.routing.BufferAreaRestriction
import fi.hsl.jore4.mapmatching.repository.routing.INodeRepository
import fi.hsl.jore4.mapmatching.repository.routing.RouteDTO
import fi.hsl.jore4.mapmatching.repository.routing.SnapPointToNodesDTO
import fi.hsl.jore4.mapmatching.service.common.IRoutingServiceInternal
import fi.hsl.jore4.mapmatching.service.common.response.RoutingResponse
import fi.hsl.jore4.mapmatching.service.common.response.RoutingResponseCreator
import fi.hsl.jore4.mapmatching.service.matching.MatchingServiceHelper.getTerminusLinkOrThrowException
import fi.hsl.jore4.mapmatching.service.matching.MatchingServiceHelper.resolveTerminusLinkIfStopPoint
import fi.hsl.jore4.mapmatching.service.matching.MatchingServiceHelper.validateInputForRouteMatching
import fi.hsl.jore4.mapmatching.service.matching.PublicTransportRouteMatchingParameters.JunctionMatchingParameters
import fi.hsl.jore4.mapmatching.service.node.INodeServiceInternal
import fi.hsl.jore4.mapmatching.service.node.NodeSequenceAlternatives
import fi.hsl.jore4.mapmatching.service.node.NodeSequenceAlternativesCreator
import fi.hsl.jore4.mapmatching.service.node.VisitedNodes
import fi.hsl.jore4.mapmatching.service.node.VisitedNodesResolver
import fi.hsl.jore4.mapmatching.util.LogUtils.joinToLogString
import fi.hsl.jore4.mapmatching.util.MathUtils.isWithinTolerance
import mu.KotlinLogging
import org.geolatte.geom.G2D
import org.geolatte.geom.LineString
import org.geolatte.geom.Point
import org.geolatte.geom.jts.JTS
import org.locationtech.jts.linearref.LengthIndexedLine
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import kotlin.math.max

private val LOGGER = KotlinLogging.logger {}

@Service
class MatchingServiceImpl @Autowired constructor(val stopRepository: IStopRepository,
                                                 val linkRepository: ILinkRepository,
                                                 val nodeRepository: INodeRepository,
                                                 val nodeService: INodeServiceInternal,
                                                 val routingService: IRoutingServiceInternal)
    : IMatchingService {

    internal data class InfrastructureLinksOnRoute(val startLink: SnappedLinkState,
                                                   val endLink: SnappedLinkState,
                                                   val linksIndexedByRoutePointOrdering: Map<Int, SnappedLinkState>)

    internal data class PreProcessingResult(val startLink: SnappedLinkState,
                                            val endLink: SnappedLinkState,
                                            val nodeSequenceAlternatives: NodeSequenceAlternatives)

    @Transactional(readOnly = true)
    override fun findMatchForPublicTransportRoute(routeGeometry: LineString<G2D>,
                                                  routePoints: List<RoutePoint>,
                                                  vehicleType: VehicleType,
                                                  matchingParameters: PublicTransportRouteMatchingParameters)
        : RoutingResponse {

        validateInputForRouteMatching(routePoints, vehicleType)?.let { validationError ->
            return RoutingResponse.invalidValue(validationError)
        }

        val (startLink: SnappedLinkState,
            endLink: SnappedLinkState,
            nodeSequenceAlternatives: NodeSequenceAlternatives) = try {

            findTerminusLinksAndNodeSequenceAlternatives(routePoints,
                                                         vehicleType,
                                                         matchingParameters.terminusLinkQueryDistance,
                                                         matchingParameters.roadJunctionMatching)
        } catch (ex: RuntimeException) {
            val errMessage: String = ex.message ?: "Could not resolve node sequence alternatives"
            LOGGER.warn(errMessage)
            return RoutingResponse.noSegment(errMessage)
        }

        if (!nodeSequenceAlternatives.isRoutePossible()) {
            return RoutingResponse.noSegment("Cannot produce route based on single infrastructure node")
        }

        val bufferAreaRestriction = BufferAreaRestriction(routeGeometry,
                                                          matchingParameters.bufferRadiusInMeters,
                                                          startLink.infrastructureLinkId,
                                                          endLink.infrastructureLinkId)

        val nodeIdSeq: NodeIdSequence = try {
            resolveNetworkNodeIds(nodeSequenceAlternatives, vehicleType, bufferAreaRestriction)
        } catch (ex: RuntimeException) {
            val errMessage: String = ex.message ?: "Failure while resolving infrastructure network nodes"
            LOGGER.warn(errMessage)
            return RoutingResponse.noSegment(errMessage)
        }

        val route: RouteDTO = routingService.findRoute(nodeIdSeq,
                                                       vehicleType,
                                                       startLink.closestPointFractionalMeasure,
                                                       endLink.closestPointFractionalMeasure,
                                                       bufferAreaRestriction)

        return RoutingResponseCreator.create(route)
    }

    /**
     * @throws [IllegalStateException] in case a sequence of infrastructure node identifiers could not be resolved
     */
    internal fun findTerminusLinksAndNodeSequenceAlternatives(routePoints: List<RoutePoint>,
                                                              vehicleType: VehicleType,
                                                              terminusLinkQueryDistance: Double,
                                                              junctionMatchingParams: JunctionMatchingParameters?)
        : PreProcessingResult {

        // Resolve infrastructure links to visit on route derived from the given route points.
        val (
            firstLink: SnappedLinkState,
            lastLink: SnappedLinkState,
            fromRoutePointIndexToInfrastructureLink: Map<Int, SnappedLinkState?>
        ) = findInfrastructureLinksOnRoute(routePoints,
                                           vehicleType,
                                           terminusLinkQueryDistance)

        // Resolve infrastructure network nodes to visit on route derived from the given route points.
        val fromRoutePointIndexToRoadJunctionNode: Map<Int, NodeProximity?> = junctionMatchingParams
            ?.let { getInfrastructureNodesByJunctionMatchingIndexedByRoutePointOrdering(routePoints, vehicleType, it) }
            ?: emptyMap()

        val viaNodeIds: List<InfrastructureNodeId> = routePoints
            .withIndex()
            .drop(1)
            .dropLast(1)
            .mapNotNull { (routePointIndex: Int, routePoint: RoutePoint) ->

                when (routePoint) {
                    is RouteStopPoint -> fromRoutePointIndexToInfrastructureLink[routePointIndex]
                    is RouteJunctionPoint -> fromRoutePointIndexToRoadJunctionNode[routePointIndex]
                    else -> null
                }
            }
            .map(HasInfrastructureNodeId::getInfrastructureNodeId)

        val nodesToVisit: VisitedNodes = VisitedNodesResolver.resolve(firstLink, viaNodeIds, lastLink)

        return PreProcessingResult(firstLink,
                                   lastLink,
                                   NodeSequenceAlternativesCreator.create(nodesToVisit))
    }

    /**
     * @throws [IllegalStateException]
     */
    internal fun findInfrastructureLinksOnRoute(routePoints: List<RoutePoint>,
                                                vehicleType: VehicleType,
                                                terminusLinkQueryDistance: Double)
        : InfrastructureLinksOnRoute {

        val fromRoutePointIndexToStopNationalId: Map<Int, Int> = routePoints
            .mapIndexedNotNull { index: Int, routePoint: RoutePoint ->
                when (routePoint) {
                    is RouteStopPoint -> routePoint.nationalId?.let { nationalId -> index to nationalId }
                    else -> null
                }
            }
            .toMap()

        val foundStops: List<PublicTransportStopRecord> =
            stopRepository.findByNationalIds(fromRoutePointIndexToStopNationalId.values)

        val snappedLinks: List<SnapStopToLinkDTO> = findInfrastructureLinksAssociatedWithStops(foundStops)

        val fromStopNationalIdToInfrastructureLink: Map<Int, SnappedLinkState> =
            snappedLinks.associateBy(SnapStopToLinkDTO::stopNationalId, SnapStopToLinkDTO::link)

        val fromRoutePointIndexToMatchedStopNationalId: Map<Int, Int> = fromRoutePointIndexToStopNationalId
            .filterValues(fromStopNationalIdToInfrastructureLink::containsKey)

        LOGGER.debug {
            "Matched following stop points from route points: ${
                joinToLogString(fromRoutePointIndexToMatchedStopNationalId.toSortedMap().entries) {
                    "Route point #${it.key + 1}: nationalId=${it.value}"
                }
            }"
        }

        val fromRoutePointIndexToInfrastructureLink: Map<Int, SnappedLinkState> =
            fromRoutePointIndexToMatchedStopNationalId.mapValues { (_, stopNationalId: Int) ->
                fromStopNationalIdToInfrastructureLink[stopNationalId]!!
            }

        val (startLink: SnappedLinkState, endLink: SnappedLinkState) =
            resolveTerminusLinksOfRoute(routePoints.first(),
                                        routePoints.last(),
                                        fromStopNationalIdToInfrastructureLink,
                                        vehicleType,
                                        terminusLinkQueryDistance)

        return InfrastructureLinksOnRoute(startLink, endLink, fromRoutePointIndexToInfrastructureLink)
    }

    internal fun findInfrastructureLinksAssociatedWithStops(stops: List<PublicTransportStopRecord>)
        : List<SnapStopToLinkDTO> {

        val linkIds: List<InfrastructureLinkId> = stops
            .map(PublicTransportStopRecord::getLocatedOnInfrastructureLinkId)
            .distinct() // remove duplicates
            .map(::InfrastructureLinkId)

        val linkRecordById: Map<Long, InfrastructureLinkRecord> = linkRepository
            .findByIds(linkIds)
            .associateBy(InfrastructureLinkRecord::getInfrastructureLinkId)

        return stops.mapNotNull { stop ->
            val linkId: Long = stop.locatedOnInfrastructureLinkId

            linkRecordById[linkId]?.let { linkRecord: InfrastructureLinkRecord ->

                val stopDistFromStart: Double = stop.distanceFromLinkStartInMeters
                val linkLength: Double = max(linkRecord.cost, linkRecord.reverseCost)

                val stopPointFractionalMeasureOnLink: Double? = when {
                    linkLength.isWithinTolerance(0.0) -> {
                        // Avoid division by zero. Length of infrastructure link should never be zero.
                        null
                    }
                    stopDistFromStart <= linkLength -> stopDistFromStart / linkLength
                    else -> {
                        val linkGeom: org.locationtech.jts.geom.LineString = JTS.to(linkRecord.geom)
                        val stopGeom: org.locationtech.jts.geom.Point = JTS.to(stop.geom)

                        // Calculate fractional location of Point on LineString using JTS.
                        val frac: Double = LengthIndexedLine(linkGeom).indexOf(stopGeom.coordinate)

                        LOGGER.warn(
                            "Distance of public transport stop (ID={}) from the start of the infrastructure link the " +
                                "stop is located along (as taken from database) is greater than the length of the " +
                                "link (ID={}): {} > {}. Calculated fractional measure manually: {}",
                            stop.publicTransportStopId, linkId, stopDistFromStart, linkLength, frac)

                        // Discard possible outliers. A too great distance between stop point and infrastructure link
                        // might cause erroneous numbers.
                        // TODO It will discovered later whether clamping values greater than 1.0 to 1.0 is beneficial.
                        frac.takeIf { it in 0.0..1.0 }
                    }
                }

                stopPointFractionalMeasureOnLink?.let { stopLocationOnLinkAsFraction ->
                    SnapStopToLinkDTO(stop.publicTransportStopNationalId,
                                      SnappedLinkState(
                                          InfrastructureLinkId(linkId),
                                          0.0, // closest distance from stop to link can be set to zero
                                          stopLocationOnLinkAsFraction,
                                          linkRecord.trafficFlowDirectionType,
                                          linkLength,
                                          InfrastructureNodeId(linkRecord.startNodeId),
                                          InfrastructureNodeId(linkRecord.endNodeId)))
                }
            }
        }
    }

    /**
     * @throws [IllegalStateException]
     */
    internal fun resolveTerminusLinksOfRoute(routeStartPoint: RoutePoint,
                                             routeEndPoint: RoutePoint,
                                             fromStopNationalIdToInfrastructureLink: Map<Int, SnappedLinkState>,
                                             vehicleType: VehicleType,
                                             linkQueryDistance: Double)
        : Pair<SnappedLinkState, SnappedLinkState> {

        val snappedLinkFromStartStop: SnappedLinkState? =
            resolveTerminusLinkIfStopPoint(routeStartPoint, START, fromStopNationalIdToInfrastructureLink)

        val snappedLinkFromEndStop: SnappedLinkState? =
            resolveTerminusLinkIfStopPoint(routeEndPoint, END, fromStopNationalIdToInfrastructureLink)

        if (snappedLinkFromStartStop != null && snappedLinkFromEndStop != null) {
            return snappedLinkFromStartStop to snappedLinkFromEndStop
        }

        val routeStartLocation: Point<G2D> = routeStartPoint.location
        val routeEndLocation: Point<G2D> = routeEndPoint.location

        // linkSearchResults is one-based index
        val linkSearchResults: Map<Int, SnapPointToLinkDTO> =
            linkRepository.findClosestLinks(listOf(routeStartLocation, routeEndLocation),
                                            vehicleType,
                                            linkQueryDistance)

        val startLink: SnappedLinkState =
            snappedLinkFromStartStop ?: getTerminusLinkOrThrowException(linkSearchResults[1],
                                                                        START,
                                                                        routeStartLocation,
                                                                        vehicleType,
                                                                        linkQueryDistance)

        val endLink: SnappedLinkState =
            snappedLinkFromEndStop ?: getTerminusLinkOrThrowException(linkSearchResults[2],
                                                                      END,
                                                                      routeEndLocation,
                                                                      vehicleType,
                                                                      linkQueryDistance)

        return startLink to endLink
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

    /**
     * @throws [IllegalStateException]
     */
    internal fun resolveNetworkNodeIds(nodeSequenceAlternatives: NodeSequenceAlternatives,
                                       vehicleType: VehicleType,
                                       bufferAreaRestriction: BufferAreaRestriction)
        : NodeIdSequence {

        return nodeService
            .resolveNodeIdSequence(nodeSequenceAlternatives, vehicleType, bufferAreaRestriction)
            .also { nodeIdSeq: NodeIdSequence ->
                LOGGER.debug {
                    "Resolved node resolution params ${nodeSequenceAlternatives.prettyPrint()} to nodes $nodeIdSeq"
                }
            }
    }
}
