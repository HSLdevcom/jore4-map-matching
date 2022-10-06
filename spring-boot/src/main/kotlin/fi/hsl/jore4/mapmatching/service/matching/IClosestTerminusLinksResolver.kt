package fi.hsl.jore4.mapmatching.service.matching

import fi.hsl.jore4.mapmatching.model.VehicleType
import fi.hsl.jore4.mapmatching.repository.infrastructure.SnappedPointOnLink
import org.geolatte.geom.G2D
import org.geolatte.geom.Point

interface IClosestTerminusLinksResolver {

    /**
     * Finds the closest infrastructure links to the given start and end point.
     *
     * @param startPoint the start point of route
     * @param endPoint the end point of route
     * @param vehicleType vehicle type constraint to be satisfied while finding
     * infrastructure links. The returned links must be safely traversable by
     * the given vehicle type.
     * @property linkQueryDistance the distance in meters at which the returned
     * infrastructure link must be located relative to the given terminus point.
     * @property linkQueryLimit the maximum number of the closest infrastructure
     * links that are returned for route endpoint.
     *
     * @throws [IllegalStateException] if no links are found for one or both of
     * the two endpoints of the route
     */
    fun findClosestInfrastructureLinksForRouteEndpoints(startPoint: Point<G2D>,
                                                        endPoint: Point<G2D>,
                                                        vehicleType: VehicleType,
                                                        linkQueryDistance: Double,
                                                        linkQueryLimit: Int)
        : Pair<List<SnappedPointOnLink>, List<SnappedPointOnLink>>
}
