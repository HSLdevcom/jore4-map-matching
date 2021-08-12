package fi.hsl.jore4.mapmatching.service.routing

import fi.hsl.jore4.mapmatching.model.LatLng
import fi.hsl.jore4.mapmatching.model.PathTraversal
import fi.hsl.jore4.mapmatching.model.VehicleType
import fi.hsl.jore4.mapmatching.model.tables.records.PublicTransportStopRecord
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

    internal fun filterStopsByDirectionOfTraversal(allStopsAlongLinks: List<PublicTransportStopRecord>,
                                                   paths: List<PathTraversal>): List<PublicTransportStopRecord> {

        val uniqueLinkIds: Set<Long> = paths.map { it.infrastructureLinkId }.toSet()

        // Verify mutual consistency of given parameters.
        allStopsAlongLinks.forEach {
            if (!uniqueLinkIds.contains(it.locatedOnInfrastructureLinkId)) {
                throw IllegalArgumentException(
                    "Inconsistent parameters: encountered a public transport stop not along traversed links")
            }
        }

        val stopsByLinkId: Map<Long, List<PublicTransportStopRecord>> =
            allStopsAlongLinks.groupBy { it.locatedOnInfrastructureLinkId }

        return paths.flatMap { path ->
            val linkTraversedForwards = path.alongLinkDirection

            stopsByLinkId
                .getOrDefault(path.infrastructureLinkId, emptyList())
                .filter { stop ->
                    when (stop.isOnDirectionOfLinkForwardTraversal) {
                        true -> linkTraversedForwards
                        false -> !linkTraversedForwards
                        null -> false
                    }
                }
                .sortedWith(compareBy {
                    if (linkTraversedForwards)
                        it.distanceFromLinkStartInMeters
                    else
                        -it.distanceFromLinkStartInMeters
                })
        }
    }
}
