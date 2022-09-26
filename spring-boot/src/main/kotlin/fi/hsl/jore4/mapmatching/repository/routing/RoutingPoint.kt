package fi.hsl.jore4.mapmatching.repository.routing

import fi.hsl.jore4.mapmatching.model.InfrastructureLinkId

data class RoutingPoint(val linkId: InfrastructureLinkId, val fractionalLocation: Double)
