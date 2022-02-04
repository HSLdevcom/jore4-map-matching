package fi.hsl.jore4.mapmatching.service.common.response

import fi.hsl.jore4.mapmatching.model.GeomTraversal
import fi.hsl.jore4.mapmatching.model.InfrastructureLinkTraversal
import fi.hsl.jore4.mapmatching.util.GeolatteUtils.mergeContinuousTraversals
import org.geolatte.geom.G2D
import org.geolatte.geom.LineString

object RoutingResponseCreator {

    fun create(linkTraversals: List<InfrastructureLinkTraversal>): RoutingResponse {
        if (linkTraversals.isEmpty()) {
            return RoutingResponse.noSegment("Could not find a matching route")
        }

        val geomTraversals: List<GeomTraversal> = linkTraversals.map(InfrastructureLinkTraversal::geomTraversal)

        val mergedLine: LineString<G2D>
        try {
            mergedLine = mergeContinuousTraversals(geomTraversals)
        } catch (ex: Exception) {
            return RoutingResponse.noSegment(
                ex.message ?: "Merging compound LineString from multiple infrastructure link geometries failed")
        }

        val totalCost = linkTraversals.fold(0.0) { accumulatedCost, path -> accumulatedCost + path.cost }

        val linkResults = linkTraversals.map(LinkTraversalDTO::from)

        val route = RouteResultDTO(mergedLine, totalCost, totalCost, linkResults)

        return RoutingResponse.ok(route)
    }
}
