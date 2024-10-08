package fi.hsl.jore4.mapmatching.test.generators

import fi.hsl.jore4.mapmatching.model.InfrastructureLinkId
import fi.hsl.jore4.mapmatching.test.generators.CommonGenerators.discretePair
import org.quicktheories.core.Gen
import org.quicktheories.generators.Generate.longRange

object InfrastructureLinkIdGenerator {
    private val ID_VALUE: Gen<Long> = longRange(1, 9_999)

    fun infrastructureLinkId(): Gen<InfrastructureLinkId> = ID_VALUE.map(::InfrastructureLinkId)

    // Generate pairs of discrete link IDs.
    fun infrastructureLinkIdPair(): Gen<Pair<InfrastructureLinkId, InfrastructureLinkId>> =
        discretePair(infrastructureLinkId())
}
