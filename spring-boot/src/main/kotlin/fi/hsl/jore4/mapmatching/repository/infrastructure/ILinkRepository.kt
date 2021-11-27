package fi.hsl.jore4.mapmatching.repository.infrastructure

import fi.hsl.jore4.mapmatching.model.LatLng
import fi.hsl.jore4.mapmatching.model.VehicleType

interface ILinkRepository {

    /**
     * Find the closest link for every coordinate within given distance.
     *
     * @param coordinates list of coordinates for which matches are to be found
     * @param vehicleType vehicle type constraint for link matches. A resulting
     * link must be safely traversable by the given vehicle type.
     * @param distanceInMeters the distance in meters within which the matches
     * are required to be found
     *
     * @return one-based index of the closest link matches for the given
     * coordinates as [Map]. For each resolved closest link an entry is added to
     * the result map. An entry key is the one-based index for a coordinate
     * appearing in the parameter list. If the closest link for a coordinate
     * could not be found, no entry for the index of the coordinate is added to
     * the result map. The result map contains at most the same amount of
     * entries as there are coordinates in the parameter list.
     */
    fun findClosestLinks(coordinates: List<LatLng>,
                         vehicleType: VehicleType,
                         distanceInMeters: Double): Map<Int, SnapPointToLinkDTO>
}
