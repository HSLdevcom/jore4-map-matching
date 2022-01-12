package fi.hsl.jore4.mapmatching.test.generators

import fi.hsl.jore4.mapmatching.model.InfrastructureNodeId
import fi.hsl.jore4.mapmatching.test.generators.CommonGenerators.distinctPair
import fi.hsl.jore4.mapmatching.test.generators.CommonGenerators.distinctQuadruple
import fi.hsl.jore4.mapmatching.test.generators.CommonGenerators.distinctTriple
import fi.hsl.jore4.mapmatching.test.util.Quadruple
import org.quicktheories.core.Gen
import org.quicktheories.generators.Generate.longRange

object InfrastructureNodeIdGenerator {

    private val ID_VALUE: Gen<Long> = longRange(10_000L, 99_999L)

    fun infrastructureNodeId(): Gen<InfrastructureNodeId> = ID_VALUE.map { id: Long -> InfrastructureNodeId(id) }

    // Generate pairs of distinct node IDs e.g. for endpoints of single infrastructure link.
    fun infrastructureNodeIdPair(): Gen<Pair<InfrastructureNodeId, InfrastructureNodeId>> =
        distinctPair(infrastructureNodeId())

    // triple of distinct node IDs
    fun infrastructureNodeIdTriple(): Gen<Triple<InfrastructureNodeId, InfrastructureNodeId, InfrastructureNodeId>> =
        distinctTriple(infrastructureNodeId())

    // quadruple of distinct node IDs
    fun infrastructureNodeIdQuadruple(): Gen<Quadruple<InfrastructureNodeId, InfrastructureNodeId, InfrastructureNodeId, InfrastructureNodeId>> =
        distinctQuadruple(infrastructureNodeId())
}
