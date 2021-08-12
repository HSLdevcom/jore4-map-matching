package fi.hsl.jore4.mapmatching.service.routing

import fi.hsl.jore4.mapmatching.model.LatLng
import fi.hsl.jore4.mapmatching.repository.infrastructure.NearestLinkResultDTO
import fi.hsl.jore4.mapmatching.repository.routing.NetworkNodeParams
import fi.hsl.jore4.mapmatching.repository.routing.RouteSegmentDTO
import fi.hsl.jore4.mapmatching.service.routing.response.LinkDTO
import fi.hsl.jore4.mapmatching.service.routing.response.ResponseCode
import fi.hsl.jore4.mapmatching.service.routing.response.RouteResultDTO
import fi.hsl.jore4.mapmatching.service.routing.response.RoutingFailureDTO
import fi.hsl.jore4.mapmatching.service.routing.response.RoutingResponse
import fi.hsl.jore4.mapmatching.service.routing.response.RoutingSuccessDTO
import fi.hsl.jore4.mapmatching.util.CollectionUtils.filterOutConsecutiveDuplicates
import fi.hsl.jore4.mapmatching.util.GeolatteUtils.mergeContinuousLines
import org.geolatte.geom.G2D
import org.geolatte.geom.LineString

object RoutingServiceHelper {

    internal fun findUnmatchedCoordinates(links: Collection<NearestLinkResultDTO>,
                                          coordinates: List<LatLng>)
        : List<LatLng> {

        val matchedCoordinates = links.map { it.fromCoordinate }.toSet()

        return coordinates.filter { !matchedCoordinates.contains(it) }
    }

    internal fun createNetworkNodeParams(links: Collection<NearestLinkResultDTO>): NetworkNodeParams {
        if (links.size < 2) {
            throw IllegalArgumentException("Must have at least 2 link objects")
        }

        val firstLinkEndpoints = links.first().getNetworkNodeIds()
        val lastLinkEndpoints = links.last().getNetworkNodeIds()
        val interimNodes = links.drop(1).dropLast(1).map { it.closerNodeId }

        return NetworkNodeParams(firstLinkEndpoints, lastLinkEndpoints, filterOutConsecutiveDuplicates(interimNodes))
    }

    internal fun transformToResponse(routeSegments: List<RouteSegmentDTO>): RoutingResponse {
        if (routeSegments.isEmpty()) {
            return RoutingFailureDTO.noSegment("Could not find a matching route")
        }

        val links = routeSegments.map { LinkDTO(it.linkId, it.isTraversalForwards, it.geom, it.cost, it.cost) }

        val segmentGeometries: List<LineString<G2D>> = routeSegments.map { it.geom }

        val mergedLine: LineString<G2D>
        try {
            mergedLine = mergeContinuousLines(segmentGeometries)
        } catch (ex: Exception) {
            return RoutingFailureDTO.noSegment(ex.message ?: "")
        }

        val totalCost = routeSegments.fold(0.0) { accumulatedCost, segment -> accumulatedCost + segment.cost }

        val route = RouteResultDTO(mergedLine, totalCost, totalCost, links)

        return RoutingSuccessDTO(ResponseCode.Ok, listOf(route))
    }
}
