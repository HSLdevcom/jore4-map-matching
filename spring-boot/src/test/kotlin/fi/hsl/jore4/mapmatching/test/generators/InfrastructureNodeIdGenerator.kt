package fi.hsl.jore4.mapmatching.test.generators

import fi.hsl.jore4.mapmatching.test.generators.CommonGenerators.distinctPair
import fi.hsl.jore4.mapmatching.test.generators.CommonGenerators.distinctQuadruple
import fi.hsl.jore4.mapmatching.test.generators.CommonGenerators.distinctTriple
import fi.hsl.jore4.mapmatching.test.util.Quadruple
import org.quicktheories.core.Gen
import org.quicktheories.generators.Generate.longRange

object InfrastructureNodeIdGenerator {

    private val ID: Gen<Long> = longRange(10_000L, 99_999L)

    // Generate pairs of distinct node IDs e.g. for endpoints of single infrastructure link.
    private val ID_PAIR: Gen<Pair<Long, Long>> = distinctPair(ID)

    // triple of distinct node IDs
    private val ID_TRIPLE: Gen<Triple<Long, Long, Long>> = distinctTriple(ID)

    // quadruple of distinct node IDs
    private val ID_QUADRUPLE: Gen<Quadruple<Long, Long, Long, Long>> = distinctQuadruple(ID)

    fun infrastructureNodeId(): Gen<Long> = ID

    fun infrastructureNodeIdPair(): Gen<Pair<Long, Long>> = ID_PAIR

    fun infrastructureNodeIdTriple(): Gen<Triple<Long, Long, Long>> = ID_TRIPLE

    fun infrastructureNodeIdQuadruple(): Gen<Quadruple<Long, Long, Long, Long>> = ID_QUADRUPLE
}
