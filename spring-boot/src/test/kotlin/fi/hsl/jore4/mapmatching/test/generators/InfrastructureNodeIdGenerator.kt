package fi.hsl.jore4.mapmatching.test.generators

import fi.hsl.jore4.mapmatching.model.InfrastructureNodeId
import fi.hsl.jore4.mapmatching.test.generators.CommonGenerators.discretePair
import fi.hsl.jore4.mapmatching.test.generators.CommonGenerators.discreteQuadruple
import fi.hsl.jore4.mapmatching.test.generators.CommonGenerators.discreteTriple
import fi.hsl.jore4.mapmatching.test.util.Quadruple
import org.quicktheories.core.Gen
import org.quicktheories.generators.Generate.longRange

object InfrastructureNodeIdGenerator {

    private val ID_VALUE: Gen<Long> = longRange(10_000L, 99_999L)

    fun infrastructureNodeId(): Gen<InfrastructureNodeId> = ID_VALUE.map(::InfrastructureNodeId)

    // Generate pairs of discrete node IDs e.g. for endpoints of single infrastructure link.
    fun infrastructureNodeIdPair(): Gen<Pair<InfrastructureNodeId, InfrastructureNodeId>> =
        discretePair(infrastructureNodeId())

    // triple of discrete node IDs
    fun infrastructureNodeIdTriple(): Gen<Triple<InfrastructureNodeId, InfrastructureNodeId, InfrastructureNodeId>> =
        discreteTriple(infrastructureNodeId())

    // quadruple of discrete node IDs
    fun infrastructureNodeIdQuadruple(): Gen<Quadruple<InfrastructureNodeId, InfrastructureNodeId, InfrastructureNodeId, InfrastructureNodeId>> =
        discreteQuadruple(infrastructureNodeId())
}
