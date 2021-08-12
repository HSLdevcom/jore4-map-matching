package fi.hsl.jore4.mapmatching.repository.infrastructure

import fi.hsl.jore4.mapmatching.model.tables.records.DrPysakkiRecord

interface StopRepository {

    /**
     * Finds all stops associated with the given set of link identifiers.
     */
    fun findAllStops(linkIds: Set<String>): List<DrPysakkiRecord>
}
