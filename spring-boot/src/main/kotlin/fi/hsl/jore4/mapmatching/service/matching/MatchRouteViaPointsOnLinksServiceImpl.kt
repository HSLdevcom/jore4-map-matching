package fi.hsl.jore4.mapmatching.service.matching

import fi.hsl.jore4.mapmatching.Constants
import fi.hsl.jore4.mapmatching.model.LinkSide
import fi.hsl.jore4.mapmatching.model.NodeProximity
import fi.hsl.jore4.mapmatching.model.VehicleType
import fi.hsl.jore4.mapmatching.model.matching.RouteJunctionPoint
import fi.hsl.jore4.mapmatching.model.matching.RoutePoint
import fi.hsl.jore4.mapmatching.model.matching.RouteStopPoint
import fi.hsl.jore4.mapmatching.repository.infrastructure.SnapStopToLinkResult
import fi.hsl.jore4.mapmatching.repository.infrastructure.SnappedPointOnLink
import fi.hsl.jore4.mapmatching.repository.routing.BufferAreaRestriction
import fi.hsl.jore4.mapmatching.repository.routing.PgRoutingPoint
import fi.hsl.jore4.mapmatching.repository.routing.RealNode
import fi.hsl.jore4.mapmatching.repository.routing.RouteLink
import fi.hsl.jore4.mapmatching.service.common.IRoutingServiceInternal
import fi.hsl.jore4.mapmatching.service.common.response.RoutingResponse
import fi.hsl.jore4.mapmatching.service.common.response.RoutingResponseCreator
import fi.hsl.jore4.mapmatching.service.matching.MatchingServiceHelper.getSourceRouteTerminusPoint
import fi.hsl.jore4.mapmatching.util.GeolatteUtils.toPoint
import fi.hsl.jore4.mapmatching.util.InternalService
import fi.hsl.jore4.mapmatching.util.LogUtils.joinToLogString
import io.github.oshai.kotlinlogging.KotlinLogging
import org.geolatte.geom.G2D
import org.geolatte.geom.LineString
import org.geolatte.geom.Point
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.transaction.annotation.Transactional

private val LOGGER = KotlinLogging.logger {}

@InternalService
class MatchRouteViaPointsOnLinksServiceImpl
    @Autowired
    constructor(
        val closestTerminusLinksResolver: IClosestTerminusLinksResolver,
        val publicTransportStopMatcher: IPublicTransportStopMatcher,
        val roadJunctionMatcher: IRoadJunctionMatcher,
        val routingService: IRoutingServiceInternal,
    ) : IMatchRouteViaPointsOnLinksService {
        internal data class TerminusPointCandidatesAndStopPoints(
            val targetStartPointCandidates: List<TerminusPointCandidate>,
            val targetEndPointCandidates: List<TerminusPointCandidate>,
            val targetStopPointsIndexedByRoutePointOrdering: Map<Int, PgRoutingPoint>,
        )

        @Transactional(readOnly = true, noRollbackFor = [RuntimeException::class])
        override fun findMatchForPublicTransportRoute(
            sourceRouteGeometry: LineString<G2D>,
            sourceRoutePoints: List<RoutePoint>,
            vehicleType: VehicleType,
            matchingParameters: PublicTransportRouteMatchingParameters,
        ): RoutingResponse {
            val terminusLinkSelectionInput: TerminusLinkSelectionInput =
                try {
                    resolveTerminusLinkSelectionInput(
                        sourceRouteGeometry,
                        sourceRoutePoints,
                        vehicleType,
                        matchingParameters.terminusLinkQueryDistance,
                        matchingParameters.terminusLinkQueryLimit,
                    )
                } catch (ex: RuntimeException) {
                    val errMessage: String =
                        ex.message ?: "Failed to find closest terminus links on either end of route"
                    return RoutingResponse.noSegment(errMessage)
                }

            val targetRoutePointSequenceCandidates: List<List<PgRoutingPoint>> =
                resolveRoutePointSequenceCandidates(
                    sourceRoutePoints,
                    vehicleType,
                    terminusLinkSelectionInput,
                    matchingParameters,
                )

            return findFirstMatchingRouteOrNull(
                sourceRouteGeometry,
                vehicleType,
                targetRoutePointSequenceCandidates,
                matchingParameters.bufferRadiusInMeters,
            )?.let { routeLinks: List<RouteLink> ->
                LOGGER.debug { "Got route links: ${joinToLogString(routeLinks)}" }
                RoutingResponseCreator.create(routeLinks)
            }
                ?: RoutingResponse.noSegment(
                    "Could not find route while map-matching via graph edges (points on links)",
                )
        }

        /**
         * @throws [IllegalStateException] if no links are found for one or both of the two endpoints
         * of the route
         */
        internal fun resolveTerminusLinkSelectionInput(
            sourceRouteGeometry: LineString<G2D>,
            sourceRoutePoints: List<RoutePoint>,
            vehicleType: VehicleType,
            terminusLinkQueryDistance: Double,
            terminusLinkQueryLimit: Int,
        ): TerminusLinkSelectionInput {
            // The terminus locations are extracted from the LineString geometry of the source route
            // instead of the route point entities (mostly stop point instances) since in this context
            // we are interested in the start/end coordinates of the source route line.
            val startLocation: Point<G2D> = toPoint(sourceRouteGeometry.startPosition)
            val endLocation: Point<G2D> = toPoint(sourceRouteGeometry.endPosition)

            val (closestStartLinks: List<SnappedPointOnLink>, closestEndLinks: List<SnappedPointOnLink>) =
                closestTerminusLinksResolver.findClosestInfrastructureLinksForRouteEndpoints(
                    startLocation,
                    endLocation,
                    vehicleType,
                    terminusLinkQueryDistance,
                    terminusLinkQueryLimit,
                )

            fun snapToTerminusNodes(pointsOnLinks: List<SnappedPointOnLink>): List<SnappedPointOnLink> =
                pointsOnLinks.map {
                    // The location is snapped to terminus node if within close distance.
                    it.withSnappedToTerminusNode(Constants.SNAP_TO_LINK_ENDPOINT_DISTANCE_IN_METERS)
                }

            return TerminusLinkSelectionInput(
                getSourceRouteTerminusPoint(sourceRoutePoints.first(), startLocation, true),
                snapToTerminusNodes(closestStartLinks),
                getSourceRouteTerminusPoint(sourceRoutePoints.last(), endLocation, false),
                snapToTerminusNodes(closestEndLinks),
            )
        }

        internal fun resolveRoutePointSequenceCandidates(
            sourceRoutePoints: List<RoutePoint>,
            vehicleType: VehicleType,
            terminusLinkSelectionInput: TerminusLinkSelectionInput,
            matchingParams: PublicTransportRouteMatchingParameters,
        ): List<List<PgRoutingPoint>> {
            val (
                targetStartPointCandidates: List<TerminusPointCandidate>,
                targetEndPointCandidates: List<TerminusPointCandidate>,
                fromRoutePointIndexToTargetStopPoint: Map<Int, PgRoutingPoint>,
            ) =
                resolveTerminusPointCandidatesAndStopPoints(
                    sourceRoutePoints,
                    terminusLinkSelectionInput,
                    matchingParams.maxStopLocationDeviation,
                )

            // Resolve infrastructure network nodes to visit on route derived from the given route points.
            val fromRoutePointIndexToRoadJunctionNode: Map<Int, NodeProximity?> =
                matchingParams.roadJunctionMatching
                    ?.let { (matchDistance, clearingDistance) ->
                        roadJunctionMatcher.findInfrastructureNodesMatchingRoadJunctions(
                            sourceRoutePoints,
                            vehicleType,
                            matchDistance,
                            clearingDistance,
                        )
                    }
                    ?: emptyMap()

            val targetViaRoutePoints: List<PgRoutingPoint> =
                sourceRoutePoints
                    .withIndex()
                    .drop(1)
                    .dropLast(1)
                    .mapNotNull { (sourceRoutePointIndex: Int, sourceRoutePoint: RoutePoint) ->

                        when (sourceRoutePoint) {
                            is RouteStopPoint -> fromRoutePointIndexToTargetStopPoint[sourceRoutePointIndex]

                            is RouteJunctionPoint -> {
                                fromRoutePointIndexToRoadJunctionNode[sourceRoutePointIndex]?.let { RealNode(it.id) }
                            }

                            else -> null
                        }
                    }

            return MatchingServiceHelper.getSortedRoutePointSequenceCandidates(
                targetStartPointCandidates,
                targetEndPointCandidates,
                targetViaRoutePoints,
            )
        }

        internal fun resolveTerminusPointCandidatesAndStopPoints(
            sourceRoutePoints: List<RoutePoint>,
            terminusLinkSelectionInput: TerminusLinkSelectionInput,
            maxStopLocationDeviation: Double,
        ): TerminusPointCandidatesAndStopPoints {
            val fromRoutePointIndexToSnappedLinkOfMatchedStop: Map<Int, SnapStopToLinkResult> =
                publicTransportStopMatcher
                    .findStopPointsByNationalIdsAndIndexByRoutePointOrdering(
                        sourceRoutePoints,
                        maxStopLocationDeviation,
                    )

            val fromNationalIdToTargetStopPoint: Map<Int, PgRoutingPoint> =
                fromRoutePointIndexToSnappedLinkOfMatchedStop
                    .values
                    .associateBy(SnapStopToLinkResult::stopNationalId) { snap ->

                        val pointOnLink: SnappedPointOnLink =
                            snap.pointOnLink
                                // The location is snapped to terminus node if within close distance.
                                .withSnappedToTerminusNode(Constants.SNAP_TO_LINK_ENDPOINT_DISTANCE_IN_METERS)

                        // LinkSide.BOTH seems to produce more accurate results than snap.stopSideOnLink
                        PgRoutingPoint.fromSnappedPointOnLink(pointOnLink, LinkSide.BOTH)
                    }

            val (
                targetStartPointCandidates: List<TerminusPointCandidate>,
                targetEndPointCandidates: List<TerminusPointCandidate>,
            ) =
                MatchingServiceHelper.createPairwiseCandidatesForRouteTerminusPoints(
                    terminusLinkSelectionInput,
                    fromNationalIdToTargetStopPoint,
                )

            val fromRoutePointIndexToTargetStopPoint: Map<Int, PgRoutingPoint> =
                fromRoutePointIndexToSnappedLinkOfMatchedStop.mapValues {
                    // !! operator is used because the compiler is not yet smart enough to interfere
                    // that null is not a possible outcome here.
                    fromNationalIdToTargetStopPoint[it.value.stopNationalId]!!
                }

            return TerminusPointCandidatesAndStopPoints(
                targetStartPointCandidates,
                targetEndPointCandidates,
                fromRoutePointIndexToTargetStopPoint,
            )
        }

        internal fun findFirstMatchingRouteOrNull(
            sourceRouteGeometry: LineString<G2D>,
            vehicleType: VehicleType,
            targetRoutePointSequenceCandidates: List<List<PgRoutingPoint>>,
            bufferRadiusInMeters: Double,
        ): List<RouteLink>? =
            targetRoutePointSequenceCandidates.firstNotNullOfOrNull { targetRoutePoints ->

                val bufferAreaRestriction =
                    BufferAreaRestriction.from(
                        sourceRouteGeometry,
                        bufferRadiusInMeters,
                        targetRoutePoints.first(),
                        targetRoutePoints.last(),
                    )

                routingService
                    .findRouteViaPointsOnLinks(targetRoutePoints, vehicleType, true, bufferAreaRestriction)
                    .ifEmpty { null }
            }
    }
