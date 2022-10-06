package fi.hsl.jore4.mapmatching.service.matching

import arrow.core.Either
import fi.hsl.jore4.mapmatching.model.HasInfrastructureNodeId
import fi.hsl.jore4.mapmatching.model.InfrastructureLinkId
import fi.hsl.jore4.mapmatching.model.InfrastructureNodeId
import fi.hsl.jore4.mapmatching.model.NodeIdSequence
import fi.hsl.jore4.mapmatching.model.NodeProximity
import fi.hsl.jore4.mapmatching.model.VehicleMode
import fi.hsl.jore4.mapmatching.model.VehicleType
import fi.hsl.jore4.mapmatching.model.matching.RoutePoint
import fi.hsl.jore4.mapmatching.model.matching.RouteStopPoint
import fi.hsl.jore4.mapmatching.model.matching.TerminusType
import fi.hsl.jore4.mapmatching.repository.infrastructure.SnappedPointOnLink
import fi.hsl.jore4.mapmatching.repository.routing.PgRoutingPoint
import fi.hsl.jore4.mapmatching.repository.routing.RealNode
import fi.hsl.jore4.mapmatching.repository.routing.VirtualNode
import fi.hsl.jore4.mapmatching.service.node.NodeSequenceCandidatesBetweenSnappedLinks
import fi.hsl.jore4.mapmatching.service.node.NodeSequenceCombinationsCreator
import fi.hsl.jore4.mapmatching.service.node.VisitedNodes
import fi.hsl.jore4.mapmatching.service.node.VisitedNodesResolver
import fi.hsl.jore4.mapmatching.util.CollectionUtils.filterOutConsecutiveDuplicates
import mu.KotlinLogging
import org.geolatte.geom.G2D
import org.geolatte.geom.Point
import kotlin.math.max
import kotlin.math.min

private val LOGGER = KotlinLogging.logger {}

/**
 * This object contains static helper methods related to map-matching. The reason these methods are
 * here instead of e.g. a service implementation class is that this allows for or a more testable
 * code structure. The methods added here must be independent and contain such logic that it is
 * meaningful to test them with unit tests.
 */
object MatchingServiceHelper {

    fun validateInputForRouteMatching(routePoints: List<RoutePoint>, vehicleType: VehicleType): String? {
        if (vehicleType.vehicleMode != VehicleMode.BUS)
            return "Only bus infrastructure is currently supported in map-matching"

        if (!hasAtLeastTwoDistinctRoutePointLocations(routePoints))
            return "At least 2 distinct locations within route points must be given"

        return null
    }

    private fun hasAtLeastTwoDistinctRoutePointLocations(routePoints: List<RoutePoint>): Boolean {
        val routePointLocations: List<Point<G2D>> = routePoints.map(RoutePoint::location)

        return filterOutConsecutiveDuplicates(routePointLocations).size >= 2
    }

    fun getSourceRouteTerminusPoint(routePoint: RoutePoint,
                                    terminusLocationFromRouteLine: Point<G2D>,
                                    isStartPoint: Boolean): SourceRouteTerminusPoint {

        val terminusType = if (isStartPoint) TerminusType.START else TerminusType.END

        return when (routePoint) {
            is RouteStopPoint -> SourceRouteTerminusStopPoint(terminusLocationFromRouteLine,
                                                              terminusType,
                                                              routePoint.nationalId)

            else -> SourceRouteTerminusNonStopPoint(terminusLocationFromRouteLine, terminusType)
        }
    }

    fun createPairwiseCandidatesForRouteTerminusPoints(terminusLinkSelectionInput: TerminusLinkSelectionInput,
                                                       stopPointsIndexedByNationalId: Map<Int, PgRoutingPoint>)
        : Pair<List<TerminusPointCandidate>, List<TerminusPointCandidate>> {

        fun createTerminusPointCandidatesForOneEndpoint(linkCandidates: List<SnappedPointOnLink>,
                                                        sourceRouteTerminusPoint: SourceRouteTerminusPoint)
            : List<TerminusPointCandidate> {

            val stopPointMatchedByNationalId: PgRoutingPoint? =
                extractTerminusStopPointIfMatchFoundByNationalId(sourceRouteTerminusPoint,
                                                                 stopPointsIndexedByNationalId)

            val terminusPointCandidates: MutableList<TerminusPointCandidate> = ArrayList()

            linkCandidates.forEach { snappedPointOnLink ->

                val targetRoutePoint: PgRoutingPoint = PgRoutingPoint.fromSnappedPointOnLink(snappedPointOnLink)

                val isTargetRoutePointAStopPointMatchedByNationalId: Boolean = stopPointMatchedByNationalId
                    ?.let { refStopPoint ->
                        when (refStopPoint) {
                            is RealNode ->
                                targetRoutePoint is RealNode && targetRoutePoint.nodeId == refStopPoint.nodeId

                            is VirtualNode ->
                                targetRoutePoint is VirtualNode && targetRoutePoint.linkId == refStopPoint.linkId
                        }
                    }
                    ?: false

                if (isTargetRoutePointAStopPointMatchedByNationalId) {
                    // When targetRoutePoint corresponds to the public transport stop point
                    // possessing the national ID that is given in the map-matching request,
                    // then add the point as the first candidate into the list.
                    terminusPointCandidates.add(0,
                                                TerminusPointCandidate(targetRoutePoint,
                                                                       true,
                                                                       snappedPointOnLink.closestDistance))
                } else {
                    terminusPointCandidates.add(TerminusPointCandidate(targetRoutePoint,
                                                                       false,
                                                                       snappedPointOnLink.closestDistance))
                }
            }

            // There may be duplicates if multiple points on different links are snapped to same endpoint node.
            return terminusPointCandidates.distinct()
        }

        return Pair(
            createTerminusPointCandidatesForOneEndpoint(terminusLinkSelectionInput.closestStartLinks,
                                                        terminusLinkSelectionInput.sourceRouteStartPoint),
            createTerminusPointCandidatesForOneEndpoint(terminusLinkSelectionInput.closestEndLinks,
                                                        terminusLinkSelectionInput.sourceRouteEndPoint))
    }

    private fun extractTerminusStopPointIfMatchFoundByNationalId(
        sourceRouteTerminusPoint: SourceRouteTerminusPoint,
        stopPointsIndexedByNationalId: Map<Int, PgRoutingPoint>): PgRoutingPoint? {

        val terminusType: TerminusType = sourceRouteTerminusPoint.terminusType

        return when (sourceRouteTerminusPoint) {

            is SourceRouteTerminusStopPoint -> {

                when (val stopPointNationalId: Int? = sourceRouteTerminusPoint.stopPointNationalId) {

                    null -> {
                        LOGGER.debug { "Public transport stop for route $terminusType point is not given national ID" }
                        null
                    }

                    else -> {
                        stopPointsIndexedByNationalId[stopPointNationalId]
                            ?.also {
                                LOGGER.debug {
                                    when (it) {
                                        is RealNode ->
                                            "Resolved infrastructureNodeId=${it.nodeId} as $terminusType node " +
                                                "candidate from public transport stop matched with " +
                                                "nationalId=$stopPointNationalId"

                                        is VirtualNode ->
                                            "Resolved infrastructureLinkId=${it.linkId} as $terminusType link " +
                                                "candidate from public transport stop matched with " +
                                                "nationalId=$stopPointNationalId"
                                    }
                                }
                            }
                            ?: run {
                                LOGGER.debug {
                                    "Could not resolve public transport stop for route $terminusType point by " +
                                        "national ID: $stopPointNationalId"
                                }
                                null
                            }
                    }
                }
            }

            else -> {
                LOGGER.debug { "Route $terminusType point is not a public transport stop point" }
                null
            }
        }
    }

    fun resolveTerminusLinkCandidates(terminusLinkSelectionInput: TerminusLinkSelectionInput,
                                      fromStopNationalIdToInfrastructureLinkId: Map<Int, InfrastructureLinkId>)
        : Pair<List<TerminusLinkCandidate>, List<TerminusLinkCandidate>> {

        fun createTerminusLinkCandidates(closestLinks: List<SnappedPointOnLink>,
                                         routeTerminusPoint: SourceRouteTerminusPoint)
            : List<TerminusLinkCandidate> {

            val linkIdAssociatedWithStopPoint: InfrastructureLinkId? =
                resolveTerminusLinkIdIfMatchFoundByStopNationalId(routeTerminusPoint,
                                                                  fromStopNationalIdToInfrastructureLinkId)

            return when (linkIdAssociatedWithStopPoint) {
                null -> closestLinks.map { TerminusLinkCandidate(it, terminusStopPointMatchFoundByNationalId = false) }
                else -> {
                    closestLinks.map { pointOnLink ->
                        if (pointOnLink.infrastructureLinkId == linkIdAssociatedWithStopPoint)
                            TerminusLinkCandidate(
                                // Move snap point inwards just 1.0 meters.
                                pointOnLink.moveSnapPointInwardsIfLocatedAtEndpoint(1.0),
                                terminusStopPointMatchFoundByNationalId = true)
                        else
                            TerminusLinkCandidate(pointOnLink,
                                                  terminusStopPointMatchFoundByNationalId = false)
                    }
                }
            }
        }

        return Pair(createTerminusLinkCandidates(terminusLinkSelectionInput.closestStartLinks,
                                                 terminusLinkSelectionInput.sourceRouteStartPoint),
                    createTerminusLinkCandidates(terminusLinkSelectionInput.closestEndLinks,
                                                 terminusLinkSelectionInput.sourceRouteEndPoint))
    }

    private fun resolveTerminusLinkIdIfMatchFoundByStopNationalId(
        sourceRouteTerminusPoint: SourceRouteTerminusPoint,
        fromStopNationalIdToInfrastructureLinkId: Map<Int, InfrastructureLinkId>): InfrastructureLinkId? {

        val terminusType: TerminusType = sourceRouteTerminusPoint.terminusType

        return when (sourceRouteTerminusPoint) {

            is SourceRouteTerminusStopPoint -> {

                when (val stopPointNationalId: Int? = sourceRouteTerminusPoint.stopPointNationalId) {

                    null -> {
                        LOGGER.debug { "Public transport stop for route $terminusType point is not given national ID" }
                        null
                    }

                    else -> {
                        fromStopNationalIdToInfrastructureLinkId[stopPointNationalId]
                            ?.also { linkId ->
                                LOGGER.debug {
                                    "Resolved infrastructureLinkId=$linkId as $terminusType link candidate from " +
                                        "public transport stop matched with nationalId=$stopPointNationalId"
                                }
                            }
                            ?: run {
                                LOGGER.debug {
                                    "Could not resolve public transport stop for route $terminusType point by " +
                                        "national ID: $stopPointNationalId"
                                }
                                null
                            }
                    }
                }
            }

            else -> {
                LOGGER.debug { "Route $terminusType point is not a public transport stop point" }
                null
            }
        }
    }

    /**
     * Creates a sorted list of route point sequence candidates from the given terminus point
     * candidates and via-points.
     *
     * In the sorting, priority is given to those route point sequences that are related to public
     * transport stop points that are matched with the national identifiers found in the
     * map-matching request. The distance from the end point of the source route to the terminus
     * point candidate is used as a secondary sorting criterion.
     *
     * In addition, sorting takes place in pairs. For example, a pair with more associations with
     * public transport stop points is sorted before a pair with fewer associations. Likewise, if
     * there are an equal number of stop point associations, the pair with the closest snap distance
     * to one of its terminus point candidates is sorted first.
     */
    fun getSortedRoutePointSequenceCandidates(startPointCandidates: List<TerminusPointCandidate>,
                                              endPointCandidates: List<TerminusPointCandidate>,
                                              viaRoutePoints: List<PgRoutingPoint>)
        : List<List<PgRoutingPoint>> {

        return startPointCandidates
            .flatMap { startPointCandidate ->
                endPointCandidates.map { endPointCandidate -> startPointCandidate to endPointCandidate }
            }
            .sortedWith { terminiCandidate1, terminiCandidate2 ->

                // Sort terminus points candidates.

                fun getNumberOfStopsRelatedToTerminiCandidate(terminiCandidate: Pair<TerminusPointCandidate, TerminusPointCandidate>)
                    : Int {

                    val isStartPointMatchedByStopNationalId: Boolean =
                        terminiCandidate.first.isAStopPointMatchedByNationalId

                    val isEndPointMatchedByStopNationalId: Boolean =
                        terminiCandidate.second.isAStopPointMatchedByNationalId

                    // Deduce the amount of stops associated with terminus points.
                    return if (isStartPointMatchedByStopNationalId) {
                        if (isEndPointMatchedByStopNationalId) 2 else 1
                    } else if (isEndPointMatchedByStopNationalId) 1
                    else 0
                }

                val stopCount1: Int = getNumberOfStopsRelatedToTerminiCandidate(terminiCandidate1)
                val stopCount2: Int = getNumberOfStopsRelatedToTerminiCandidate(terminiCandidate2)

                // Prioritise terminus candidate pairs that are associated with public transport stops by sorting them
                // first. The stops are matched with national ID from source route's first and last route point.

                if (stopCount1 > stopCount2)
                    -1
                else if (stopCount1 < stopCount2)
                    1
                else {
                    // delegate to snap distance comparison

                    fun getClosestDistance(terminiCandidate: Pair<TerminusPointCandidate, TerminusPointCandidate>): Double =
                        min(terminiCandidate.first.snapDistance, terminiCandidate.second.snapDistance)

                    fun getFurthestDistance(terminiCandidate: Pair<TerminusPointCandidate, TerminusPointCandidate>): Double =
                        max(terminiCandidate.first.snapDistance, terminiCandidate.second.snapDistance)

                    // sort terminus point candidate pairs by their snap distances

                    val closestSnapDistance1: Double = getClosestDistance(terminiCandidate1)
                    val closestSnapDistance2: Double = getClosestDistance(terminiCandidate2)

                    if (closestSnapDistance1 < closestSnapDistance2)
                        -1
                    else if (closestSnapDistance1 > closestSnapDistance2)
                        1
                    else
                        getFurthestDistance(terminiCandidate1).compareTo(getFurthestDistance(terminiCandidate2))
                }
            }
            .map { (startPointCandidate, endPointCandidate) ->

                // Returns an immutable list.
                buildList {
                    add(startPointCandidate.targetPoint)
                    addAll(viaRoutePoints)
                    add(endPointCandidate.targetPoint)
                }
            }
    }

    /**
     * Creates a sorted list of node sequence candidates from the given terminus link candidates and
     * via-nodes.
     *
     * In the sorting, priority is given to those terminus link candidates that are related to public
     * transport stop points that are matched with the national identifiers found in the map-matching
     * request. The distance from the end point of the source route to the terminus link candidate is
     * used as a secondary sorting criterion.
     *
     * In addition, sorting takes place in pairs. For example, a pair with more associations with
     * public transport stop points is sorted before a pair with fewer associations. Likewise, if
     * there are an equal number of stop point associations, the pair with the closest snap distance
     * to one of its terminus link candidates is sorted first.
     *
     * @throws [IllegalStateException]
     */
    fun getSortedNodeSequenceCandidates(startLinkCandidates: List<TerminusLinkCandidate>,
                                        endLinkCandidates: List<TerminusLinkCandidate>,
                                        viaNodeHolders: List<Either<SnappedPointOnLink, NodeProximity>>)
        : List<NodeSequenceCandidatesBetweenSnappedLinks> {

        val viaNodeIds: List<InfrastructureNodeId> = viaNodeHolders.map { either ->
            either.fold(HasInfrastructureNodeId::getInfrastructureNodeId,
                        HasInfrastructureNodeId::getInfrastructureNodeId)
        }

        fun findLinkIdOfTerminusStopPoint(linkCandidates: List<TerminusLinkCandidate>): InfrastructureLinkId? {
            return linkCandidates
                .find(TerminusLinkCandidate::terminusStopPointMatchFoundByNationalId)
                ?.pointOnLink?.infrastructureLinkId
        }

        val linkIdOfStartStop: InfrastructureLinkId? = findLinkIdOfTerminusStopPoint(startLinkCandidates)
        val linkIdOfEndStop: InfrastructureLinkId? = findLinkIdOfTerminusStopPoint(endLinkCandidates)

        val hashCodesOfNodeSequences: MutableSet<Int> = mutableSetOf()

        return startLinkCandidates
            .flatMap { startLinkCandidate ->
                endLinkCandidates.mapNotNull { endLinkCandidate ->

                    val pointOnStartLink: SnappedPointOnLink = startLinkCandidate.pointOnLink
                    val pointOnEndLink: SnappedPointOnLink = endLinkCandidate.pointOnLink

                    val nodesToVisit: VisitedNodes =
                        VisitedNodesResolver.resolve(pointOnStartLink, viaNodeIds, pointOnEndLink)

                    // Filter out duplicate node sequences that are already encountered within
                    // previously processed pairs of terminus links. Duplicates may exist in case
                    // multiple links are snapped to their endpoint nodes. This optimisation
                    // eventually avoids unnecessary expensive SQL queries.

                    val nodeSequenceCombinations: List<NodeIdSequence> = NodeSequenceCombinationsCreator
                        .create(nodesToVisit)
                        .filter { nodeIdSeq: NodeIdSequence ->
                            // Add hashcode of node sequence into set as side effect. Return false
                            // if the hashcode already exists in the set.
                            hashCodesOfNodeSequences.add(nodeIdSeq.list.hashCode())
                        }

                    if (nodeSequenceCombinations.isNotEmpty())
                        NodeSequenceCandidatesBetweenSnappedLinks(pointOnStartLink,
                                                                  pointOnEndLink,
                                                                  nodeSequenceCombinations)
                    else
                        null
                }
            }
            .filter(NodeSequenceCandidatesBetweenSnappedLinks::isRoutePossible)
            .sortedWith { choice1, choice2 ->
                // Sort node sequence candidates.

                fun getNumberOfTerminusStopsAlongTerminusLinks(choice: NodeSequenceCandidatesBetweenSnappedLinks): Int {
                    // Indicates whether the source route's start point (in case it is a stop point)
                    // is along the start link of this terminus link pair.
                    val isRouteStartStopAlongStartLink: Boolean = linkIdOfStartStop
                        ?.let { it == choice.pointOnStartLink.infrastructureLinkId }
                        ?: false

                    // Indicates whether the source route's end point (in case it is a stop point)
                    // is along the end link of this terminus link pair.
                    val isRouteEndStopAlongEndLink: Boolean = linkIdOfEndStop
                        ?.let { it == choice.pointOnEndLink.infrastructureLinkId }
                        ?: false

                    // Deduce the amount of stops along given terminus link pair.
                    return if (isRouteStartStopAlongStartLink) {
                        if (isRouteEndStopAlongEndLink) 2 else 1
                    } else if (isRouteEndStopAlongEndLink) 1
                    else 0
                }

                val stopCount1: Int = getNumberOfTerminusStopsAlongTerminusLinks(choice1)
                val stopCount2: Int = getNumberOfTerminusStopsAlongTerminusLinks(choice2)

                // Prioritise terminus link pairs that are associated with public transport stops by sorting them first.
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
