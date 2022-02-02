package fi.hsl.jore4.mapmatching.service.matching.test

import fi.hsl.jore4.mapmatching.util.LogUtils.joinToLogString
import mu.KotlinLogging
import org.nield.kotlinstatistics.standardDeviation
import org.springframework.stereotype.Component

private val LOGGER = KotlinLogging.logger {}

@Component
class MapMatchingBulkTestResultsPublisherImpl : IMapMatchingBulkTestResultsPublisher {

    override fun publishResults(matchResults: List<MatchResult>) {
        val (succeeded, failed) = partitionBySuccess(matchResults)

        LOGGER.info { "Successful: ${succeeded.size}, failed: ${failed.size}" }

        val lengthDiffs: List<Double> = succeeded.map(SuccessfulMatchResult::getLengthDifferenceForFirstMatch)

        LOGGER.info("Average length difference: {} meters, standard deviation: {} meters",
                    lengthDiffs.average(),
                    lengthDiffs.standardDeviation())

        val lengthDiffPercentages: List<Double> = succeeded.map(SuccessfulMatchResult::getLengthDifferencePercentageForFirstMatch)

        LOGGER.info("Average length difference-%: {} %, standard deviation: {} %",
                    lengthDiffPercentages.average(),
                    lengthDiffPercentages.standardDeviation())

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

    companion object {

        private fun partitionBySuccess(results: List<MatchResult>)
            : Pair<List<SuccessfulMatchResult>, List<MatchFailure>> {

            val succeeded: List<SuccessfulMatchResult> =
                results.mapNotNull { if (it is SuccessfulMatchResult) it else null }

            val failed: List<MatchFailure> = results.mapNotNull { if (it is MatchFailure) it else null }

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
    }
}
