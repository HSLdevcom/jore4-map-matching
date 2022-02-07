package fi.hsl.jore4.mapmatching.service.common.response

import fi.hsl.jore4.mapmatching.model.GeomTraversal
import fi.hsl.jore4.mapmatching.model.InfrastructureLinkTraversal
import fi.hsl.jore4.mapmatching.repository.routing.RouteDTO
import fi.hsl.jore4.mapmatching.repository.routing.RouteLinkDTO
import fi.hsl.jore4.mapmatching.util.GeolatteUtils.mergeContinuousTraversals
import org.geolatte.geom.G2D
import org.geolatte.geom.LineString

object RoutingResponseCreator {

    fun create(route: RouteDTO): RoutingResponse {
        if (route.routeLinks.isEmpty()) {
            return RoutingResponse.noSegment("Could not find a matching route")
        }

        val trimmedLinkTraversals: List<InfrastructureLinkTraversal> = route
            .getRouteLinksWithTrimmedTerminus()
            .map(RouteLinkDTO::linkTraversal)

        val totalCost = trimmedLinkTraversals.fold(0.0) { accumulatedCost, link -> accumulatedCost + link.cost }

        val geomTraversals: List<GeomTraversal> = trimmedLinkTraversals.map(InfrastructureLinkTraversal::geomTraversal)

        val mergedLine: LineString<G2D> = try {
            mergeContinuousTraversals(geomTraversals)
        } catch (ex: Exception) {
            return RoutingResponse.noSegment(
                ex.message ?: "Merging compound LineString from multiple infrastructure link geometries failed")
        }

        val individualLinks = route.routeLinks
            .map(RouteLinkDTO::linkTraversal)
            .map(LinkTraversalDTO::from)

        val routeResult = RouteResultDTO(mergedLine, totalCost, totalCost, individualLinks)

        return RoutingResponse.ok(routeResult)
    }
}
