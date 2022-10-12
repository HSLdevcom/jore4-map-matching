package fi.hsl.jore4.mapmatching.service.common.response

import fi.hsl.jore4.mapmatching.model.InfrastructureLinkTraversal
import fi.hsl.jore4.mapmatching.repository.routing.RouteLinkDTO
import fi.hsl.jore4.mapmatching.util.GeolatteUtils.mergeContinuousLines
import org.geolatte.geom.G2D
import org.geolatte.geom.LineString

object RoutingResponseCreator {

    fun create(routeLinks: List<RouteLinkDTO>): RoutingResponse {
        if (routeLinks.isEmpty()) {
            return RoutingResponse.noSegment("Could not find a matching route")
        }

        val linkTraversals: List<InfrastructureLinkTraversal> = routeLinks.map(RouteLinkDTO::linkTraversal)

        val totalWeight = linkTraversals.fold(0.0) { accumulatedWeight, link ->
            accumulatedWeight + link.traversedDistance
        }

        val sumOfLinkLengths = linkTraversals.fold(0.0) { accumulatedLength, link ->
            accumulatedLength + link.linkLength
        }

        val linesToMerge: List<LineString<G2D>> = linkTraversals.map(InfrastructureLinkTraversal::traversedGeometry)

        val mergedLine: LineString<G2D> = try {
            mergeContinuousLines(linesToMerge)
        } catch (ex: Exception) {
            return RoutingResponse.noSegment(
                ex.message ?: "Merging compound LineString from multiple infrastructure link geometries failed")
        }

        val individualLinks = routeLinks
            .map(RouteLinkDTO::linkTraversal)
            .map(LinkTraversalDTO::from)

        val routeResult = RouteResultDTO(mergedLine, totalWeight, sumOfLinkLengths, individualLinks)

        return RoutingResponse.ok(routeResult)
    }
}
