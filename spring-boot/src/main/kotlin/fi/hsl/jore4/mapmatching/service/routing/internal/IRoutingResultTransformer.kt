package fi.hsl.jore4.mapmatching.service.routing.internal

import fi.hsl.jore4.mapmatching.model.PathTraversal
import fi.hsl.jore4.mapmatching.model.tables.records.PublicTransportStopRecord
import fi.hsl.jore4.mapmatching.service.routing.response.RoutingResponse

interface IRoutingResultTransformer {

    fun createResponse(paths: List<PathTraversal>, stopsAlongRoute: List<PublicTransportStopRecord>): RoutingResponse
}
