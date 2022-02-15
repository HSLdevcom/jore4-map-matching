package fi.hsl.jore4.mapmatching.service.matching.test

import fi.hsl.jore4.mapmatching.model.matching.RoutePoint
import fi.hsl.jore4.mapmatching.model.matching.RouteStopPoint
import fi.hsl.jore4.mapmatching.util.LogUtils.joinToLogString
import io.github.oshai.kotlinlogging.KotlinLogging
import org.geolatte.geom.G2D
import org.geolatte.geom.Geometries.mkLineString
import org.geolatte.geom.LineString
import org.geolatte.geom.Point
import org.geolatte.geom.PositionSequence
import org.geolatte.geom.PositionSequenceBuilder
import org.geolatte.geom.PositionSequenceBuilders
import org.geolatte.geom.crs.CoordinateReferenceSystems.WGS84

private val LOGGER = KotlinLogging.logger {}

object ExtractStopToStopSegments {
    private data class SegmentInfo(
        val routePoints: List<RoutePoint>,
        val referencingRoutes: MutableList<String>
    ) {
        fun withReferencingRoute(routeId: String): SegmentInfo {
            referencingRoutes.add(routeId)
            return this
        }
    }

    private data class RoutePointLocations(
        val measuredLocation: Point<G2D>,
        val projectedLocation: Point<G2D>? = null
    ) {
        fun hasMeasuredLocationOf(pos: G2D): Boolean = pos == measuredLocation.position

        fun hasProjectedLocationOf(pos: G2D): Boolean = projectedLocation?.run { pos == position } ?: false

        fun hasPosition(pos: G2D): Boolean = hasMeasuredLocationOf(pos) || hasProjectedLocationOf(pos)

        fun hasSharedPositionWith(other: RoutePointLocations): Boolean =
            hasPosition(other.measuredLocation.position) ||
                other.projectedLocation?.run { hasPosition(position) } == true
    }

    private fun RoutePoint.toLocations() =
        when (this) {
            is RouteStopPoint -> RoutePointLocations(location, projectedLocation)
            else -> RoutePointLocations(location)
        }

    fun extractStopToStopSegments(routes: List<PublicTransportRoute>): StopToStopSegmentation {
        val segmentMap: MutableMap<String, MutableMap<LineString<G2D>, SegmentInfo>> = mutableMapOf()
        val discardedRoutes = mutableListOf<String>()

        routes.forEach { route ->

            try {
                splitToStopToStopSegments(route).forEach { (stopToStopSegmentId, geometry, routePoints) ->

                    segmentMap
                        .getOrPut(stopToStopSegmentId) { mutableMapOf() }
                        .getOrPut(geometry) { SegmentInfo(routePoints, mutableListOf()) }
                        .withReferencingRoute(route.routeId)
                }
            } catch (e: IllegalArgumentException) {
                LOGGER.info {
                    "${route.routeId}: Discarding route because of mismatch between number of route point ranges " +
                        "and LineString segments"
                }
                e.message?.let(discardedRoutes::add)
            }
        }

        val stopToStopSegments =
            segmentMap
                .toSortedMap()
                .entries
                .flatMap { (stopToStopSegmentId, segmentMap) ->

                    segmentMap.entries
                        .sortedBy { it.key.numPositions }
                        .withIndex()
                        .map { (index, segmentEntry) ->

                            val geometry: LineString<G2D> = segmentEntry.key

                            val stopToStopRouteId =
                                if (index == 0) {
                                    "$stopToStopSegmentId"
                                } else {
                                    "$stopToStopSegmentId-${index + 1}"
                                }

                            val routePoints: List<RoutePoint> = segmentEntry.value.routePoints
                            val referencingRoutes: List<String> = segmentEntry.value.referencingRoutes

                            StopToStopSegment(stopToStopRouteId, geometry, routePoints, referencingRoutes)
                        }
                }

        return StopToStopSegmentation(stopToStopSegments, discardedRoutes)
    }

    private fun splitToStopToStopSegments(route: PublicTransportRoute): List<PublicTransportRoute> {
        val originRoutePoints: List<RoutePoint> = trimRoutePointStart(route)

        val routePointSegments: List<List<RoutePoint>> = splitRoutePoints(originRoutePoints)

        if (routePointSegments.isEmpty()) {
            return emptyList()
        }

        val terminusOfRoutePointSegments: List<Pair<RouteStopPoint, RouteStopPoint>> =
            routePointSegments
                .map { routePoints ->
                    val first: RoutePoint = routePoints.first()

                    if (first !is RouteStopPoint) {
                        throw IllegalStateException("Expected first point to be a RouteStopPoint")
                    }

                    val last: RoutePoint = routePoints.last()

                    if (last !is RouteStopPoint) {
                        throw IllegalStateException("Expected last point to be a RouteStopPoint")
                    }

                    first to last
                }

        val geometrySegments: List<LineString<G2D>> =
            splitRouteGeometry(
                route.routeId,
                route.routeGeometry,
                terminusOfRoutePointSegments,
                false
            )

        if (geometrySegments.size != routePointSegments.size) {
            LOGGER.info {
                "${route.routeId}: Stop-to-stop route point ranges (${terminusOfRoutePointSegments.size}): ${
                    joinToLogString(terminusOfRoutePointSegments)
                }"
            }
            LOGGER.info {
                "${route.routeId}: Split LineStrings (${geometrySegments.size}): ${
                    joinToLogString(geometrySegments)
                }"
            }

            // Re-run LineString splitting with debug prints.
            splitRouteGeometry(route.routeId, route.routeGeometry, terminusOfRoutePointSegments, true)

            throw IllegalArgumentException(route.routeId)
            // throw IllegalStateException(route.routeId)
        }

        fun getStopId(routePoint: RoutePoint): String =
            when (routePoint) {
                is RouteStopPoint -> {
                    routePoint.run { passengerId ?: nationalId?.toString() }
                        ?: routePoint.location.position.run { "($lon,$lat)" }
                }
                else -> throw IllegalStateException("Expected RouteStopPoint")
            }

        return geometrySegments
            .zip(routePointSegments)
            .map { (lineString, routePoints) ->

                PublicTransportRoute(
                    "${getStopId(routePoints.first())}-${getStopId(routePoints.last())}",
                    lineString,
                    routePoints
                )
            }
    }

    private fun trimRoutePointStart(route: PublicTransportRoute): List<RoutePoint> {
        // Drop the first route point if the route geometry does not start with it.
        return if (route.routePoints
                .first()
                .toLocations()
                .hasPosition(route.routeGeometry.startPosition)
        ) {
            route.routePoints
        } else {
            LOGGER.info {
                "${route.routeId}: Dropping the first route point because it is not part of route geometry"
            }
            route.routePoints.drop(1)
        }
    }

    private fun splitRoutePoints(originRoutePoints: List<RoutePoint>): List<List<RoutePoint>> {
        val stopToStopIndexRanges: List<Pair<Int, Int>> =
            extractRoutePointIndexRangesForStopToStopSegments(originRoutePoints)

        if (stopToStopIndexRanges.size < 2) {
            return emptyList()
        }

        return stopToStopIndexRanges
            .map { (startIndex, endIndex) ->
                val rps: MutableList<RoutePoint> = mutableListOf()

                for (index in startIndex..endIndex) {
                    rps.add(originRoutePoints[index])
                }

                rps
            }.filter { routePoints ->
                routePoints.run {
                    size > 2 || !first().toLocations().hasSharedPositionWith(last().toLocations())
                }
            }
    }

    private fun extractRoutePointIndexRangesForStopToStopSegments(routePoints: List<RoutePoint>): List<Pair<Int, Int>> {
        if (routePoints.isEmpty()) {
            return emptyList()
        }

        val routeStopPointIndices: List<Int> =
            routePoints.mapIndexedNotNull { index, routePoint ->
                if (routePoint is RouteStopPoint) index else null
            }

        if (routeStopPointIndices.size < 2) {
            return emptyList()
        }

        val indexRanges: MutableList<Pair<Int, Int>> = mutableListOf()

        for (i in 0 until routeStopPointIndices.lastIndex) {
            val startIndex: Int = routeStopPointIndices[i]
            val endIndex: Int = routeStopPointIndices[i + 1]

            indexRanges.add(startIndex to endIndex)
        }

        return indexRanges
    }

    private fun splitRouteGeometry(
        routeId: String,
        routeGeometry: LineString<G2D>,
        stopPointRanges: List<Pair<RouteStopPoint, RouteStopPoint>>,
        printDebug: Boolean
    ): List<LineString<G2D>> {
        if (stopPointRanges.isEmpty()) {
            return emptyList()
        }

        val firstStopPointOnRoute: RoutePoint = stopPointRanges.first().first

        val trimmedLine: LineString<G2D> = dropFromStart(routeGeometry, firstStopPointOnRoute.toLocations())

        val (_, splitLineStrings: List<LineString<G2D>>) =
            stopPointRanges
                .fold(
                    trimmedLine to emptyList<LineString<G2D>>()
                ) { (remainingLineString, substrings), (_, endStopPoint) ->

                    if (printDebug) {
                        LOGGER.info { "$routeId: Remaining    : $remainingLineString" }
                    }

                    if (remainingLineString.isEmpty) {
                        remainingLineString to substrings
                    } else {
                        val endStopLocations: RoutePointLocations = endStopPoint.toLocations()

                        val substring: LineString<G2D> = takeFromStart(remainingLineString, endStopLocations)

                        if (printDebug) {
                            LOGGER.info { "$routeId: LineSubstring: $substring" }
                        }

                        if (substring.isEmpty) {
                            remainingLineString to substrings
                        } else {
                            val newRemaining: LineString<G2D> = dropFromStart(remainingLineString, endStopLocations)

                            newRemaining to substrings + substring
                        }
                    }
                }

        return splitLineStrings
    }

    private fun takeFromStart(
        lineString: LineString<G2D>,
        toLocation: RoutePointLocations
    ): LineString<G2D> {
        val positionSequenceBuilder = PositionSequenceBuilders.variableSized(G2D::class.java)

        var measuredLocationMatchedAlready = false
        var projectedLocationMatchedAlready = false

        lineString.positions
            .takeWhile { pos ->
                when {
                    measuredLocationMatchedAlready && projectedLocationMatchedAlready -> false
                    measuredLocationMatchedAlready -> {
                        toLocation.hasProjectedLocationOf(pos).also { projectedLocationMatchedAlready = it }
                    }
                    projectedLocationMatchedAlready -> {
                        toLocation.hasMeasuredLocationOf(pos).also { measuredLocationMatchedAlready = it }
                    }
                    else -> {
                        if (toLocation.hasMeasuredLocationOf(pos)) {
                            measuredLocationMatchedAlready = true
                        } else if (toLocation.hasProjectedLocationOf(pos)) {
                            projectedLocationMatchedAlready = true
                        }
                        true
                    }
                }
            }.forEach(positionSequenceBuilder::add)

        return toLineString(positionSequenceBuilder)
    }

    private fun dropFromStart(
        lineString: LineString<G2D>,
        untilLocation: RoutePointLocations
    ): LineString<G2D> {
        if (untilLocation.hasPosition(lineString.startPosition)) {
            return lineString
        }

        val linePositions: PositionSequence<G2D> = lineString.positions

        val firstIndex: Int = linePositions.indexOfFirst(untilLocation::hasPosition)

        if (firstIndex < 0) {
            return lineString
        }

        val numPositions: Int = linePositions.size()

        val hasAtLeast2StopLocations =
            numPositions >= firstIndex + 2 &&
                untilLocation.hasPosition(linePositions.getPositionN(firstIndex + 1))

        val hasAtLeast3StopLocations =
            hasAtLeast2StopLocations &&
                numPositions >= firstIndex + 3 &&
                untilLocation.hasPosition(linePositions.getPositionN(firstIndex + 2))

        val has4StopLocations =
            hasAtLeast3StopLocations &&
                numPositions >= firstIndex + 4 &&
                untilLocation.hasPosition(linePositions.getPositionN(firstIndex + 3))

        val numPositionsToDrop: Int =
            when {
                has4StopLocations -> firstIndex + 2
                hasAtLeast2StopLocations -> firstIndex + 1
                else -> firstIndex
            }

        val positionSequenceBuilder = PositionSequenceBuilders.variableSized(G2D::class.java)

        linePositions
            .drop(numPositionsToDrop)
            .forEach(positionSequenceBuilder::add)

        return toLineString(positionSequenceBuilder)
    }

    private fun toLineString(positionSequenceBuilder: PositionSequenceBuilder<G2D>): LineString<G2D> {
        val positionSequence: PositionSequence<G2D> = positionSequenceBuilder.toPositionSequence()

        return when (positionSequence.size()) {
            0, 1 -> LineString(WGS84)
            else -> mkLineString(positionSequence, WGS84)
        }
    }
}
