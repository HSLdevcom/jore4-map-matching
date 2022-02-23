package fi.hsl.jore4.mapmatching.test.generators

import fi.hsl.jore4.mapmatching.model.InfrastructureNodeId
import fi.hsl.jore4.mapmatching.test.generators.CommonGenerators.discretePair
import fi.hsl.jore4.mapmatching.test.generators.CommonGenerators.discreteQuadruple
import fi.hsl.jore4.mapmatching.test.generators.CommonGenerators.discreteTriple
import fi.hsl.jore4.mapmatching.test.util.Quadruple
import org.quicktheories.core.Gen
import org.quicktheories.generators.Generate.booleans
import org.quicktheories.generators.Generate.longRange

object InfrastructureNodeIdGenerator {

    private val ID_VALUE: Gen<Long> = longRange(10_000L, 99_999L)

    fun infrastructureNodeId(): Gen<InfrastructureNodeId> = ID_VALUE.map(::InfrastructureNodeId)

    // Generate pairs of discrete node IDs e.g. for endpoints of single infrastructure link.
    fun discreteNodeIdPair(): Gen<Pair<InfrastructureNodeId, InfrastructureNodeId>> =
        discretePair(infrastructureNodeId())

    // pair of node IDs
    fun nodeIdPair(discrete: Boolean): Gen<Pair<InfrastructureNodeId, InfrastructureNodeId>> = if (discrete)
        discreteNodeIdPair()
    else
        infrastructureNodeId().map { id -> id to id }

    // triple of discrete node IDs
    fun discreteNodeIdTriple(): Gen<Triple<InfrastructureNodeId, InfrastructureNodeId, InfrastructureNodeId>> =
        discreteTriple(infrastructureNodeId())

    // quadruple of discrete node IDs
    fun discreteNodeIdQuadruple()
        : Gen<Quadruple<InfrastructureNodeId, InfrastructureNodeId, InfrastructureNodeId, InfrastructureNodeId>> =
        discreteQuadruple(infrastructureNodeId())

    /**
     * Generate quadruple of node IDs.
     *
     * @param numberOfDiscreteNodes number of discrete node identifiers to be
     * created within a single tuple
     * @param discreteNodesBetweenHalves a Boolean indicating whether same node
     * identifiers can appear in both halves of a [Quadruple] (a half means here
     * either the first two or last two items of a quadruple)
     *
     * @return [Quadruple] containing four node identifiers.
     */
    fun nodeIdQuadruple(numberOfDiscreteNodes: Int,
                        discreteNodesBetweenHalves: Boolean)
        : Gen<Quadruple<InfrastructureNodeId, InfrastructureNodeId, InfrastructureNodeId, InfrastructureNodeId>> {

        if (discreteNodesBetweenHalves) {
            require(numberOfDiscreteNodes in 2..4) {
                "numberOfDiscreteNodes must be in range 2..4 when discreteNodesBetweenHalves=true, but was: $numberOfDiscreteNodes"
            }
        } else {
            require(numberOfDiscreteNodes in 1..4) {
                "numberOfDiscreteNodes must be in range 1..4, but was: $numberOfDiscreteNodes"
            }
        }

        return when (numberOfDiscreteNodes) {
            1 -> infrastructureNodeId().map { id -> Quadruple(id, id, id, id) }
            2 -> when (discreteNodesBetweenHalves) {
                true -> discreteNodeIdPair().map { (id1, id2) -> Quadruple(id1, id1, id2, id2) }
                false -> discreteNodeIdPair().map { (id1, id2) -> Quadruple(id1, id2, id1, id2) }
            }
            3 -> when (discreteNodesBetweenHalves) {
                true -> discreteNodeIdTriple().zip(booleans()) { (id1, id2, id3), shuffle ->
                    if (shuffle)
                        Quadruple(id1, id1, id2, id3)
                    else
                        Quadruple(id1, id2, id3, id3)
                }
                false -> discreteNodeIdTriple().map { (id1, id2, id3) -> Quadruple(id1, id2, id2, id3) }
            }
            4 -> discreteNodeIdQuadruple()
            else -> throw IllegalStateException("Something went wrong while generating quadruple of node IDs")
        }
    }
}
