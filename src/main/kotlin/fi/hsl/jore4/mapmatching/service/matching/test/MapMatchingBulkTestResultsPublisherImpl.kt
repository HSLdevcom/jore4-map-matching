package fi.hsl.jore4.mapmatching.service.matching.test

import fi.hsl.jore4.mapmatching.util.LogUtils.joinToLogString
import io.github.oshai.kotlinlogging.KotlinLogging
import org.nield.kotlinstatistics.standardDeviation
import org.springframework.stereotype.Component

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
                        it.getLengthDifferenceForFirstMatch()
                    }, "lengthDiff-%": ${
                        it.getLengthDifferencePercentageForFirstMatch()
                    }}"""
                }

            "Best $limit successful route matches: $bestResults"
        }

        LOGGER.info {
            val limit = 10
            val worstResults =
                joinToLogString(getWorstMatchResults(succeeded, 10)) {
                    """{"routeId": "${it.routeId}", "lengthDiff": ${
                        it.getLengthDifferenceForFirstMatch()
                    }, "lengthDiff-%": ${
                        it.getLengthDifferencePercentageForFirstMatch()
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
                .sortedBy(SuccessfulMatchResult::getLengthDifferenceForFirstMatch)
                .take(limit)

        private fun getWorstMatchResults(
            results: List<SuccessfulMatchResult>,
            limit: Int
        ): List<SuccessfulMatchResult> =
            results
                .sortedByDescending(SuccessfulMatchResult::getLengthDifferenceForFirstMatch)
                .take(limit)

        private fun getMostReferencedFailedSegments(
            results: List<SegmentMatchResult>,
            limit: Int
        ): List<SegmentMatchResult> =
            results
                .filterIsInstance<SegmentMatchFailure>()
                .sortedByDescending { it.referencingRoutes.size }
                .take(limit)

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
                    100.0 * numSucceeded / numTotal
                } %), failed: $numFailed (${
                    100.0 * numFailed / numTotal
                } %)"
            }

            val lengthDiffs: List<Double> = succeeded.map(SuccessfulMatchResult::getLengthDifferenceForFirstMatch)

            LOGGER.info {
                "$resultSetName average length difference: ${
                    lengthDiffs.average()
                } meters, standard deviation: ${
                    lengthDiffs.standardDeviation()
                } meters"
            }

            val lengthDiffPercentages: List<Double> =
                succeeded.map(SuccessfulMatchResult::getLengthDifferencePercentageForFirstMatch)

            LOGGER.info {
                "$resultSetName average length difference-%: ${
                    lengthDiffPercentages.average()
                } %, standard deviation: ${
                    lengthDiffPercentages.standardDeviation()
                } %"
            }
        }
    }
}
