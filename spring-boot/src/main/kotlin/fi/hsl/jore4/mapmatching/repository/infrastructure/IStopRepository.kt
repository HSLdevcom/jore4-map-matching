package fi.hsl.jore4.mapmatching.repository.infrastructure

import fi.hsl.jore4.mapmatching.model.tables.records.PublicTransportStopRecord

interface IStopRepository {

    /**
     * Find public transport stops by national identifiers.
     *
     * @param publicTransportStopNationalIds the national identifiers of
     * public transport stops to find matches for.
     *
     * @return [List] of [PublicTransportStopRecord]s found by the given
     * national identifiers.
     */
    fun findByNationalIds(publicTransportStopNationalIds: Collection<Int>): List<PublicTransportStopRecord>
}
