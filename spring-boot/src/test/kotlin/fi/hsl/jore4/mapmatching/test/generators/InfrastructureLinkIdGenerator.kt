package fi.hsl.jore4.mapmatching.test.generators

import org.quicktheories.core.Gen
import org.quicktheories.generators.Generate

object InfrastructureLinkIdGenerator {

    private val LINK_ID: Gen<Long> = Generate.longRange(1, 9_999)

    // Generate pairs of distinct link IDs.
    private val LINK_ID_PAIR: Gen<Pair<Long, Long>> =
        LINK_ID.flatMap { firstLinkId ->
            LINK_ID
                .assuming { it != firstLinkId }
                .map { secondLinkId -> Pair(firstLinkId, secondLinkId) }
        }

    fun infrastructureLinkId(): Gen<Long> = LINK_ID

    fun infrastructureLinkIdPair(): Gen<Pair<Long, Long>> = LINK_ID_PAIR
}
