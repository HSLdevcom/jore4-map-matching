package fi.hsl.jore4.mapmatching.service.common.response

import fi.hsl.jore4.mapmatching.model.PathTraversal
import fi.hsl.jore4.mapmatching.util.GeolatteUtils.mergeContinuousLines
import org.geolatte.geom.G2D
import org.geolatte.geom.LineString

object RoutingResponseCreator {

    fun create(paths: List<PathTraversal>): RoutingResponse {
        if (paths.isEmpty()) {
            return RoutingResponse.noSegment("Could not find a matching route")
        }

        val pathGeometries: List<LineString<G2D>> = paths.map(PathTraversal::geom)

        val mergedLine: LineString<G2D>
        try {
            mergedLine = mergeContinuousLines(pathGeometries)
        } catch (ex: Exception) {
            return RoutingResponse.noSegment(ex.message ?: "Merging compound LineString from multiple parts failed")
        }

        val totalCost = paths.fold(0.0) { accumulatedCost, path -> accumulatedCost + path.cost }

        val linkResults = paths.map(LinkTraversalDTO::from)

        val route = RouteResultDTO(mergedLine, totalCost, totalCost, linkResults)

        return RoutingResponse.ok(route)
    }
}
