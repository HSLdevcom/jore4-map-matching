package fi.hsl.jore4.mapmatching.service.routing

import fi.hsl.jore4.mapmatching.Constants.SNAP_TO_LINK_ENDPOINT_DISTANCE_IN_METERS
import fi.hsl.jore4.mapmatching.model.VehicleType
import fi.hsl.jore4.mapmatching.repository.infrastructure.ILinkRepository
import fi.hsl.jore4.mapmatching.repository.infrastructure.SnapPointToLinkDTO
import fi.hsl.jore4.mapmatching.repository.routing.PgRoutingPoint
import fi.hsl.jore4.mapmatching.repository.routing.RouteDTO
import fi.hsl.jore4.mapmatching.service.common.IRoutingServiceInternal
import fi.hsl.jore4.mapmatching.service.common.response.RoutingResponse
import fi.hsl.jore4.mapmatching.service.common.response.RoutingResponseCreator
import fi.hsl.jore4.mapmatching.service.routing.RoutingServiceHelper.findUnmatchedPoints
import fi.hsl.jore4.mapmatching.service.routing.RoutingServiceHelper.toPgRoutingPoint
import fi.hsl.jore4.mapmatching.util.CollectionUtils.filterOutConsecutiveDuplicates
import fi.hsl.jore4.mapmatching.util.LogUtils.joinToLogString
import mu.KotlinLogging
import org.geolatte.geom.G2D
import org.geolatte.geom.Point
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.util.SortedMap

private val LOGGER = KotlinLogging.logger {}

@Service
class RoutingServiceImpl @Autowired constructor(val linkRepository: ILinkRepository,
                                                val routingServiceInternal: IRoutingServiceInternal)
    : IRoutingService {

    @Transactional(readOnly = true)
    override fun findRoute(viaPoints: List<Point<G2D>>,
                           vehicleType: VehicleType,
                           linkQueryDistance: Int)
        : RoutingResponse {

        val filteredPoints = filterOutConsecutiveDuplicates(viaPoints)

        if (filteredPoints.distinct().size < 2) {
            return RoutingResponse.invalidValue("At least 2 distinct points must be given")
        }

        val closestLinks: Collection<SnapPointToLinkDTO> =
            findClosestInfrastructureLinks(filteredPoints, vehicleType, linkQueryDistance)

        if (closestLinks.size < filteredPoints.size) {
            return RoutingResponse.noSegment(findUnmatchedPoints(closestLinks, filteredPoints))
        }

        val routePoints: List<PgRoutingPoint> = closestLinks.map { toPgRoutingPoint(it.link) }

        val route: RouteDTO = routingServiceInternal.findRouteViaPoints(routePoints, vehicleType)

        return RoutingResponseCreator.create(route)
    }

    private fun findClosestInfrastructureLinks(points: List<Point<G2D>>,
                                               vehicleType: VehicleType,
                                               linkQueryDistance: Int)
        : List<SnapPointToLinkDTO> {

        val closestLinks: Collection<SnapPointToLinkDTO> = linkRepository
            .findClosestLinks(points, vehicleType, linkQueryDistance.toDouble())
            .toSortedMap()
            .also { sortedResults: SortedMap<Int, SnapPointToLinkDTO> ->
                LOGGER.debug {
                    "Found closest links within $linkQueryDistance m radius: ${
                        joinToLogString(sortedResults.entries) {
                            "Point #${it.key}: ${it.value}"
                        }
                    }"
                }
            }
            .values

        // On the first and last link on route, the location is snapped to terminus node if within close distance.

        fun snapToTerminusNode(snap: SnapPointToLinkDTO): SnapPointToLinkDTO =
            snap.withLocationOnLinkSnappedToTerminusNodeIfWithinDistance(SNAP_TO_LINK_ENDPOINT_DISTANCE_IN_METERS)

        fun firstSnap() = snapToTerminusNode(closestLinks.first())
        fun lastSnap() = snapToTerminusNode(closestLinks.last())

        return when (closestLinks.size) {
            0 -> emptyList()
            1 -> listOf(firstSnap())
            2 -> listOf(firstSnap(), lastSnap())
            else -> listOf(firstSnap()) + closestLinks.drop(1).dropLast(1) + lastSnap()
        }
    }
}
