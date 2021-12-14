package fi.hsl.jore4.mapmatching.test.generators

import fi.hsl.jore4.mapmatching.test.util.Quadruple
import org.quicktheories.core.Gen
import org.quicktheories.generators.Generate

object InfrastructureNodeIdGenerator {

    private val ID: Gen<Long> = Generate.longRange(100_000, 999_999)

    // Generate pairs of distinct node IDs e.g. for endpoints of single infrastructure link.
    private val ID_PAIR: Gen<Pair<Long, Long>> =
        ID.flatMap { firstNodeId ->
            ID
                .assuming { it != firstNodeId }
                .map { secondNodeId -> Pair(firstNodeId, secondNodeId) }
        }

    // triple of distinct node IDs
    private val ID_TRIPLE: Gen<Triple<Long, Long, Long>> =
        ID_PAIR.flatMap { (node1Id, node2Id) ->
            ID
                .assuming { it != node1Id && it != node2Id }
                .map { node3Id -> Triple(node1Id, node2Id, node3Id) }
        }

    // quadruple of distinct node IDs
    private val ID_QUADRUPLE: Gen<Quadruple<Long, Long, Long, Long>> =
        ID_TRIPLE.flatMap { (node1Id, node2Id, node3Id) ->
            ID
                .assuming { it != node1Id && it != node2Id && it != node3Id }
                .map { node4Id -> Quadruple(node1Id, node2Id, node3Id, node4Id) }
        }

    fun infrastructureNodeId(): Gen<Long> = ID

    fun infrastructureNodeIdPair(): Gen<Pair<Long, Long>> = ID_PAIR

    fun infrastructureNodeIdTriple(): Gen<Triple<Long, Long, Long>> = ID_TRIPLE

    fun infrastructureNodeIdQuadruple(): Gen<Quadruple<Long, Long, Long, Long>> = ID_QUADRUPLE
}
