package fi.hsl.jore4.mapmatching.service.matching.test

import org.geolatte.geom.G2D
import org.geolatte.geom.LineString
import java.util.SortedMap
import kotlin.math.abs

sealed interface MatchResult {
    val routeId: String
    val sourceRouteGeometry: LineString<G2D>
    val sourceRouteLength: Double

    val matchFound: Boolean
}

interface SuccessfulMatchResult : MatchResult {

    val details: MatchDetails

    fun getLowestBufferRadius(): BufferRadius = details.getBufferRadiusOfFirstMatch()

    fun getLengthOfFirstMatch(): Double = details.getLengthOfFirstMatch()

    fun getLengthOfClosestMatch(): Double = details.getLengthOfClosestMatch(sourceRouteLength)

    fun getLengthDifferenceForFirstMatch(): Double = abs(sourceRouteLength - getLengthOfFirstMatch())

    fun getLengthDifferenceForClosestMatch(): Double = abs(sourceRouteLength - getLengthOfClosestMatch())

    fun getLengthDifferencePercentageForFirstMatch(): Double =
        100.0 * getLengthDifferenceForFirstMatch() / sourceRouteLength

    fun getLengthDifferencePercentageForClosestMatch(): Double =
        100.0 * getLengthDifferenceForClosestMatch() / sourceRouteLength
}

interface SegmentMatchResult : MatchResult {
    val startStopId: String
    val endStopId: String

    val numberOfRoutePoints: Int
    val referencingRoutes: List<String>
}

@JvmInline
value class BufferRadius(val value: Double) : Comparable<BufferRadius> {

    override fun compareTo(other: BufferRadius): Int = value.compareTo(other.value)

    override fun toString() = value.toString()
}

data class MatchDetails(val lengthsOfMatchResults: SortedMap<BufferRadius, Double>,
                        val unsuccessfulBufferRadiuses: Set<BufferRadius>) {

    init {
        require(lengthsOfMatchResults.isNotEmpty()) { "lengthsOfMatchResults must not be empty" }
    }

    fun getBufferRadiusOfFirstMatch(): BufferRadius = getMapEntryForFirstMatch().key

    fun getBufferRadiusOfClosestMatch(sourceRouteLength: Double): BufferRadius =
        getMapEntryForClosestMatch(sourceRouteLength).key

    fun getLengthOfFirstMatch(): Double = getMapEntryForFirstMatch().value

    fun getLengthOfClosestMatch(sourceRouteLength: Double): Double = getMapEntryForClosestMatch(sourceRouteLength).value

    private fun getMapEntryForFirstMatch(): Map.Entry<BufferRadius, Double> =
        lengthsOfMatchResults.entries.first()

    private fun getMapEntryForClosestMatch(sourceRouteLength: Double): Map.Entry<BufferRadius, Double> {
        return when (lengthsOfMatchResults.size) {
            1 -> getMapEntryForFirstMatch()
            else -> lengthsOfMatchResults.entries.minByOrNull { (bufferRadius, length) ->
                abs(length - sourceRouteLength)
            }!!
        }
    }
}

data class SuccessfulRouteMatchResult(override val routeId: String,
                                      override val sourceRouteGeometry: LineString<G2D>,
                                      override val sourceRouteLength: Double,
                                      override val details: MatchDetails)
    : SuccessfulMatchResult {

    override val matchFound = true
}

data class RouteMatchFailure(override val routeId: String,
                             override val sourceRouteGeometry: LineString<G2D>,
                             override val sourceRouteLength: Double,
                             val bufferRadius: BufferRadius)
    : MatchResult {

    override val matchFound = false
}

data class SuccessfulSegmentMatchResult(override val routeId: String,
                                        override val sourceRouteGeometry: LineString<G2D>,
                                        override val sourceRouteLength: Double,
                                        override val details: MatchDetails,
                                        override val startStopId: String,
                                        override val endStopId: String,
                                        override val numberOfRoutePoints: Int,
                                        override val referencingRoutes: List<String>)
    : SuccessfulMatchResult, SegmentMatchResult {

    override val matchFound = true
}

data class SegmentMatchFailure(override val routeId: String,
                               override val sourceRouteGeometry: LineString<G2D>,
                               override val sourceRouteLength: Double,
                               val bufferRadius: BufferRadius,
                               override val startStopId: String,
                               override val endStopId: String,
                               override val numberOfRoutePoints: Int,
                               override val referencingRoutes: List<String>)
    : SegmentMatchResult {

    override val matchFound = false
}
