package fi.hsl.jore4.mapmatching.service.routing

import fi.hsl.jore4.mapmatching.model.NodeIdSequence
import fi.hsl.jore4.mapmatching.model.PathTraversal
import fi.hsl.jore4.mapmatching.model.VehicleType
import fi.hsl.jore4.mapmatching.repository.infrastructure.ILinkRepository
import fi.hsl.jore4.mapmatching.repository.infrastructure.SnapPointToLinkDTO
import fi.hsl.jore4.mapmatching.repository.routing.IRoutingRepository
import fi.hsl.jore4.mapmatching.repository.routing.RouteLinkDTO
import fi.hsl.jore4.mapmatching.service.common.response.RoutingResponse
import fi.hsl.jore4.mapmatching.service.common.response.RoutingResponseCreator
import fi.hsl.jore4.mapmatching.service.node.INodeServiceInternal
import fi.hsl.jore4.mapmatching.service.node.NodeSequenceAlternatives
import fi.hsl.jore4.mapmatching.service.routing.RoutingServiceHelper.createNodeSequenceAlternatives
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

        val nodeIdSeq: NodeIdSequence

        try {
            nodeIdSeq = resolveNetworkNodeIds(closestLinks, vehicleType)
        } catch (ex: Exception) {
            val errMessage: String = ex.message ?: "Failure while resolving infrastructure network nodes"
            LOGGER.warn(errMessage)
            return RoutingResponse.noSegment(errMessage)
        }

        val traversedPaths: List<PathTraversal> = findRoute(nodeIdSeq, vehicleType)

        return RoutingResponseCreator.create(traversedPaths)
    }

    private fun findClosestInfrastructureLinks(points: List<Point<G2D>>,
                                               vehicleType: VehicleType,
                                               linkQueryDistance: Int)
        : Collection<SnapPointToLinkDTO> {

        val closestLinksResult: SortedMap<Int, SnapPointToLinkDTO> = linkRepository
            .findClosestLinks(points, vehicleType, linkQueryDistance.toDouble())
            .toSortedMap()

        if (LOGGER.isDebugEnabled) {
            LOGGER.debug("Found closest links within $linkQueryDistance m radius: {}",
                         joinToLogString(closestLinksResult.entries) {
                             "Point #${it.key}: ${it.value}"
                         })
        }

        return closestLinksResult.values
    }

    /**
     * @throws [IllegalStateException]
     */
    private fun resolveNetworkNodeIds(closestLinks: Collection<SnapPointToLinkDTO>,
                                      vehicleType: VehicleType)
        : NodeIdSequence {

        val nodeSequenceAlternatives: NodeSequenceAlternatives = createNodeSequenceAlternatives(closestLinks)
        val nodeIdSeq: NodeIdSequence = nodeService.resolveNodeIdSequence(nodeSequenceAlternatives, vehicleType)

        if (LOGGER.isDebugEnabled) {
            LOGGER.debug("Resolved params ${nodeSequenceAlternatives.toCompactString()} to $nodeIdSeq")
        }

        return nodeIdSeq
    }

    fun findRoute(nodeIdSequence: NodeIdSequence, vehicleType: VehicleType) : List<PathTraversal> {
        val routeLinks: List<RouteLinkDTO> = routingRepository.findRouteViaNetworkNodes(nodeIdSequence, vehicleType)

        if (LOGGER.isDebugEnabled) {
            LOGGER.debug("Got route links for $nodeIdSequence: {}", joinToLogString(routeLinks))
        }

        return routeLinks.map { it.path }
    }

    companion object {
        private val LOGGER: Logger = LoggerFactory.getLogger(RoutingServiceImpl::class.java)
    }
}
