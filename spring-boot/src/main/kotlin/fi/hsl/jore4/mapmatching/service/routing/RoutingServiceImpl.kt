package fi.hsl.jore4.mapmatching.service.routing

import fi.hsl.jore4.mapmatching.model.LatLng
import fi.hsl.jore4.mapmatching.model.PathTraversal
import fi.hsl.jore4.mapmatching.repository.infrastructure.ILinkRepository
import fi.hsl.jore4.mapmatching.repository.infrastructure.SnapToLinkDTO
import fi.hsl.jore4.mapmatching.repository.routing.IRoutingRepository
import fi.hsl.jore4.mapmatching.repository.routing.RouteLinkDTO
import fi.hsl.jore4.mapmatching.service.routing.RoutingServiceHelper.createNetworkNodeParams
import fi.hsl.jore4.mapmatching.service.routing.RoutingServiceHelper.findUnmatchedCoordinates
import fi.hsl.jore4.mapmatching.service.routing.response.RoutingFailureDTO
import fi.hsl.jore4.mapmatching.service.routing.response.RoutingResponse
import fi.hsl.jore4.mapmatching.service.routing.response.RoutingResultTransformer
import fi.hsl.jore4.mapmatching.util.CollectionUtils.filterOutConsecutiveDuplicates
import fi.hsl.jore4.mapmatching.util.LogUtils.joinToLogString
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.util.SortedMap

@Service
class RoutingServiceImpl @Autowired constructor(val linkRepository: ILinkRepository,
                                                val routingRepository: IRoutingRepository)
    : IRoutingService {

    @Transactional(readOnly = true)
    override fun findRoute(coordinates: List<LatLng>, linkQueryDistance: Int): RoutingResponse {
        val filteredCoords = filterOutConsecutiveDuplicates(coordinates)

        if (filteredCoords.distinct().size < 2) {
            return RoutingFailureDTO.invalidValue("At least 2 distinct coordinates must be given")
        }

        val closestLinksResult: SortedMap<Int, SnapToLinkDTO> = linkRepository
            .findClosestLinks(filteredCoords, linkQueryDistance.toDouble())
            .toSortedMap()

        if (LOGGER.isDebugEnabled) {
            LOGGER.debug("Found closest links within $linkQueryDistance m radius: {}",
                         joinToLogString(closestLinksResult.entries) {
                             "Coordinate #${it.key}: ${it.value}"
                         })
        }

        val closestLinks: Collection<SnapToLinkDTO> = closestLinksResult.values

        if (closestLinks.size < filteredCoords.size) {
            return RoutingFailureDTO.noSegment(findUnmatchedCoordinates(closestLinks, filteredCoords))
        }

        val routeLinks: List<RouteLinkDTO> =
            routingRepository.findRouteViaNetworkNodes(createNetworkNodeParams(closestLinks))

        if (LOGGER.isDebugEnabled) {
            LOGGER.debug("Got route links: {}", joinToLogString(routeLinks))
        }

        val traversedPaths: List<PathTraversal> = routeLinks.map { it.path }

        return RoutingResultTransformer.createResponse(traversedPaths)
    }

    companion object {
        private val LOGGER: Logger = LoggerFactory.getLogger(RoutingServiceImpl::class.java)
    }
}