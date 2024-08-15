package fi.hsl.jore4.mapmatching.service.matching

import fi.hsl.jore4.mapmatching.model.NodeProximity
import fi.hsl.jore4.mapmatching.model.VehicleType
import fi.hsl.jore4.mapmatching.model.matching.RouteJunctionPoint
import fi.hsl.jore4.mapmatching.model.matching.RoutePoint
import fi.hsl.jore4.mapmatching.repository.routing.INodeRepository
import fi.hsl.jore4.mapmatching.repository.routing.SnapPointToNodesResult
import fi.hsl.jore4.mapmatching.util.InternalService
import fi.hsl.jore4.mapmatching.util.LogUtils.joinToLogString
import io.github.oshai.kotlinlogging.KotlinLogging
import org.geolatte.geom.G2D
import org.geolatte.geom.Point
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.transaction.annotation.Transactional

private val LOGGER = KotlinLogging.logger {}

@InternalService
class RoadJunctionMatcherImpl
    @Autowired
    constructor(
        val nodeRepository: INodeRepository,
    ) : IRoadJunctionMatcher {
        @Transactional(readOnly = true, noRollbackFor = [RuntimeException::class])
        override fun findInfrastructureNodesMatchingRoadJunctions(
            routePoints: List<RoutePoint>,
            vehicleType: VehicleType,
            matchDistance: Double,
            clearingDistance: Double,
        ): Map<Int, NodeProximity?> {
            require(matchDistance <= clearingDistance) { "matchDistance must not be greater than clearingDistance" }

            val junctionPointsWithRoutePointOrdering: List<IndexedValue<RoutePoint>> =
                routePoints
                    .withIndex()
                    .filter { it.value is RouteJunctionPoint }

            val fromJunctionPointOneBasedIndexToRoutePointIndex: Map<Int, Int> =
                junctionPointsWithRoutePointOrdering
                    .map(IndexedValue<*>::index)
                    .withIndex()
                    .associateBy(keySelector = { it.index + 1 }, valueTransform = IndexedValue<Int>::value)

            val pointCoordinates: List<Point<G2D>> = junctionPointsWithRoutePointOrdering.map { it.value.location }

            // Fetch all nodes that are located within clearing distance from some source route point.
            val nClosestNodes: Map<Int, SnapPointToNodesResult> =
                nodeRepository.findNClosestNodes(
                    pointCoordinates,
                    vehicleType,
                    clearingDistance,
                )

            return nClosestNodes.entries
                .mapNotNull { (junctionPointOneBasedIndex: Int, snap: SnapPointToNodesResult) ->

                    val routePointIndex: Int = fromJunctionPointOneBasedIndexToRoutePointIndex[junctionPointOneBasedIndex]!!

                    val nodes: List<NodeProximity> = snap.nodes

                    // Infrastructure node is accepted as a match if:
                    // 1. it is within match distance (small circle) from a source route point
                    // 2. there does not exist any other node within clearing distance (larger circle)
                    when (nodes.size) {
                        1 -> {
                            val node: NodeProximity = nodes[0]

                            if (node.distanceToNode <= matchDistance) {
                                routePointIndex to node
                            } else {
                                null // discard because node not within match distance
                            }
                        }

                        else -> null // discard because multiple nodes within clearing distance
                    }
                }.also { routePointIndexToJunctionNode: List<Pair<Int, NodeProximity>> ->
                    LOGGER.debug {
                        "Matched following road junction points from source route points: ${
                            joinToLogString(routePointIndexToJunctionNode) {
                                "Route point #${it.first + 1}: ${it.second}"
                            }
                        }"
                    }
                }.toMap()
        }
    }
