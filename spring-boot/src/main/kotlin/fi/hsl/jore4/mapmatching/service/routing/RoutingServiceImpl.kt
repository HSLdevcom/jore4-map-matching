package fi.hsl.jore4.mapmatching.service.routing

import fi.hsl.jore4.mapmatching.model.InfrastructureLinkTraversal
import fi.hsl.jore4.mapmatching.model.NodeIdSequence
import fi.hsl.jore4.mapmatching.model.VehicleType
import fi.hsl.jore4.mapmatching.repository.infrastructure.ILinkRepository
import fi.hsl.jore4.mapmatching.repository.infrastructure.SnapPointToLinkDTO
import fi.hsl.jore4.mapmatching.service.common.IRoutingServiceInternal
import fi.hsl.jore4.mapmatching.service.common.response.RoutingResponse
import fi.hsl.jore4.mapmatching.service.common.response.RoutingResponseCreator
import fi.hsl.jore4.mapmatching.service.node.INodeServiceInternal
import fi.hsl.jore4.mapmatching.service.node.NodeSequenceAlternatives
import fi.hsl.jore4.mapmatching.service.routing.RoutingServiceHelper.createNodeSequenceAlternatives
import fi.hsl.jore4.mapmatching.service.routing.RoutingServiceHelper.findUnmatchedPoints
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
                                                val nodeService: INodeServiceInternal,
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

        val nodeIdSeq: NodeIdSequence

        try {
            nodeIdSeq = resolveNetworkNodeIds(closestLinks, vehicleType)
        } catch (ex: Exception) {
            val errMessage: String = ex.message ?: "Failure while resolving infrastructure network nodes"
            LOGGER.warn(errMessage)
            return RoutingResponse.noSegment(errMessage)
        }

        val traversedLinks: List<InfrastructureLinkTraversal> = routingServiceInternal.findRoute(nodeIdSeq, vehicleType)

        return RoutingResponseCreator.create(traversedLinks)
    }

    private fun findClosestInfrastructureLinks(points: List<Point<G2D>>,
                                               vehicleType: VehicleType,
                                               linkQueryDistance: Int)
        : Collection<SnapPointToLinkDTO> {

        val closestLinksResult: SortedMap<Int, SnapPointToLinkDTO> = linkRepository
            .findClosestLinks(points, vehicleType, linkQueryDistance.toDouble())
            .toSortedMap()

        LOGGER.debug {
            "Found closest links within $linkQueryDistance m radius: ${
                joinToLogString(closestLinksResult.entries) {
                    "Point #${it.key}: ${it.value}"
                }
            }"
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

        require(nodeSequenceAlternatives.isRoutePossible()) {
            "Cannot produce route based on single infrastructure node"
        }

        return nodeService
            .resolveNodeIdSequence(nodeSequenceAlternatives, vehicleType)
            .also { nodeIdSeq: NodeIdSequence ->
                LOGGER.debug {
                    "Resolved node resolution params ${nodeSequenceAlternatives.prettyPrint()} to nodes $nodeIdSeq"
                }
            }
    }
}
