package fi.hsl.jore4.mapmatching.service.matching

import fi.hsl.jore4.mapmatching.model.matching.RoutePoint
import fi.hsl.jore4.mapmatching.repository.infrastructure.SnapStopToLinkDTO

interface IPublicTransportStopMatcher {

    /**
     * Finds public transport stop points from the local database whose national
     * identifier matches with the one given for some [routePoints] item. Only
     * such items of [routePoints] are taken into account that represent public
     * transport stop points. For each stop point successfully matched from
     * database the closest point along the infrastructure link that the stop
     * point is located is returned. The results are indexed in the same order
     * as the route points exist in [routePoints], that is, there may be gaps
     * between the index numbers found as keys of the result [Map].
     *
     * @param routePoints points of a source route to be map-matched. National
     * identifiers are extracted from the route points representing public
     * transport stop points.
     * @param maxStopLocationDeviation the maximum distance between two
     * locations defined for a public transport stop, one given in the
     * map-matching request and the other in the local database, used as a
     * condition for matching the stop point (represented by some route point)
     * to infrastructure links in the local database. If the distance between
     * these two type of locations exceeds [maxStopLocationDeviation] for a stop
     * point, then the stop point is not included in the results.
     */
    fun findStopPointsByNationalIdsAndIndexByRoutePointOrdering(routePoints: List<RoutePoint>,
                                                                maxStopLocationDeviation: Double)
        : Map<Int, SnapStopToLinkDTO>
}
