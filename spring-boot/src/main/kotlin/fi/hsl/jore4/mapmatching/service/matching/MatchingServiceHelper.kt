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
import fi.hsl.jore4.mapmatching.repository.infrastructure.SnappedLinkState
import fi.hsl.jore4.mapmatching.service.node.NodeSequenceCandidatesBetweenSnappedLinks
import fi.hsl.jore4.mapmatching.service.node.NodeSequenceCombinationsCreator
import fi.hsl.jore4.mapmatching.service.node.VisitedNodes
import fi.hsl.jore4.mapmatching.service.node.VisitedNodesResolver
import fi.hsl.jore4.mapmatching.util.CollectionUtils.filterOutConsecutiveDuplicates
import mu.KotlinLogging
import org.geolatte.geom.G2D
import org.geolatte.geom.Point

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

    fun resolveTerminusLinkCandidates(terminusLinkSelectionInput: TerminusLinkSelectionInput,
                                      fromStopNationalIdToInfrastructureLinkId: Map<Int, InfrastructureLinkId>)
        : Pair<List<TerminusLinkCandidate>, List<TerminusLinkCandidate>> {

        fun createTerminusLinkCandidates(closestLinks: List<SnappedLinkState>,
                                         routeTerminusPoint: SourceRouteTerminusPoint)
            : List<TerminusLinkCandidate> {

            val linkIdAssociatedWithStopPoint: InfrastructureLinkId? =
                resolveTerminusLinkIdIfMatchFoundByStopNationalId(routeTerminusPoint,
                                                                  fromStopNationalIdToInfrastructureLinkId)

            return when (linkIdAssociatedWithStopPoint) {
                null -> closestLinks.map { TerminusLinkCandidate(it, terminusStopPointMatchFoundByNationalId = false) }
                else -> {
                    closestLinks.map { snappedLink ->
                        if (snappedLink.infrastructureLinkId == linkIdAssociatedWithStopPoint)
                            TerminusLinkCandidate(
                                // Move snap point inwards just 1.0 meters.
                                snappedLink.moveSnapPointInwardsIfLocatedAtEndpoint(1.0),
                                terminusStopPointMatchFoundByNationalId = true)
                        else
                            TerminusLinkCandidate(snappedLink,
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
                                        viaNodeHolders: List<Either<SnappedLinkState, NodeProximity>>)
        : List<NodeSequenceCandidatesBetweenSnappedLinks> {

        val viaNodeIds: List<InfrastructureNodeId> = viaNodeHolders.map { either ->
            either.fold(HasInfrastructureNodeId::getInfrastructureNodeId,
                        HasInfrastructureNodeId::getInfrastructureNodeId)
        }

        fun findLinkIdOfTerminusStopPoint(linkCandidates: List<TerminusLinkCandidate>): InfrastructureLinkId? {
            return linkCandidates
                .find(TerminusLinkCandidate::terminusStopPointMatchFoundByNationalId)
                ?.snappedLink?.infrastructureLinkId
        }

        val linkIdOfStartStop: InfrastructureLinkId? = findLinkIdOfTerminusStopPoint(startLinkCandidates)
        val linkIdOfEndStop: InfrastructureLinkId? = findLinkIdOfTerminusStopPoint(endLinkCandidates)

        val hashCodesOfNodeSequences: MutableSet<Int> = mutableSetOf()

        return startLinkCandidates
            .flatMap { startLinkCandidate ->
                endLinkCandidates.mapNotNull { endLinkCandidate ->

                    val startLink: SnappedLinkState = startLinkCandidate.snappedLink
                    val endLink: SnappedLinkState = endLinkCandidate.snappedLink

                    val nodesToVisit: VisitedNodes = VisitedNodesResolver.resolve(startLink, viaNodeIds, endLink)

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

                fun getNumberOfTerminusStopsAlongTerminusLinks(choice: NodeSequenceCandidatesBetweenSnappedLinks): Int {
                    // Indicates whether the source route's start point (in case it is a stop point)
                    // is along the start link of this terminus link pair.
                    val isRouteStartStopAlongStartLink: Boolean = linkIdOfStartStop
                        ?.let { it == choice.startLink.infrastructureLinkId }
                        ?: false

                    // Indicates whether the source route's end point (in case it is a stop point)
                    // is along the end link of this terminus link pair.
                    val isRouteEndStopAlongEndLink: Boolean = linkIdOfEndStop
                        ?.let { it == choice.endLink.infrastructureLinkId }
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
