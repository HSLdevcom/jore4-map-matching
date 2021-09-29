package fi.hsl.jore4.mapmatching.service.routing.response

import fi.hsl.jore4.mapmatching.model.ExternalLinkReference
import fi.hsl.jore4.mapmatching.model.PathTraversal
import fi.hsl.jore4.mapmatching.repository.infrastructure.StopInfoDTO
import fi.hsl.jore4.mapmatching.service.routing.response.PublicTransportStopDTO.LinkReferenceDTO
import fi.hsl.jore4.mapmatching.util.GeolatteUtils.mergeContinuousLines
import org.geolatte.geom.G2D
import org.geolatte.geom.LineString

object RoutingResultTransformer {

    fun createResponse(paths: List<PathTraversal>, stopsAlongRoute: List<StopInfoDTO>): RoutingResponse {
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

        val externalLinkRefByLinkId: Map<Long, ExternalLinkReference> =
            paths.associateBy({ it.infrastructureLinkId }, { it.externalLinkRef })

        val stopResults = stopsAlongRoute.map { stop ->
            val externalLinkRef: ExternalLinkReference = externalLinkRefByLinkId[stop.locatedOnInfrastructureLinkId]!!

            toPublicTransportStopDTO(stop, externalLinkRef)
        }

        val route = RouteResultDTO(mergedLine, totalCost, totalCost, linkResults, stopResults)

        return RoutingSuccessDTO(ResponseCode.Ok, listOf(route))
    }

    private fun toPublicTransportStopDTO(stop: StopInfoDTO,
                                         externalLinkRef: ExternalLinkReference): PublicTransportStopDTO {

        return PublicTransportStopDTO(stop.publicTransportStopId,
                                      stop.stopNationalId,
                                      stop.stopPoint.toGeolattePoint(),
                                      stop.name,
                                      LinkReferenceDTO(stop.locatedOnInfrastructureLinkId, externalLinkRef))
    }
}
