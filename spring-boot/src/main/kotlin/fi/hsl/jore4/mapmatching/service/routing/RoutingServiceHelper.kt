package fi.hsl.jore4.mapmatching.service.routing

import fi.hsl.jore4.mapmatching.model.LatLng
import fi.hsl.jore4.mapmatching.repository.infrastructure.SnapToLinkDTO
import fi.hsl.jore4.mapmatching.repository.routing.NetworkNodeParams
import fi.hsl.jore4.mapmatching.util.CollectionUtils.filterOutConsecutiveDuplicates

object RoutingServiceHelper {

    internal fun findUnmatchedCoordinates(links: Collection<SnapToLinkDTO>,
                                          coordinates: List<LatLng>)
        : List<LatLng> {

        val matchedCoordinates = links.map { it.point }.toSet()

        return coordinates.filter { !matchedCoordinates.contains(it) }
    }

    internal fun createNetworkNodeParams(snaps: Collection<SnapToLinkDTO>): NetworkNodeParams {
        if (snaps.size < 2) {
            throw IllegalArgumentException("Must have at least 2 snapped links")
        }

        val firstLinkEndpoints = snaps.first().getNetworkNodeIds()
        val lastLinkEndpoints = snaps.last().getNetworkNodeIds()
        val interimNodes = snaps.drop(1).dropLast(1).map { it.closerNodeId }

        return NetworkNodeParams(firstLinkEndpoints, lastLinkEndpoints, filterOutConsecutiveDuplicates(interimNodes))
    }
}
