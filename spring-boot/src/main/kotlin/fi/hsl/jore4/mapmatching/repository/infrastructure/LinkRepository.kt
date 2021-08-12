package fi.hsl.jore4.mapmatching.repository.infrastructure

import fi.hsl.jore4.mapmatching.model.LatLng

interface LinkRepository {

    /**
     * Finds the nearest link for every coordinate within given search radius.
     */
    fun findNearestLinks(coordinates: List<LatLng>, searchRadius: Int): Map<Int, NearestLinkResultDTO>
}
