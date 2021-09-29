package fi.hsl.jore4.mapmatching.repository.infrastructure

interface IStopRepository {

    /**
     * Finds all stops associated with the given set of infrastructure link identifiers.
     */
    fun findStopsAlongLinks(infrastructureLinkIds: Set<Long>): List<StopInfoDTO>
}
