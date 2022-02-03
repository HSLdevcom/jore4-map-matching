package fi.hsl.jore4.mapmatching.service.matching

import fi.hsl.jore4.mapmatching.model.VehicleMode
import fi.hsl.jore4.mapmatching.model.VehicleType
import fi.hsl.jore4.mapmatching.model.matching.RoutePoint
import fi.hsl.jore4.mapmatching.model.matching.RoutePointType.PUBLIC_TRANSPORT_STOP
import fi.hsl.jore4.mapmatching.repository.infrastructure.SnapPointToLinkDTO
import fi.hsl.jore4.mapmatching.repository.infrastructure.SnappedLinkState
import fi.hsl.jore4.mapmatching.model.matching.TerminusType
import fi.hsl.jore4.mapmatching.util.CollectionUtils.filterOutConsecutiveDuplicates
import org.geolatte.geom.G2D
import org.geolatte.geom.Point
import org.slf4j.Logger
import org.slf4j.LoggerFactory

object MatchingServiceHelper {

    private val LOGGER: Logger = LoggerFactory.getLogger(MatchingServiceHelper::class.java)

    fun validateInputForRouteMatching(routePoints: List<RoutePoint>, vehicleType: VehicleType): String? {
        if (vehicleType.vehicleMode != VehicleMode.BUS)
            return "Only bus infrastructure is currently supported in map-matching"

        if (!hasAtLeastTwoDistinctRoutePointLocations(routePoints))
            return "At least 2 distinct locations within route points must be given"

        val allRoutePointsValid = routePoints.all(RoutePoint::isStopPointInfoPresentOnlyIfTypeIsPublicTransportStop)

        if (!allRoutePointsValid)
            return "Found invalid route point having stopPointInfo present but type not ${PUBLIC_TRANSPORT_STOP.name}"

        return null
    }

    private fun hasAtLeastTwoDistinctRoutePointLocations(routePoints: List<RoutePoint>): Boolean {
        val routePointLocations: List<Point<G2D>> = routePoints.map(RoutePoint::location)

        return filterOutConsecutiveDuplicates(routePointLocations).size >= 2
    }

    internal fun resolveTerminusLinkIfStopPoint(routePoint: RoutePoint,
                                                terminusType: TerminusType,
                                                fromStopNationalIdToInfrastructureLink: Map<Int, SnappedLinkState>)
        : SnappedLinkState? {

        return when (routePoint.isStopPoint) {
            true -> {
                val nationalId: Int? = routePoint.stopPointInfo?.nationalId

                fromStopNationalIdToInfrastructureLink[nationalId]
                    ?.let { link ->
                        if (LOGGER.isDebugEnabled) {
                            LOGGER.debug("Resolved infrastructureLinkId=${link.infrastructureLinkId} as route "
                                             + "$terminusType link from public transport stop matched with "
                                             + "nationalId=$nationalId")
                        }

                        link
                    }
                    ?: run {
                        if (LOGGER.isDebugEnabled) {
                            LOGGER.debug("Could not resolve public transport stop for route $terminusType point by "
                                             + "national ID: $nationalId")
                        }
                        null
                    }
            }
            false -> {
                if (LOGGER.isDebugEnabled) {
                    LOGGER.debug("Route $terminusType point is not stop point")
                }
                null
            }
        }
    }

    /**
     * @throws [IllegalStateException]
     */
    internal fun getTerminusLinkOrThrowException(linkSearchResult: SnapPointToLinkDTO?,
                                                 terminusType: TerminusType,
                                                 routePointLocation: Point<G2D>,
                                                 vehicleType: VehicleType,
                                                 linkQueryDistance: Double)
        : SnappedLinkState {

        return linkSearchResult
            ?.let {
                val link: SnappedLinkState = it.link

                if (LOGGER.isDebugEnabled) {
                    LOGGER.debug("Resolved infrastructureLinkId=${link.infrastructureLinkId} as route $terminusType "
                                     + "link by finding the closest link to point=$routePointLocation")
                }

                link
            }
            ?: throw IllegalStateException(
                "Could not find infrastructure link within $linkQueryDistance meter distance from route "
                    + "$terminusType point ($routePointLocation) while applying vehicle type constraint '$vehicleType'")
    }
}
