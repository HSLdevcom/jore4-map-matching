package fi.hsl.jore4.mapmatching.repository.infrastructure

import fi.hsl.jore4.mapmatching.model.VehicleType
import org.geolatte.geom.G2D
import org.geolatte.geom.Point

interface ILinkRepository {

    /**
     * Find the closest link for every given point within the given distance.
     *
     * @param points list of points for which link matches are to be found
     * @param vehicleType vehicle type constraint for link matches. A resulting
     * link must be safely traversable by the given vehicle type.
     * @param distanceInMeters the distance in meters within which the matches
     * are required to be found
     *
     * @return one-based index of the closest link matches for the given
     * points as [Map]. For each resolved closest link an entry is added to
     * the result map. Entry key is derived as one-based index for point
     * appearing in the parameter list. If the closest link for a point
     * could not be found, no entry for the index of the point is added to
     * the result map. The result map contains at most the same amount of
     * entries as there are points in the parameter list.
     */
    fun findClosestLinks(points: List<Point<G2D>>,
                         vehicleType: VehicleType,
                         distanceInMeters: Double): Map<Int, SnapPointToLinkDTO>
}
