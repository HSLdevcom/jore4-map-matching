package fi.hsl.jore4.mapmatching.service.routing

import fi.hsl.jore4.mapmatching.model.LatLng
import fi.hsl.jore4.mapmatching.model.LocalisedName
import fi.hsl.jore4.mapmatching.model.tables.records.DrPysakkiRecord
import fi.hsl.jore4.mapmatching.repository.infrastructure.NearestLinkResultDTO
import fi.hsl.jore4.mapmatching.repository.routing.RouteSegmentDTO
import fi.hsl.jore4.mapmatching.service.routing.response.LinkDTO
import fi.hsl.jore4.mapmatching.service.routing.response.ResponseCode
import fi.hsl.jore4.mapmatching.service.routing.response.RouteResultDTO
import fi.hsl.jore4.mapmatching.service.routing.response.RoutingFailureDTO
import fi.hsl.jore4.mapmatching.service.routing.response.RoutingResponse
import fi.hsl.jore4.mapmatching.service.routing.response.RoutingSuccessDTO
import fi.hsl.jore4.mapmatching.service.routing.response.StopDTO
import fi.hsl.jore4.mapmatching.util.GeolatteUtils.mergeContinuousLines
import fi.hsl.jore4.mapmatching.util.GeolatteUtils.transformFrom3067To4326
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

    internal fun filterStopsByTraversalDirection(allStopsAlongLinks: List<DrPysakkiRecord>,
                                                 traversedLinks: List<LinkTraversalDTO>): List<DrPysakkiRecord> {

        val traversedLinkIds: Set<String> = traversedLinks.map { it.linkId }.toSet()

        // Verify mutual consistency of given parameters.
        allStopsAlongLinks.forEach {
            if (!traversedLinkIds.contains(it.linkId)) {
                throw IllegalArgumentException(
                    "Inconsistent parameters: encountered a stop not contained in traversed links")
            }
        }

        val stopsByLinkId: Map<String, List<DrPysakkiRecord>> = allStopsAlongLinks.groupBy { it.linkId }

        return traversedLinks.flatMap {
            val linkTraversedForwards = it.onLinkTraversalForwards
            val stopsForLinkId: List<DrPysakkiRecord> = stopsByLinkId.getOrDefault(it.linkId, emptyList())

            stopsForLinkId
                .filter { stop ->
                    when (stop.vaikSuunt) {
                        2 -> linkTraversedForwards
                        3 -> !linkTraversedForwards
                        else -> false
                    }
                }
                .sortedWith(compareBy {
                    if (linkTraversedForwards)
                        it.sijaintiM
                    else
                        -it.sijaintiM
                })
        }
    }

    internal fun transformToResponse(routeSegments: List<RouteSegmentDTO>,
                                     stopsAlongRoute: List<DrPysakkiRecord>): RoutingResponse {

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

        val stops = stopsAlongRoute.map {
            StopDTO(transformFrom3067To4326(it.geom),
                    it.valtakId,
                    it.linkId,
                    LocalisedName(it.nimiSu, it.nimiRu))
        }

        val route = RouteResultDTO(mergedLine, totalCost, totalCost, links, stops)

        return RoutingSuccessDTO(ResponseCode.Ok, listOf(route))
    }
}