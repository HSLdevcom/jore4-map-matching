package fi.hsl.jore4.mapmatching.service.matching

import fi.hsl.jore4.mapmatching.model.VehicleMode
import fi.hsl.jore4.mapmatching.model.VehicleType
import fi.hsl.jore4.mapmatching.model.matching.RoutePoint
import fi.hsl.jore4.mapmatching.repository.infrastructure.SnappedLinkState
import fi.hsl.jore4.mapmatching.util.CollectionUtils.filterOutConsecutiveDuplicates
import mu.KotlinLogging
import org.geolatte.geom.G2D
import org.geolatte.geom.Point

private val LOGGER = KotlinLogging.logger {}

object MatchingServiceHelper {

    fun validateInputForRouteMatching(routePoints: List<RoutePoint>, vehicleType: VehicleType): String? {
        if (vehicleType.vehicleMode != VehicleMode.BUS)
            return "Only bus infrastructure is currently supported in map-matching"

        if (!hasAtLeastTwoDistinctRoutePointLocations(routePoints))
            return "At least 2 distinct locations within route points must be given"

        return null
    }

    private fun hasAtLeastTwoDistinctRoutePointLocations(routePoints: List<RoutePoint>): Boolean {
        val routePointLocations: List<Point<G2D>> = routePoints.map(RoutePoint::location)

        return filterOutConsecutiveDuplicates(routePointLocations).size >= 2
    }

    internal fun resolveTerminusLinkIfStopPoint(routeTerminusPoint: SourceRouteTerminusPoint,
                                                fromStopNationalIdToInfrastructureLink: Map<Int, SnappedLinkState>)
        : SnappedLinkState? {

        return routeTerminusPoint.run {
            if (isStopPoint) {
                when (stopPointNationalId) {
                    null -> {
                        LOGGER.debug { "Public transport stop for route $terminusType point is not given national ID" }
                        null
                    }
                    else -> {
                        fromStopNationalIdToInfrastructureLink[stopPointNationalId]
                            ?.also { link ->
                                LOGGER.debug {
                                    "Resolved infrastructureLinkId=${link.infrastructureLinkId} as $terminusType " +
                                        "link candidate from public transport stop matched with " +
                                        "nationalId=$stopPointNationalId"
                                }
                            }
                            ?: run {
                                LOGGER.debug {
                                    "Could not resolve public transport stop for route $terminusType point by " +
                                        "national ID: $stopPointNationalId"
                                }
                                null
                            }
                    }
                }
            } else {
                LOGGER.debug { "Route $terminusType point is not stop point" }
                null
            }
        }
    }
}
