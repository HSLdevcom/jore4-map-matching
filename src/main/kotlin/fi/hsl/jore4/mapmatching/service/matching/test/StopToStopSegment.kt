package fi.hsl.jore4.mapmatching.service.matching.test

import fi.hsl.jore4.mapmatching.model.matching.RoutePoint
import fi.hsl.jore4.mapmatching.model.matching.RouteStopPoint
import org.geolatte.geom.G2D
import org.geolatte.geom.LineString

data class StopToStopSegment(
    val segmentId: String,
    val geometry: LineString<G2D>,
    val routePoints: List<RoutePoint>,
    val referencingRoutes: List<String>
) {
    val startStopId: String
        get() =
            when (val startStop = routePoints.first()) {
                is RouteStopPoint -> startStop.passengerId
                else -> throw IllegalStateException("The first point is not a stop point")
            }

    val endStopId: String
        get() =
            when (val endStop = routePoints.last()) {
                is RouteStopPoint -> endStop.passengerId
                else -> throw IllegalStateException("The last point is not a stop point")
            }
}
