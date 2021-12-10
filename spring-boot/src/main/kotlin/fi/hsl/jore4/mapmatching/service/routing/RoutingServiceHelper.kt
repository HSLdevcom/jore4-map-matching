package fi.hsl.jore4.mapmatching.service.routing

import fi.hsl.jore4.mapmatching.model.LatLng
import fi.hsl.jore4.mapmatching.model.VehicleType
import fi.hsl.jore4.mapmatching.repository.infrastructure.SnapPointToLinkDTO
import fi.hsl.jore4.mapmatching.repository.infrastructure.SnappedLinkState
import fi.hsl.jore4.mapmatching.service.node.NodeSequenceProducer

object RoutingServiceHelper {

    internal fun findUnmatchedCoordinates(snaps: Collection<SnapPointToLinkDTO>,
                                          allCoordinates: List<LatLng>)
        : List<LatLng> {

        val snappedCoordinates = snaps.map { it.point }.toSet()

        return allCoordinates.filter { !snappedCoordinates.contains(it) }
    }

    internal fun createNodeSequenceProducer(snaps: Collection<SnapPointToLinkDTO>): NodeSequenceProducer {
        val links: List<SnappedLinkState> = snaps.map { it.link }

        return NodeSequenceProducer(links)
    }
}
