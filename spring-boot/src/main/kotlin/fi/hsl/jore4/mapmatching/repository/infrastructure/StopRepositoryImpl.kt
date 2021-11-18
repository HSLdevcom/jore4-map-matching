package fi.hsl.jore4.mapmatching.repository.infrastructure

import fi.hsl.jore4.mapmatching.model.tables.PublicTransportStop
import fi.hsl.jore4.mapmatching.model.tables.records.PublicTransportStopRecord
import org.jooq.DSLContext
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Repository
import org.springframework.transaction.annotation.Transactional

@Repository
class StopRepositoryImpl @Autowired constructor(val dslContext: DSLContext) : IStopRepository {

    @Transactional(readOnly = true)
    override fun findByNationalIds(publicTransportStopNationalIds: Collection<Int>): List<PublicTransportStopRecord> {
        if (publicTransportStopNationalIds.isEmpty()) {
            return emptyList()
        }

        return dslContext
            .select()
            .from(STOP)
            .where(STOP.PUBLIC_TRANSPORT_STOP_NATIONAL_ID.`in`(publicTransportStopNationalIds))
            .fetch()
            .into(PublicTransportStopRecord::class.java)
    }

    companion object {
        private val STOP = PublicTransportStop.PUBLIC_TRANSPORT_STOP
    }
}
