package fi.hsl.jore4.mapmatching.service.routing

import fi.hsl.jore4.mapmatching.model.LatLng
import fi.hsl.jore4.mapmatching.model.VehicleType
import fi.hsl.jore4.mapmatching.repository.infrastructure.SnapToLinkDTO
import fi.hsl.jore4.mapmatching.repository.infrastructure.SnappedLinkState
import fi.hsl.jore4.mapmatching.service.routing.internal.NodeResolutionParams

object RoutingServiceHelper {

    internal fun findUnmatchedCoordinates(links: Collection<SnapToLinkDTO>,
                                          coordinates: List<LatLng>)
        : List<LatLng> {

        val matchedCoordinates = links.map { it.point }.toSet()

        return coordinates.filter { !matchedCoordinates.contains(it) }
    }

    internal fun createNodeResolutionParams(vehicleType: VehicleType,
                                            snaps: Collection<SnapToLinkDTO>): NodeResolutionParams {

        val links: List<SnappedLinkState> = snaps.map { it.link }
        return NodeResolutionParams(vehicleType, links)
    }
}
