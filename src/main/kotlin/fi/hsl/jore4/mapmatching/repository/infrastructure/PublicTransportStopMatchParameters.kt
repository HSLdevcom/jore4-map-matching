package fi.hsl.jore4.mapmatching.repository.infrastructure

import org.geolatte.geom.G2D
import org.geolatte.geom.Point

/**
 * Contains parameters for matching public transport stops from a client system
 * with the ones hosted by this map-matching service.
 *
 * @property nationalId the national identifier for public transport stop
 * @property sourceLocation the location (for public transport stop) defined in
 * the client system (invoking this map-matching service).
 */
data class PublicTransportStopMatchParameters(
    val nationalId: Int,
    val sourceLocation: Point<G2D>
)
