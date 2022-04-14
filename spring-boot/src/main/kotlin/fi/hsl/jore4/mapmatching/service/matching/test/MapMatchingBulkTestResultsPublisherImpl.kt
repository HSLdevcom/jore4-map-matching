package fi.hsl.jore4.mapmatching.service.matching.test

import fi.hsl.jore4.mapmatching.util.LogUtils.joinToLogString
import mu.KotlinLogging
import org.nield.kotlinstatistics.standardDeviation
import org.springframework.stereotype.Component
import java.util.SortedMap
import kotlin.math.abs

private val LOGGER = KotlinLogging.logger {}

@Component
class MapMatchingBulkTestResultsPublisherImpl : IMapMatchingBulkTestResultsPublisher {

    override fun publishMatchResultsForRoutesAndStopToStopSegments(routeResults: List<MatchResult>,
                                                                   stopToStopSegmentResults: List<SegmentMatchResult>) {

        publishRouteMatchResults(routeResults)
        publishStopToStopSegmentMatchResults(stopToStopSegmentResults)

        LOGGER.info("List of IDs of failed routes whose all segments were matched: {}",
                    joinToLogString(getRoutesNotMatchedEvenThoughAllSegmentsMatched(routeResults,
                                                                                    stopToStopSegmentResults)))
    }

    fun publishRouteMatchResults(results: List<MatchResult>) {
        val (succeeded, failed) = partitionBySuccess(results)

        printBasicStatistics(succeeded, failed, "Route")
        printBufferStatistics(succeeded, "Route")

        LOGGER.info {
            val limit = 10
            val bestResults = joinToLogString(getBestMatchResults(succeeded, limit)) {
                """{"routeId": "${it.routeId}", "lengthDiff": ${it.getLengthDifferenceForFirstMatch()}, "lengthDiff-%": ${it.getLengthDifferencePercentageForFirstMatch()}}"""
            }

            "Best $limit successful route matches: $bestResults"
        }

        LOGGER.info {
            val limit = 10
            val worstResults = joinToLogString(getWorstMatchResults(succeeded, 10)) {
                """{"routeId": "${it.routeId}", "lengthDiff": ${it.getLengthDifferenceForFirstMatch()}, "lengthDiff-%": ${it.getLengthDifferencePercentageForFirstMatch()}}"""
            }

            "Worst $limit successful route matches: $worstResults"
        }
    }

    fun publishStopToStopSegmentMatchResults(results: List<SegmentMatchResult>) {
        val (succeeded, failed) = partitionSegmentsBySuccess(results)

        printBasicStatistics(succeeded, failed, "Stop-to-stop segment")
        printBufferStatistics(succeeded, "Stop-to-stop segment")

        LOGGER.info {
            val limit = 20
            val mostReferencedFailedSegments = joinToLogString(getMostReferencedFailedSegments(failed, limit)) {
                "{\"segmentId\": \"${it.routeId}\", \"numReferencingRoutes\": ${it.referencingRoutes.size}, " +
                    "\"referencingRoutes\": \"${it.referencingRoutes}\", \"sourceRouteGeometry\": \"${it.sourceRouteGeometry}\"}"
            }

            "$limit most referenced failed stop-to-stop segments: $mostReferencedFailedSegments"
        }
    }

    companion object {

        private fun partitionBySuccess(results: List<MatchResult>)
            : Pair<List<SuccessfulRouteMatchResult>, List<RouteMatchFailure>> {

            val succeeded: List<SuccessfulRouteMatchResult> =
                results.mapNotNull { if (it is SuccessfulRouteMatchResult) it else null }

            val failed: List<RouteMatchFailure> = results.mapNotNull { if (it is RouteMatchFailure) it else null }

            return succeeded to failed
        }

        private fun partitionSegmentsBySuccess(results: List<SegmentMatchResult>)
            : Pair<List<SuccessfulSegmentMatchResult>, List<SegmentMatchFailure>> {

            val succeeded: List<SuccessfulSegmentMatchResult> =
                results.mapNotNull { if (it is SuccessfulSegmentMatchResult) it else null }

            val failed: List<SegmentMatchFailure> = results.mapNotNull { if (it is SegmentMatchFailure) it else null }

            return succeeded to failed
        }

        private fun getBestMatchResults(results: List<SuccessfulMatchResult>, limit: Int)
            : List<SuccessfulMatchResult> {

            return results
                .sortedBy(SuccessfulMatchResult::getLengthDifferenceForFirstMatch)
                .take(limit)
        }

        private fun getWorstMatchResults(results: List<SuccessfulMatchResult>, limit: Int)
            : List<SuccessfulMatchResult> {

            return results
                .sortedByDescending(SuccessfulMatchResult::getLengthDifferenceForFirstMatch)
                .take(limit)
        }

        private fun getMostReferencedFailedSegments(results: List<SegmentMatchResult>, limit: Int)
            : List<SegmentMatchResult> {

            return results
                .filterIsInstance(SegmentMatchFailure::class.java)
                .sortedByDescending { it.referencingRoutes.size }
                .take(limit)
        }

        private fun getRoutesNotMatchedEvenThoughAllSegmentsMatched(routeResults: List<MatchResult>,
                                                                    stopToStopSegmentResults: List<SegmentMatchResult>)
            : List<String> {

            val idsOfRoutesContainingFailedSegments: Set<String> = stopToStopSegmentResults
                .flatMap(SegmentMatchResult::referencingRoutes)
                .toSet()

            return routeResults
                .filterIsInstance<RouteMatchFailure>()
                .map(MatchResult::routeId)
                .filter { it !in idsOfRoutesContainingFailedSegments }
                .sorted()
        }

        private fun <SUCCESSFUL : SuccessfulMatchResult, FAILED : MatchResult> printBasicStatistics(succeeded: List<SUCCESSFUL>,
                                                                                                    failed: List<FAILED>,
                                                                                                    resultSetName: String) {
            val numSucceeded = succeeded.size
            val numFailed = failed.size
            val numTotal: Int = numSucceeded + numFailed

            LOGGER.info("{} match results: all: {}, successful: {} ({} %), failed: {} ({} %)",
                        resultSetName, numTotal,
                        numSucceeded, 100.0 * numSucceeded / numTotal,
                        numFailed, 100.0 * numFailed / numTotal)

            val lengthDiffs: List<Double> = succeeded.map(SuccessfulMatchResult::getLengthDifferenceForFirstMatch)

            LOGGER.info("{} average length difference: {} meters, standard deviation: {} meters",
                        resultSetName, lengthDiffs.average(), lengthDiffs.standardDeviation())

            val lengthDiffPercentages: List<Double> =
                succeeded.map(SuccessfulMatchResult::getLengthDifferencePercentageForFirstMatch)

            LOGGER.info("{} average length difference-%: {} %, standard deviation: {} %",
                        resultSetName, lengthDiffPercentages.average(), lengthDiffPercentages.standardDeviation())
        }

        private fun <SUCCESSFUL : SuccessfulMatchResult> printBufferStatistics(succeeded: List<SUCCESSFUL>,
                                                                               resultSetName: String) {
            val bufferRadiusSet: MutableSet<BufferRadius> = mutableSetOf()

            succeeded.forEach {
                bufferRadiusSet.addAll(it.details.lengthsOfMatchResults.keys)
            }

            val bufferRadiusList: List<BufferRadius> = bufferRadiusSet.toList().sorted()

            if (bufferRadiusList.size < 2) {
                return
            }

            val lowestBufferRadiusAppearanceCounts: SortedMap<BufferRadius, Int> = bufferRadiusList
                .associateWith { bufferRadius ->
                    succeeded.count { it.getLowestBufferRadius() == bufferRadius }
                }
                .toSortedMap()

            LOGGER.info("{} matches with different buffer radius values: {}",
                        resultSetName,
                        joinToLogString(lowestBufferRadiusAppearanceCounts.entries) { (bufferRadius, matchCount) ->
                            "$bufferRadius m: $matchCount pcs"
                        })

            val lengthComparisons: List<SortedMap<BufferRadius, Double>> = (0 until bufferRadiusList.size - 1)
                .mapNotNull { firstBufferRadiusIndex ->
                    val remainingBufferRadiusValues: List<BufferRadius> = bufferRadiusList.drop(firstBufferRadiusIndex)

                    val lowestBufferRadius: BufferRadius = remainingBufferRadiusValues.first()

                    val filteredList: List<SUCCESSFUL> =
                        succeeded.filter { it.getLowestBufferRadius() == lowestBufferRadius }

                    when (filteredList.size) {
                        0 -> null
                        else -> {
                            remainingBufferRadiusValues.associateWith { bufferRadius ->
                                val lengthDifferencesOfMatchedRoutes: List<Double> = filteredList
                                    .map { match ->
                                        val lengthOfMatchedRoute: Double =
                                            match.details.lengthsOfMatchResults[bufferRadius]!!

                                        // Return absolute length difference.
                                        abs(lengthOfMatchedRoute - match.sourceRouteLength)
                                    }

                                // Return average of all differences.
                                lengthDifferencesOfMatchedRoutes.average()
                            }
                        }
                    }
                }
                .map { it.toSortedMap() }

            if (lengthComparisons.isNotEmpty()) {
                LOGGER.info("{} match length differences compared with regard to buffer radius: {}",
                            resultSetName,
                            joinToLogString(lengthComparisons) { mapOfDifferences ->
                                mapOfDifferences.entries.joinToString(
                                    prefix = "[",
                                    transform = { (bufferRadius, avgDiff) ->
                                        "$bufferRadius: $avgDiff"
                                    },
                                    postfix = "]")
                            })
            }
        }
    }
}
