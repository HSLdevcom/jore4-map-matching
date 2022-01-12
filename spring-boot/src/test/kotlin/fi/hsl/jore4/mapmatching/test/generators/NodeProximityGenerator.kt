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
import org.quicktheories.core.Gen

object NodeProximityGenerator {

    fun node(): Gen<NodeProximity> =
        infrastructureNodeId().zip(nonNegativeDistance()) { id, distanceToNode -> NodeProximity(id, distanceToNode) }

    fun distinctNodePair(): Gen<Pair<NodeProximity, NodeProximity>> {
        return infrastructureNodeIdPair().zip(nodeDistancePair()) { (node1Id, node2Id),
                                                                    (distance1, distance2) ->

            Pair(NodeProximity(node1Id, distance1),
                 NodeProximity(node2Id, distance2))
        }
    }

    fun endpointNodesOfInfrastructureLink(nodeProximityFilter: LinkEndpointsProximityFilter)
        : Gen<Pair<NodeProximity, NodeProximity>> {

        return nodeDistancePair(nodeProximityFilter)
            .zip(infrastructureNodeIdPair()) { (distanceToFirstNode, distanceToSecondNode),
                                               (firstNodeId, secondNodeId) ->

                Pair(NodeProximity(firstNodeId, distanceToFirstNode),
                     NodeProximity(secondNodeId, distanceToSecondNode))
            }
    }

    fun distinctNodeTriple(): Gen<Triple<NodeProximity, NodeProximity, NodeProximity>> {
        return infrastructureNodeIdTriple()
            .zip(nodeDistanceTriple()) { (node1Id, node2Id, node3Id),
                                         (distance1, distance2, distance3) ->

                Triple(NodeProximity(node1Id, distance1),
                       NodeProximity(node2Id, distance2),
                       NodeProximity(node3Id, distance3))
            }
    }

    fun distinctNodeQuadruple(): Gen<Quadruple<NodeProximity, NodeProximity, NodeProximity, NodeProximity>> {
        return infrastructureNodeIdQuadruple()
            .zip(nodeDistanceQuadruple()) { (node1Id, node2Id, node3Id, node4Id),
                                            (distance1, distance2, distance3, distance4) ->

                Quadruple(NodeProximity(node1Id, distance1),
                          NodeProximity(node2Id, distance2),
                          NodeProximity(node3Id, distance3),
                          NodeProximity(node4Id, distance4))
            }
    }
}
