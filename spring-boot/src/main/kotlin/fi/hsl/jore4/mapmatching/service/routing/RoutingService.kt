package fi.hsl.jore4.mapmatching.service.routing

import fi.hsl.jore4.mapmatching.model.LatLng
import fi.hsl.jore4.mapmatching.repository.infrastructure.LinkRepository
import fi.hsl.jore4.mapmatching.repository.infrastructure.NearestLinkResultDTO
import fi.hsl.jore4.mapmatching.repository.routing.RouteSegmentDTO
import fi.hsl.jore4.mapmatching.repository.routing.RoutingRepository
import fi.hsl.jore4.mapmatching.service.routing.RoutingServiceHelper.createNetworkNodeParams
import fi.hsl.jore4.mapmatching.service.routing.RoutingServiceHelper.findUnmatchedCoordinates
import fi.hsl.jore4.mapmatching.service.routing.RoutingServiceHelper.transformToResponse
import fi.hsl.jore4.mapmatching.service.routing.response.RoutingFailureDTO
import fi.hsl.jore4.mapmatching.service.routing.response.RoutingResponse
import fi.hsl.jore4.mapmatching.util.CollectionUtils.filterOutConsecutiveDuplicates
import fi.hsl.jore4.mapmatching.util.LogUtils.joinToLogString
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service
import java.util.SortedMap

@Service
class RoutingService @Autowired constructor(val linkRepository: LinkRepository,
                                            val routingRepository: RoutingRepository) {

    fun findRoute(coordinates: List<LatLng>, linkSearchRadius: Int): RoutingResponse {
        val filteredCoords = filterOutConsecutiveDuplicates(coordinates)

        if (filteredCoords.distinct().size < 2) {
            return RoutingFailureDTO.invalidValue("At least 2 distinct coordinates must be given")
        }

        val nearestLinksResult: SortedMap<Int, NearestLinkResultDTO> = linkRepository
            .findNearestLinks(filteredCoords, linkSearchRadius)
            .toSortedMap()

        if (LOGGER.isDebugEnabled) {
            LOGGER.debug("Found nearest links within $linkSearchRadius m radius: {}",
                         joinToLogString(nearestLinksResult.entries) {
                             "Coordinate #${it.key}: ${it.value}"
                         })
        }

        val nearestLinks: Collection<NearestLinkResultDTO> = nearestLinksResult.values

        if (nearestLinks.size < filteredCoords.size) {
            return RoutingFailureDTO.noSegment(findUnmatchedCoordinates(nearestLinks, filteredCoords))
        }

        val routingResultSegments: List<RouteSegmentDTO> =
            routingRepository.findRouteViaNetworkNodes(createNetworkNodeParams(nearestLinks))

        if (LOGGER.isDebugEnabled) {
            LOGGER.debug("Got route segments: {}", joinToLogString(routingResultSegments))
        }

        return transformToResponse(routingResultSegments)
    }

    companion object {
        private val LOGGER: Logger = LoggerFactory.getLogger(RoutingService::class.java)
    }
}
