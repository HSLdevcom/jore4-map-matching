package fi.hsl.jore4.mapmatching.service.routing.response

import fi.hsl.jore4.mapmatching.model.PathTraversal
import fi.hsl.jore4.mapmatching.util.GeolatteUtils.mergeContinuousLines
import org.geolatte.geom.G2D
import org.geolatte.geom.LineString

object RoutingResultTransformer {

    fun createResponse(paths: List<PathTraversal>): RoutingResponse {
        if (paths.isEmpty()) {
            return RoutingFailureDTO.noSegment("Could not find a matching route")
        }

        val pathGeometries: List<LineString<G2D>> = paths.map { it.geom }

        val mergedLine: LineString<G2D>
        try {
            mergedLine = mergeContinuousLines(pathGeometries)
        } catch (ex: Exception) {
            return RoutingFailureDTO.noSegment(ex.message ?: "")
        }

        val totalCost = paths.fold(0.0) { accumulatedCost, path -> accumulatedCost + path.cost }

        val linkResults = paths.map { LinkTraversalDTO.from(it) }

        val route = RouteResultDTO(mergedLine, totalCost, totalCost, linkResults)

        return RoutingSuccessDTO(ResponseCode.Ok, listOf(route))
    }
}
