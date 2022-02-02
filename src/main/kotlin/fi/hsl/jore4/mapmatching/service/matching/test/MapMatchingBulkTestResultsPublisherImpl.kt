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
    override fun publishResults(matchResults: List<MatchResult>) {
        val (succeeded, failed) = partitionBySuccess(matchResults)

        LOGGER.info { "Successful: ${succeeded.size}, failed: ${failed.size}" }

        val absLengthDiffs: List<Double> = succeeded.map { abs(it.getLengthDifferenceForFirstMatch()) }

        LOGGER.info {
            "Average length difference: ${
                roundTo2Digits(absLengthDiffs.average())
            } meters, standard deviation: ${
                roundTo2Digits(absLengthDiffs.standardDeviation())
            } meters"
        }

        val absLengthDiffPercentages: List<Double> =
            succeeded.map { abs(it.getLengthDifferencePercentageForFirstMatch()) }

        LOGGER.info {
            "Average length difference-%: ${
                roundTo2Digits(absLengthDiffPercentages.average())
            } %, standard deviation: ${
                roundTo2Digits(absLengthDiffPercentages.standardDeviation())
            } %"
        }

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

    companion object {
        private fun partitionBySuccess(
            results: List<MatchResult>
        ): Pair<List<SuccessfulMatchResult>, List<MatchFailure>> {
            val succeeded: List<SuccessfulMatchResult> =
                results.mapNotNull { it as? SuccessfulMatchResult }

            val failed: List<MatchFailure> = results.mapNotNull { it as? MatchFailure }

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

        private fun roundTo2Digits(n: Double): Double = (n * 100).roundToInt() / 100.0
    }
}
