package fi.hsl.jore4.mapmatching.test.generators

import fi.hsl.jore4.mapmatching.test.generators.CommonGenerators.distinctPair
import org.quicktheories.core.Gen
import org.quicktheories.generators.Generate.longRange

object InfrastructureLinkIdGenerator {

    private val ID: Gen<Long> = longRange(1, 9_999)

    fun infrastructureLinkId(): Gen<Long> = ID

    // Generate pairs of distinct link IDs.
    fun infrastructureLinkIdPair(): Gen<Pair<Long, Long>> = distinctPair(ID)
}
