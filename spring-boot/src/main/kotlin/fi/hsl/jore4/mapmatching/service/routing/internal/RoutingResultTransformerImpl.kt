package fi.hsl.jore4.mapmatching.service.routing.internal

import fi.hsl.jore4.mapmatching.model.ExternalLinkReference
import fi.hsl.jore4.mapmatching.model.PathTraversal
import fi.hsl.jore4.mapmatching.model.tables.records.PublicTransportStopRecord
import fi.hsl.jore4.mapmatching.service.routing.response.LinkTraversalDTO
import fi.hsl.jore4.mapmatching.service.routing.response.PublicTransportStopDTO
import fi.hsl.jore4.mapmatching.service.routing.response.PublicTransportStopDTO.LinkReferenceDTO
import fi.hsl.jore4.mapmatching.service.routing.response.ResponseCode
import fi.hsl.jore4.mapmatching.service.routing.response.RouteResultDTO
import fi.hsl.jore4.mapmatching.service.routing.response.RoutingFailureDTO
import fi.hsl.jore4.mapmatching.service.routing.response.RoutingResponse
import fi.hsl.jore4.mapmatching.service.routing.response.RoutingSuccessDTO
import fi.hsl.jore4.mapmatching.util.GeolatteUtils.mergeContinuousLines
import fi.hsl.jore4.mapmatching.util.GeolatteUtils.transformFrom3067To4326
import fi.hsl.jore4.mapmatching.util.MultilingualString
import fi.hsl.jore4.mapmatching.util.component.IJsonbConverter
import org.geolatte.geom.G2D
import org.geolatte.geom.LineString
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Component

@Component
class RoutingResultTransformerImpl @Autowired constructor(val jsonbConverter: IJsonbConverter)
    : IRoutingResultTransformer {

    override fun createResponse(paths: List<PathTraversal>,
                                stopsAlongRoute: List<PublicTransportStopRecord>): RoutingResponse {

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

    private fun toPublicTransportStopDTO(stop: PublicTransportStopRecord,
                                         externalLinkRef: ExternalLinkReference): PublicTransportStopDTO {

        val name = jsonbConverter.fromJson(stop.name, MultilingualString::class.java)

        return PublicTransportStopDTO(stop.publicTransportStopId,
                                      stop.publicTransportStopNationalId,
                                      transformFrom3067To4326(stop.geom),
                                      name,
                                      LinkReferenceDTO(stop.locatedOnInfrastructureLinkId, externalLinkRef))
    }
}
