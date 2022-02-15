package fi.hsl.jore4.mapmatching.service.matching.test

import fi.hsl.jore4.mapmatching.util.LogUtils.joinToLogString
import io.github.oshai.kotlinlogging.KotlinLogging
import org.nield.kotlinstatistics.standardDeviation
import org.springframework.stereotype.Component
import kotlin.math.abs
import kotlin.math.roundToInt

private val LOGGER = KotlinLogging.logger {}

@Component
class MapMatchingBulkTestResultsPublisherImpl : IMapMatchingBulkTestResultsPublisher {
    override fun publishMatchResultsForRoutesAndStopToStopSegments(
        routeResults: List<MatchResult>,
        stopToStopSegmentResults: List<SegmentMatchResult>
    ) {
        publishRouteMatchResults(routeResults)
        publishStopToStopSegmentMatchResults(stopToStopSegmentResults)

        LOGGER.info {
            "List of IDs of failed routes whose all segments were matched: ${
                joinToLogString(
                    getRoutesNotMatchedEvenThoughAllSegmentsMatched(
                        routeResults,
                        stopToStopSegmentResults
                    )
                )
            }"
        }
    }

    fun publishRouteMatchResults(results: List<MatchResult>) {
        val (succeeded, failed) = partitionBySuccess(results)

        printBasicStatistics(succeeded, failed, "Route")

        LOGGER.info {
            val limit = 10
            val bestResults =
                joinToLogString(getBestMatchResults(succeeded, limit)) {
                    """{"routeId": "${it.routeId}", "lengthDiff": ${
                        roundTo2Digits(it.getLengthDifferenceForFirstMatch())
                    }, "lengthDiff-%": ${
                        roundTo2Digits(it.getLengthDifferencePercentageForFirstMatch())
                    }}"""
                }

            "Best $limit successful route matches: $bestResults"
        }

        LOGGER.info {
            val limit = 20
            val worstResults =
                joinToLogString(getWorstMatchResults(succeeded, limit)) {
                    """{"routeId": "${it.routeId}", "lengthDiff": ${
                        roundTo2Digits(it.getLengthDifferenceForFirstMatch())
                    }, "lengthDiff-%": ${
                        roundTo2Digits(it.getLengthDifferencePercentageForFirstMatch())
                    }}"""
                }

            "Worst $limit successful route matches: $worstResults"
        }
    }

    fun publishStopToStopSegmentMatchResults(results: List<SegmentMatchResult>) {
        val (succeeded, failed) = partitionSegmentsBySuccess(results)

        printBasicStatistics(succeeded, failed, "Stop-to-stop segment")

        LOGGER.info {
            val limit = 20
            val mostReferencedFailedSegments =
                joinToLogString(getMostReferencedFailedSegments(failed, limit)) {
                    "{\"segmentId\": \"${it.routeId}\", \"numReferencingRoutes\": ${it.referencingRoutes.size}, " +
                        "\"referencingRoutes\": \"${it.referencingRoutes}\", \"sourceRouteGeometry\": \"${it.sourceRouteGeometry}\"}"
                }

            "$limit most referenced failed stop-to-stop segments: $mostReferencedFailedSegments"
        }

        LOGGER.info {
            val limit = 20
            val worstResults =
                joinToLogString(getWorstMatchResults(succeeded, limit)) {
                    """{"segmentId": "${it.routeId}", "lengthDiff": ${
                        roundTo2Digits(it.getLengthDifferenceForFirstMatch())
                    }, "lengthDiff-%": ${
                        roundTo2Digits(it.getLengthDifferencePercentageForFirstMatch())
                    }, "sourceRouteGeometry": "${it.sourceRouteGeometry}"}"""
                }

            "Worst $limit successful stop-to-stop segment matches: $worstResults"
        }
    }

    companion object {
        private fun partitionBySuccess(
            results: List<MatchResult>
        ): Pair<List<SuccessfulRouteMatchResult>, List<RouteMatchFailure>> {
            val succeeded: List<SuccessfulRouteMatchResult> =
                results.mapNotNull { it as? SuccessfulRouteMatchResult }

            val failed: List<RouteMatchFailure> = results.mapNotNull { it as? RouteMatchFailure }

            return succeeded to failed
        }

        private fun partitionSegmentsBySuccess(
            results: List<SegmentMatchResult>
        ): Pair<List<SuccessfulSegmentMatchResult>, List<SegmentMatchFailure>> {
            val succeeded: List<SuccessfulSegmentMatchResult> =
                results.mapNotNull { it as? SuccessfulSegmentMatchResult }

            val failed: List<SegmentMatchFailure> = results.mapNotNull { it as? SegmentMatchFailure }

            return succeeded to failed
        }

        private fun getBestMatchResults(
            results: List<SuccessfulMatchResult>,
            limit: Int
        ): List<SuccessfulMatchResult> =
            results
                .sortedBy { abs(it.getLengthDifferenceForFirstMatch()) }
                .take(limit)

        private fun getWorstMatchResults(
            results: List<SuccessfulMatchResult>,
            limit: Int
        ): List<SuccessfulMatchResult> =
            results
                .sortedByDescending { abs(it.getLengthDifferenceForFirstMatch()) }
                .take(limit)

        private fun getMostReferencedFailedSegments(
            results: List<SegmentMatchResult>,
            limit: Int
        ): List<SegmentMatchResult> =
            results
                .filterIsInstance<SegmentMatchFailure>()
                .sortedByDescending { it.referencingRoutes.size }
                .take(limit)

        private fun roundTo2Digits(n: Double): Double = (n * 100).roundToInt() / 100.0

        private fun getRoutesNotMatchedEvenThoughAllSegmentsMatched(
            routeResults: List<MatchResult>,
            stopToStopSegmentResults: List<SegmentMatchResult>
        ): List<String> {
            val idsOfRoutesContainingFailedSegments: Set<String> =
                stopToStopSegmentResults
                    .flatMap(SegmentMatchResult::referencingRoutes)
                    .toSet()

            return routeResults
                .filterIsInstance<RouteMatchFailure>()
                .map(MatchResult::routeId)
                .filter { it !in idsOfRoutesContainingFailedSegments }
                .sorted()
        }

        private fun <SUCCESSFUL : SuccessfulMatchResult, FAILED : MatchResult> printBasicStatistics(
            succeeded: List<SUCCESSFUL>,
            failed: List<FAILED>,
            resultSetName: String
        ) {
            val numSucceeded = succeeded.size
            val numFailed = failed.size
            val numTotal: Int = numSucceeded + numFailed

            LOGGER.info {
                "$resultSetName match results: all: $numTotal, successful: $numSucceeded (${
                    roundTo2Digits(100.0 * numSucceeded / numTotal)
                } %), failed: $numFailed (${
                    roundTo2Digits(100.0 * numFailed / numTotal)
                } %)"
            }

            val absLengthDiffs: List<Double> = succeeded.map { abs(it.getLengthDifferenceForFirstMatch()) }

            LOGGER.info {
                "$resultSetName average absolute length difference: ${
                    roundTo2Digits(absLengthDiffs.average())
                } meters, standard deviation: ${
                    roundTo2Digits(absLengthDiffs.standardDeviation())
                } meters"
            }

            val absLengthDiffPercentages: List<Double> =
                succeeded.map { abs(it.getLengthDifferencePercentageForFirstMatch()) }

            LOGGER.info {
                "$resultSetName average absolute length difference-%: ${
                    roundTo2Digits(absLengthDiffPercentages.average())
                } %, standard deviation: ${
                    roundTo2Digits(absLengthDiffPercentages.standardDeviation())
                } %"
            }
        }
    }
}
