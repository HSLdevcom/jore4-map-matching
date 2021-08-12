package fi.hsl.jore4.mapmatching.repository.infrastructure

import fi.hsl.jore4.mapmatching.model.tables.DrPysakki
import fi.hsl.jore4.mapmatching.model.tables.records.DrPysakkiRecord
import org.jooq.DSLContext
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Repository
import org.springframework.transaction.annotation.Transactional

@Repository
class StopRepositoryImpl @Autowired constructor(val dslContext: DSLContext) : StopRepository {

    @Transactional(readOnly = true)
    override fun findAllStops(linkIds: Set<String>): List<DrPysakkiRecord> = dslContext
        .select()
        .from(STOP)
        .where(STOP.LINK_ID.`in`(linkIds))
        .fetch()
        .into(DrPysakkiRecord::class.java)

    companion object {
        private val STOP = DrPysakki.DR_PYSAKKI
    }
}
