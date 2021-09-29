package fi.hsl.jore4.mapmatching.service.routing

import fi.hsl.jore4.mapmatching.model.LatLng
import fi.hsl.jore4.mapmatching.repository.infrastructure.DirectionType
import fi.hsl.jore4.mapmatching.repository.infrastructure.NearestLinkResultDTO
import fi.hsl.jore4.mapmatching.repository.infrastructure.StopInfoDTO
import fi.hsl.jore4.mapmatching.repository.routing.RouteSegmentDTO
import fi.hsl.jore4.mapmatching.service.routing.response.LinkDTO
import fi.hsl.jore4.mapmatching.service.routing.response.ResponseCode
import fi.hsl.jore4.mapmatching.service.routing.response.RouteResultDTO
import fi.hsl.jore4.mapmatching.service.routing.response.RoutingFailureDTO
import fi.hsl.jore4.mapmatching.service.routing.response.RoutingResponse
import fi.hsl.jore4.mapmatching.service.routing.response.RoutingSuccessDTO
import fi.hsl.jore4.mapmatching.service.routing.response.StopDTO
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

    internal fun createNodeResolutionParams(links: Collection<NearestLinkResultDTO>) =
        NodeResolutionParams(links.map {
            NodeResolutionParams.SelectedLink(it.linkId, it.closerNodeId, it.furtherNodeId)
        })

    internal fun filterStopsByTraversalDirection(allStopsAlongLinks: List<StopInfoDTO>,
                                                 traversedLinks: List<LinkTraversalDTO>): List<StopInfoDTO> {

        val traversedLinkIds: Set<String> = traversedLinks.map { it.linkId }.toSet()

        // Verify mutual consistency of given parameters.
        allStopsAlongLinks.forEach {
            if (!traversedLinkIds.contains(it.linkId)) {
                throw IllegalArgumentException(
                    "Inconsistent parameters: encountered a stop not contained in traversed links")
            }
        }

        val stopsByLinkId: Map<String, List<StopInfoDTO>> = allStopsAlongLinks.groupBy { it.linkId }

        return traversedLinks.flatMap {
            val linkTraversedForwards = it.onLinkTraversalForwards
            val stopsForLinkId: List<StopInfoDTO> = stopsByLinkId.getOrDefault(it.linkId, emptyList())

            stopsForLinkId
                .filter { stop ->
                    when (stop.direction) {
                        DirectionType.ALONG_DIGITISED_DIRECTION -> linkTraversedForwards
                        DirectionType.AGAINST_DIGITISED_DIRECTION -> !linkTraversedForwards
                        else -> false
                    }
                }
                .sortedWith(compareBy {
                    if (linkTraversedForwards)
                        it.distanceFromLinkStart
                    else
                        -it.distanceFromLinkStart
                })
        }
    }

    internal fun transformToResponse(routeSegments: List<RouteSegmentDTO>,
                                     stopsAlongRoute: List<StopInfoDTO>): RoutingResponse {

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

        val stops = stopsAlongRoute.map { StopDTO(it.location.toGeolattePoint(), it.nationalId, it.linkId, it.name) }

        val route = RouteResultDTO(mergedLine, totalCost, totalCost, links, stops)

        return RoutingSuccessDTO(ResponseCode.Ok, listOf(route))
    }
}
