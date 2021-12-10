package fi.hsl.jore4.mapmatching.service.routing

import fi.hsl.jore4.mapmatching.model.LatLng
import fi.hsl.jore4.mapmatching.model.PathTraversal
import fi.hsl.jore4.mapmatching.model.VehicleType
import fi.hsl.jore4.mapmatching.repository.infrastructure.ILinkRepository
import fi.hsl.jore4.mapmatching.repository.infrastructure.SnapPointToLinkDTO
import fi.hsl.jore4.mapmatching.repository.routing.IRoutingRepository
import fi.hsl.jore4.mapmatching.repository.routing.RouteLinkDTO
import fi.hsl.jore4.mapmatching.service.common.response.RoutingResponse
import fi.hsl.jore4.mapmatching.service.common.response.RoutingResponseCreator
import fi.hsl.jore4.mapmatching.service.node.INodeServiceInternal
import fi.hsl.jore4.mapmatching.service.node.NodeResolutionParams
import fi.hsl.jore4.mapmatching.service.routing.RoutingServiceHelper.createNodeResolutionParams
import fi.hsl.jore4.mapmatching.service.routing.RoutingServiceHelper.findUnmatchedCoordinates
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
                                                val routingRepository: IRoutingRepository,
                                                val nodeService: INodeServiceInternal)
    : IRoutingService {

    @Transactional(readOnly = true)
    override fun findRoute(coordinates: List<LatLng>,
                           vehicleType: VehicleType,
                           linkQueryDistance: Int): RoutingResponse {

        val filteredCoords = filterOutConsecutiveDuplicates(coordinates)

        if (filteredCoords.distinct().size < 2) {
            return RoutingResponse.invalidValue("At least 2 distinct coordinates must be given")
        }

        val closestLinksResult: SortedMap<Int, SnapPointToLinkDTO> = linkRepository
            .findClosestLinks(filteredCoords, vehicleType, linkQueryDistance.toDouble())
            .toSortedMap()

        if (LOGGER.isDebugEnabled) {
            LOGGER.debug("Found closest links within $linkQueryDistance m radius: {}",
                         joinToLogString(closestLinksResult.entries) {
                             "Coordinate #${it.key}: ${it.value}"
                         })
        }

        val closestLinks: Collection<SnapPointToLinkDTO> = closestLinksResult.values

        if (closestLinks.size < filteredCoords.size) {
            return RoutingResponse.noSegment(findUnmatchedCoordinates(closestLinks, filteredCoords))
        }

        val nodeParams: NodeResolutionParams = createNodeResolutionParams(closestLinks, vehicleType)
        val nodeIds: List<Long> = nodeService.resolveNodeSequence(nodeParams)

        if (LOGGER.isDebugEnabled) {
            LOGGER.debug("Resolved params ${nodeParams.toCompactString()} to node sequence $nodeIds")
        }

        val routeLinks: List<RouteLinkDTO> = routingRepository.findRouteViaNetworkNodes(nodeIds, vehicleType)

        if (LOGGER.isDebugEnabled) {
            LOGGER.debug("Got route links for node sequence $nodeIds: {}", joinToLogString(routeLinks))
        }

        val traversedPaths: List<PathTraversal> = routeLinks.map { it.path }

        return RoutingResponseCreator.create(traversedPaths)
    }

    companion object {
        private val LOGGER: Logger = LoggerFactory.getLogger(RoutingServiceImpl::class.java)
    }
}
