package fi.hsl.jore4.mapmatching.service.routing

import fi.hsl.jore4.mapmatching.model.PathTraversal
import fi.hsl.jore4.mapmatching.model.VehicleType
import fi.hsl.jore4.mapmatching.repository.infrastructure.ILinkRepository
import fi.hsl.jore4.mapmatching.repository.infrastructure.SnapPointToLinkDTO
import fi.hsl.jore4.mapmatching.repository.routing.IRoutingRepository
import fi.hsl.jore4.mapmatching.repository.routing.RouteLinkDTO
import fi.hsl.jore4.mapmatching.service.common.response.RoutingResponse
import fi.hsl.jore4.mapmatching.service.common.response.RoutingResponseCreator
import fi.hsl.jore4.mapmatching.service.node.INodeServiceInternal
import fi.hsl.jore4.mapmatching.service.node.NodeSequenceProducer
import fi.hsl.jore4.mapmatching.service.routing.RoutingServiceHelper.createNodeSequenceProducer
import fi.hsl.jore4.mapmatching.service.routing.RoutingServiceHelper.findUnmatchedPoints
import fi.hsl.jore4.mapmatching.util.CollectionUtils.filterOutConsecutiveDuplicates
import fi.hsl.jore4.mapmatching.util.LogUtils.joinToLogString
import org.geolatte.geom.G2D
import org.geolatte.geom.Point
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
    override fun findRoute(viaPoints: List<Point<G2D>>,
                           vehicleType: VehicleType,
                           linkQueryDistance: Int): RoutingResponse {

        val filteredPoints = filterOutConsecutiveDuplicates(viaPoints)

        if (filteredPoints.distinct().size < 2) {
            return RoutingResponse.invalidValue("At least 2 distinct points must be given")
        }

        val closestLinksResult: SortedMap<Int, SnapPointToLinkDTO> = linkRepository
            .findClosestLinks(filteredPoints, vehicleType, linkQueryDistance.toDouble())
            .toSortedMap()

        if (LOGGER.isDebugEnabled) {
            LOGGER.debug("Found closest links within $linkQueryDistance m radius: {}",
                         joinToLogString(closestLinksResult.entries) {
                             "Point #${it.key}: ${it.value}"
                         })
        }

        val closestLinks: Collection<SnapPointToLinkDTO> = closestLinksResult.values

        if (closestLinks.size < filteredPoints.size) {
            return RoutingResponse.noSegment(findUnmatchedPoints(closestLinks, filteredPoints))
        }

        val nodeSequenceProducer: NodeSequenceProducer = createNodeSequenceProducer(closestLinks)
        val nodeIds: List<Long> = nodeService.resolveNodeSequence(nodeSequenceProducer, vehicleType)

        if (LOGGER.isDebugEnabled) {
            LOGGER.debug("Resolved params ${nodeSequenceProducer.toCompactString()} to node sequence $nodeIds")
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
