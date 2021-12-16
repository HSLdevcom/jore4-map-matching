package fi.hsl.jore4.mapmatching.service.matching

import fi.hsl.jore4.mapmatching.model.HasInfrastructureNodeId
import fi.hsl.jore4.mapmatching.model.InfrastructureLinkId
import fi.hsl.jore4.mapmatching.model.InfrastructureNodeId
import fi.hsl.jore4.mapmatching.model.NodeIdSequence
import fi.hsl.jore4.mapmatching.model.NodeProximity
import fi.hsl.jore4.mapmatching.model.PathTraversal
import fi.hsl.jore4.mapmatching.model.VehicleType
import fi.hsl.jore4.mapmatching.model.matching.RoutePoint
import fi.hsl.jore4.mapmatching.model.matching.RoutePointType.PUBLIC_TRANSPORT_STOP
import fi.hsl.jore4.mapmatching.model.matching.TerminusType.END
import fi.hsl.jore4.mapmatching.model.matching.TerminusType.START
import fi.hsl.jore4.mapmatching.model.tables.records.InfrastructureLinkRecord
import fi.hsl.jore4.mapmatching.model.tables.records.PublicTransportStopRecord
import fi.hsl.jore4.mapmatching.repository.infrastructure.ILinkRepository
import fi.hsl.jore4.mapmatching.repository.infrastructure.IStopRepository
import fi.hsl.jore4.mapmatching.repository.infrastructure.SnapPointToLinkDTO
import fi.hsl.jore4.mapmatching.repository.infrastructure.SnappedLinkState
import fi.hsl.jore4.mapmatching.repository.routing.BufferAreaRestriction
import fi.hsl.jore4.mapmatching.service.common.IRoutingServiceInternal
import fi.hsl.jore4.mapmatching.service.common.response.RoutingResponse
import fi.hsl.jore4.mapmatching.service.common.response.RoutingResponseCreator
import fi.hsl.jore4.mapmatching.service.matching.MatchingServiceHelper.getTerminusLinkOrThrowException
import fi.hsl.jore4.mapmatching.service.matching.MatchingServiceHelper.resolveTerminusLinkIfStopPoint
import fi.hsl.jore4.mapmatching.service.matching.MatchingServiceHelper.validateInputForRouteMatching
import fi.hsl.jore4.mapmatching.service.node.INodeServiceInternal
import fi.hsl.jore4.mapmatching.service.node.NodeSequenceAlternatives
import fi.hsl.jore4.mapmatching.service.node.NodeSequenceAlternativesCreator
import fi.hsl.jore4.mapmatching.util.LogUtils.joinToLogString
import org.geolatte.geom.C2D
import org.geolatte.geom.G2D
import org.geolatte.geom.LineString
import org.geolatte.geom.Point
import org.geolatte.geom.ProjectedGeometryOperations
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class MatchingServiceImpl @Autowired constructor(val stopRepository: IStopRepository,
                                                 val linkRepository: ILinkRepository,
                                                 val nodeService: INodeServiceInternal,
                                                 val routingService: IRoutingServiceInternal)
    : IMatchingService {

    data class InfrastructureLinksOnRoute(val startLink: SnappedLinkState,
                                          val endLink: SnappedLinkState,
                                          val linksIndexedByRoutePointOrdering: Map<Int, SnappedLinkState>)

    @Transactional(readOnly = true)
    override fun findMatchForPublicTransportRoute(routeGeometry: LineString<G2D>,
                                                  routePoints: List<RoutePoint>,
                                                  vehicleType: VehicleType,
                                                  matchingParameters: PublicTransportRouteMatchingParameters)
        : RoutingResponse {

        validateInputForRouteMatching(routePoints, vehicleType)?.let { validationError ->
            return RoutingResponse.invalidValue(validationError)
        }

        val nodeSequenceAlternatives: NodeSequenceAlternatives =
            resolveNodeSequenceAlternatives(routePoints,
                                            vehicleType,
                                            matchingParameters.terminusLinkQueryDistance)

        val bufferAreaRestriction = BufferAreaRestriction(routeGeometry,
                                                          matchingParameters.bufferRadiusInMeters,
                                                          nodeSequenceAlternatives.startLinkId,
                                                          nodeSequenceAlternatives.endLinkId)

        val nodeIdSeq: NodeIdSequence

        try {
            nodeIdSeq = resolveNetworkNodeIds(nodeSequenceAlternatives, vehicleType, bufferAreaRestriction)
        } catch (ex: RuntimeException) {
            val errMessage: String = ex.message ?: "Failure while resolving infrastructure network nodes"
            LOGGER.warn(errMessage)
            return RoutingResponse.noSegment(errMessage)
        }

        val traversedPaths: List<PathTraversal> =
            routingService.findRoute(nodeIdSeq, vehicleType, bufferAreaRestriction)

        return RoutingResponseCreator.create(traversedPaths)
    }

    /**
     * @throws [IllegalStateException]
     */
    internal fun resolveNodeSequenceAlternatives(routePoints: List<RoutePoint>,
                                                 vehicleType: VehicleType,
                                                 terminusLinkQueryDistance: Double)
        : NodeSequenceAlternatives {

        // Resolve infrastructure links to visit on route derived from the given route points.
        val (
            firstLink: SnappedLinkState,
            lastLink: SnappedLinkState,
            fromRoutePointIndexToInfrastructureLink: Map<Int, SnappedLinkState?>
        ) = getInfrastructureLinksOnRoute(routePoints,
                                          vehicleType,
                                          terminusLinkQueryDistance)

        val viaNodes: List<HasInfrastructureNodeId> = routePoints
            .withIndex()
            .drop(1)
            .dropLast(1)
            .mapNotNull { (routePointIndex: Int, routePoint: RoutePoint) ->

                val hasNodeId: HasInfrastructureNodeId? = when (routePoint.type) {
                    // Only public transport stops are mapped to infrastructure links.
                    PUBLIC_TRANSPORT_STOP -> fromRoutePointIndexToInfrastructureLink[routePointIndex]
                    else -> null
                }

                hasNodeId
            }

        return NodeSequenceAlternativesCreator.create(firstLink, viaNodes, lastLink)
    }

    /**
     * @throws [IllegalStateException]
     */
    internal fun getInfrastructureLinksOnRoute(routePoints: List<RoutePoint>,
                                               vehicleType: VehicleType,
                                               terminusLinkQueryDistance: Double)
        : InfrastructureLinksOnRoute {

        val fromRoutePointIndexToStopNationalId: Map<Int, Int> = routePoints
            .withIndex()
            .mapNotNull { (index: Int, routePoint: RoutePoint) ->
                if (routePoint.isStopPoint)
                    routePoint.stopPointInfo?.nationalId?.let { Pair(index, it) }
                else
                    null
            }
            .toMap()

        val foundStops: List<PublicTransportStopRecord> =
            stopRepository.findByNationalIds(fromRoutePointIndexToStopNationalId.values)

        val snappedLinks: List<SnapStopToLinkDTO> = findInfrastructureLinksAssociatedWithStops(foundStops)

        val fromStopNationalIdToInfrastructureLink: Map<Int, SnappedLinkState> =
            snappedLinks.associateBy(keySelector = { it.stopNationalId }, valueTransform = { it.link })

        val fromRoutePointIndexToMatchedStopNationalId: Map<Int, Int> = fromRoutePointIndexToStopNationalId
            .filterValues { fromStopNationalIdToInfrastructureLink[it] != null }

        if (LOGGER.isDebugEnabled) {
            LOGGER.debug("Matched following stop points from route points: {}",
                         joinToLogString(fromRoutePointIndexToMatchedStopNationalId.toSortedMap().entries) {
                             "Route point #${it.key + 1}: nationalId=${it.value}"
                         })
        }

        val fromRoutePointIndexToInfrastructureLink: Map<Int, SnappedLinkState> =
            fromRoutePointIndexToMatchedStopNationalId
                .mapValues { (_: Int, stopNationalId: Int) ->
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
            .map { it.locatedOnInfrastructureLinkId }
            .toSet()
            .map { InfrastructureLinkId(it) }

        val linkRecords: List<InfrastructureLinkRecord> = linkRepository.findByIds(linkIds)

        val linkDataById: Map<Long, Pair<InfrastructureLinkRecord, Double>> = linkRecords.associateBy(
            keySelector = { it.infrastructureLinkId },
            valueTransform = { linkRecord ->
                val linkShape: LineString<C2D> = linkRecord.geom

                Pair(linkRecord,
                     ProjectedGeometryOperations.Default.length(linkShape))
            })

        return stops.mapNotNull { stop ->
            val linkId: Long = stop.locatedOnInfrastructureLinkId

            linkDataById[linkId]?.let { pair ->

                val (link: InfrastructureLinkRecord, linkLength: Double) = pair

                val distanceToStartNode: Double = stop.distanceFromLinkStartInMeters
                val distanceToEndNode: Double = linkLength - distanceToStartNode

                SnapStopToLinkDTO(stop.publicTransportStopNationalId,
                                  SnappedLinkState(
                                      InfrastructureLinkId(linkId),
                                      0.0, // closest distance from stop to link can be set to zero
                                      NodeProximity(InfrastructureNodeId(link.startNodeId), distanceToStartNode),
                                      NodeProximity(InfrastructureNodeId(link.endNodeId), distanceToEndNode)
                                  ))
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
            return Pair(snappedLinkFromStartStop, snappedLinkFromEndStop)
        }

        val routeStartLocation: Point<G2D> = routeStartPoint.location
        val routeEndLocation: Point<G2D> = routeEndPoint.location

        // linkSearchResults is one-based index
        val linkSearchResults: Map<Int, SnapPointToLinkDTO> =
            linkRepository.findClosestLinks(listOf(routeStartLocation, routeEndLocation),
                                            vehicleType,
                                            linkQueryDistance)

        val startLink: SnappedLinkState = snappedLinkFromStartStop ?: run {
            getTerminusLinkOrThrowException(linkSearchResults[1],
                                            START,
                                            routeStartLocation,
                                            vehicleType,
                                            linkQueryDistance)
        }

        val endLink: SnappedLinkState = snappedLinkFromEndStop ?: run {
            getTerminusLinkOrThrowException(linkSearchResults[2],
                                            END,
                                            routeEndLocation,
                                            vehicleType,
                                            linkQueryDistance)
        }

        return Pair(startLink, endLink)
    }

    /**
     * @throws [IllegalStateException]
     */
    internal fun resolveNetworkNodeIds(nodeSequenceAlternatives: NodeSequenceAlternatives,
                                       vehicleType: VehicleType,
                                       bufferAreaRestriction: BufferAreaRestriction)
        : NodeIdSequence {

        val nodeIdSeq: NodeIdSequence =
            nodeService.resolveNodeIdSequence(nodeSequenceAlternatives, vehicleType, bufferAreaRestriction)

        if (LOGGER.isDebugEnabled) {
            LOGGER.debug("Resolved params ${nodeSequenceAlternatives.toCompactString()} to $nodeIdSeq")
        }

        return nodeIdSeq
    }

    companion object {
        private val LOGGER: Logger = LoggerFactory.getLogger(MatchingServiceImpl::class.java)
    }
}
