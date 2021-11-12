package fi.hsl.jore4.mapmatching.repository.infrastructure

import fi.hsl.jore4.mapmatching.model.LatLng
import fi.hsl.jore4.mapmatching.model.VehicleType

interface ILinkRepository {

    /**
     * Finds the closest link for every coordinate within given distance.
     */
    fun findClosestLinks(coordinates: List<LatLng>,
                         vehicleType: VehicleType,
                         distanceInMeters: Double): Map<Int, SnapToLinkDTO>
}
