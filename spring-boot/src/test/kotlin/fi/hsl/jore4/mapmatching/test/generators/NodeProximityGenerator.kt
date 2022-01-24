package fi.hsl.jore4.mapmatching.test.generators

import fi.hsl.jore4.mapmatching.model.NodeProximity
import fi.hsl.jore4.mapmatching.test.generators.DistanceToNodeGenerator.nodeDistancePair
import fi.hsl.jore4.mapmatching.test.generators.DistanceToNodeGenerator.nodeDistanceQuadruple
import fi.hsl.jore4.mapmatching.test.generators.DistanceToNodeGenerator.nodeDistanceTriple
import fi.hsl.jore4.mapmatching.test.generators.DistanceToNodeGenerator.nonNegativeDistance
import fi.hsl.jore4.mapmatching.test.generators.InfrastructureNodeIdGenerator.infrastructureNodeId
import fi.hsl.jore4.mapmatching.test.generators.InfrastructureNodeIdGenerator.infrastructureNodeIdPair
import fi.hsl.jore4.mapmatching.test.generators.InfrastructureNodeIdGenerator.infrastructureNodeIdQuadruple
import fi.hsl.jore4.mapmatching.test.generators.InfrastructureNodeIdGenerator.infrastructureNodeIdTriple
import fi.hsl.jore4.mapmatching.test.util.Quadruple
import fi.hsl.jore4.mapmatching.util.MathUtils.isZero
import org.quicktheories.core.Gen
import org.quicktheories.generators.Generate.booleans
import kotlin.math.min

object NodeProximityGenerator {

    fun node(): Gen<NodeProximity> =
        infrastructureNodeId().zip(nonNegativeDistance()) { id, distanceToNode -> NodeProximity(id, distanceToNode) }

    fun discreteNodePair(): Gen<Pair<NodeProximity, NodeProximity>> {
        return infrastructureNodeIdPair().zip(nodeDistancePair()) { (node1Id, node2Id),
                                                                    (distance1, distance2) ->

            Pair(NodeProximity(node1Id, distance1),
                 NodeProximity(node2Id, distance2))
        }
    }

    fun singleNodeAtTwoDistances(): Gen<Pair<NodeProximity, NodeProximity>> {
        return infrastructureNodeId().zip(nodeDistancePair()) { nodeId,
                                                                (distance1, distance2) ->

            // Because of same node ID, if one distance is zero, then the other must be as well.

            val anyZeroDistance: Boolean = isZero(min(distance1, distance2))

            Pair(NodeProximity(nodeId, if (anyZeroDistance) 0.0 else distance1),
                 NodeProximity(nodeId, if (anyZeroDistance) 0.0 else distance2))
        }
    }

    fun discreteEndpointNodesForInfrastructureLink(nodeProximityFilter: LinkEndpointsProximityFilter)
        : Gen<Pair<NodeProximity, NodeProximity>> {

        return nodeDistancePair(nodeProximityFilter)
            .zip(infrastructureNodeIdPair()) { (distanceToFirstNode, distanceToSecondNode),
                                               (firstNodeId, secondNodeId) ->

                Pair(NodeProximity(firstNodeId, distanceToFirstNode),
                     NodeProximity(secondNodeId, distanceToSecondNode))
            }
    }

    fun oneNodeAsBothEndpointsForInfrastructureLink(nodeProximityFilter: LinkEndpointsProximityFilter)
        : Gen<Pair<NodeProximity, NodeProximity>> {

        return infrastructureNodeId()
            .zip(nodeDistancePair(nodeProximityFilter)) { nodeId, (distanceToStartNode, distanceToEndNode) ->

                Pair(NodeProximity(nodeId, distanceToStartNode),
                     NodeProximity(nodeId, distanceToEndNode))
            }
    }

    fun discreteNodeTriple(): Gen<Triple<NodeProximity, NodeProximity, NodeProximity>> {
        return infrastructureNodeIdTriple()
            .zip(nodeDistanceTriple()) { (node1Id, node2Id, node3Id),
                                         (distance1, distance2, distance3) ->

                Triple(NodeProximity(node1Id, distance1),
                       NodeProximity(node2Id, distance2),
                       NodeProximity(node3Id, distance3))
            }
    }

    fun nodeTriple(numberOfDiscreteNodes: Int): Gen<Triple<NodeProximity, NodeProximity, NodeProximity>> {
        return when (numberOfDiscreteNodes) {
            1 -> infrastructureNodeId()
                .zip(nodeDistanceTriple()) { nodeId,
                                             (distance1, distance2, distance3) ->

                    // Because of same node ID, if one distance is zero, then the others must be as well.

                    val anyZeroDistance: Boolean = isZero(min(distance1, min(distance2, distance3)))

                    Triple(NodeProximity(nodeId, if (anyZeroDistance) 0.0 else distance1),
                           NodeProximity(nodeId, if (anyZeroDistance) 0.0 else distance2),
                           NodeProximity(nodeId, if (anyZeroDistance) 0.0 else distance3))
                }
            2 -> infrastructureNodeIdPair()
                .zip(booleans(),
                     nodeDistanceTriple()) { (node1Id, node2Id),
                                             useFirstNodeIdTwice,
                                             (distance1, distance2, distance3) ->

                    Triple(NodeProximity(node1Id, distance1),
                           NodeProximity(if (useFirstNodeIdTwice) node1Id else node2Id, distance2),
                           NodeProximity(node2Id, distance3))
                }
            3 -> discreteNodeTriple()
            else -> throw IllegalArgumentException("numberOfDiscreteNodes should be in range 1..3, but was: $numberOfDiscreteNodes")
        }
    }

    fun discreteNodeQuadruple(): Gen<Quadruple<NodeProximity, NodeProximity, NodeProximity, NodeProximity>> {
        return infrastructureNodeIdQuadruple()
            .zip(nodeDistanceQuadruple()) { (node1Id, node2Id, node3Id, node4Id),
                                            (distance1, distance2, distance3, distance4) ->

                Quadruple(NodeProximity(node1Id, distance1),
                          NodeProximity(node2Id, distance2),
                          NodeProximity(node3Id, distance3),
                          NodeProximity(node4Id, distance4))
            }
    }

    fun nodeQuadruple(numberOfDiscreteNodes: Int): Gen<Quadruple<NodeProximity, NodeProximity, NodeProximity, NodeProximity>> {
        return when (numberOfDiscreteNodes) {
            1 -> infrastructureNodeId()
                .zip(nodeDistanceQuadruple()) { nodeId,
                                                (distance1, distance2, distance3, distance4) ->

                    // Because of same node ID, if one distance is zero, then the others must be as well.

                    val minDistance: Double = min(distance1, min(distance2, min(distance3, distance4)))
                    val anyZeroDistance: Boolean = isZero(minDistance)

                    Quadruple(NodeProximity(nodeId, if (anyZeroDistance) 0.0 else distance1),
                              NodeProximity(nodeId, if (anyZeroDistance) 0.0 else distance2),
                              NodeProximity(nodeId, if (anyZeroDistance) 0.0 else distance3),
                              NodeProximity(nodeId, if (anyZeroDistance) 0.0 else distance4))
                }
            2 -> infrastructureNodeIdPair()
                .zip(nodeDistanceQuadruple()) { (node1Id, node2Id),
                                                (distance1, distance2, distance3, distance4) ->

                    val anyZeroDistance1stHalf: Boolean = isZero(min(distance1, distance2))
                    val anyZeroDistance2ndHalf: Boolean = isZero(min(distance3, distance4))

                    Quadruple(NodeProximity(node1Id, if (anyZeroDistance1stHalf) 0.0 else distance1),
                              NodeProximity(node1Id, if (anyZeroDistance1stHalf) 0.0 else distance2),
                              NodeProximity(node2Id, if (anyZeroDistance2ndHalf) 0.0 else distance3),
                              NodeProximity(node2Id, if (anyZeroDistance2ndHalf) 0.0 else distance4))
                }
            3 -> infrastructureNodeIdTriple()
                .zip(booleans(),
                     nodeDistanceQuadruple()) { (node1Id, node2Id, node3Id),
                                                duplicateNodeIdInFirstHalf,
                                                (distance1, distance2, distance3, distance4) ->

                    val (secondNodeId, thirdNodeId) =
                        if (duplicateNodeIdInFirstHalf) Pair(node1Id, node2Id) else Pair(node2Id, node3Id)

                    val (useZeroDistance1stHalf, useZeroDistance2ndHalf) =
                        if (duplicateNodeIdInFirstHalf)
                            Pair(isZero(min(distance1, distance2)), false)
                        else
                            Pair(false, isZero(min(distance3, distance4)))

                    Quadruple(NodeProximity(node1Id, if (useZeroDistance1stHalf) 0.0 else distance1),
                              NodeProximity(secondNodeId, if (useZeroDistance1stHalf) 0.0 else distance2),
                              NodeProximity(thirdNodeId, if (useZeroDistance2ndHalf) 0.0 else distance3),
                              NodeProximity(node3Id, if (useZeroDistance2ndHalf) 0.0 else distance4))
                }
            4 -> discreteNodeQuadruple()
            else -> throw IllegalArgumentException("numberOfDiscreteNodes should be in range 1..4, but was: $numberOfDiscreteNodes")
        }
    }
}
