package fi.hsl.jore4.mapmatching.test.generators

import fi.hsl.jore4.mapmatching.model.InfrastructureLinkId
import fi.hsl.jore4.mapmatching.test.generators.CommonGenerators.distinctPair
import org.quicktheories.core.Gen
import org.quicktheories.generators.Generate.longRange

object InfrastructureLinkIdGenerator {

    private val ID_VALUE: Gen<Long> = longRange(1, 9_999)

    private val ID: Gen<InfrastructureLinkId> = ID_VALUE.map { id: Long -> InfrastructureLinkId(id) }

    fun infrastructureLinkId(): Gen<InfrastructureLinkId> = ID

    // Generate pairs of distinct link IDs.
    fun infrastructureLinkIdPair(): Gen<Pair<InfrastructureLinkId, InfrastructureLinkId>> = distinctPair(ID)
}
