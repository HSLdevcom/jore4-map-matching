package fi.hsl.jore4.mapmatching.repository.infrastructure

import org.geolatte.geom.G2D
import org.geolatte.geom.Point

data class SnapPointToLinkResult(val point: Point<G2D>,
                                 val queryDistance: Double,
                                 val pointOnClosestLink: SnappedPointOnLink) {

    fun withLocationOnLinkSnappedToTerminusNodeIfWithinDistance(distanceOfSnappingToLinkEndpointInMeters: Double)
        : SnapPointToLinkResult {

        return SnapPointToLinkResult(point,
                                     queryDistance,
                                     pointOnClosestLink.withSnappedToTerminusNode(distanceOfSnappingToLinkEndpointInMeters))
    }
}
