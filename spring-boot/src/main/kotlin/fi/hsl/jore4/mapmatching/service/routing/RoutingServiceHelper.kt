package fi.hsl.jore4.mapmatching.service.routing

import fi.hsl.jore4.mapmatching.model.LatLng
import fi.hsl.jore4.mapmatching.model.VehicleType
import fi.hsl.jore4.mapmatching.repository.infrastructure.SnapPointToLinkDTO
import fi.hsl.jore4.mapmatching.repository.infrastructure.SnappedLinkState
import fi.hsl.jore4.mapmatching.service.node.NodeResolutionParams

object RoutingServiceHelper {

    internal fun findUnmatchedCoordinates(snaps: Collection<SnapPointToLinkDTO>,
                                          allCoordinates: List<LatLng>)
        : List<LatLng> {

        val snappedCoordinates = snaps.map { it.point }.toSet()

        return allCoordinates.filter { !snappedCoordinates.contains(it) }
    }

    internal fun createNodeResolutionParams(vehicleType: VehicleType,
                                            snaps: Collection<SnapPointToLinkDTO>)
        : NodeResolutionParams {

        val links: List<SnappedLinkState> = snaps.map { it.link }
        return NodeResolutionParams(vehicleType, links)
    }
}
