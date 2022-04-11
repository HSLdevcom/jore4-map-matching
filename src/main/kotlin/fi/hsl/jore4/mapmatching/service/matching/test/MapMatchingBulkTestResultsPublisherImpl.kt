package fi.hsl.jore4.mapmatching.service.matching.test

import com.fasterxml.jackson.databind.ObjectMapper
import fi.hsl.jore4.mapmatching.util.LogUtils.joinToLogString
import io.github.oshai.kotlinlogging.KotlinLogging
import org.geolatte.geom.G2D
import org.geolatte.geom.json.GeoJsonFeature
import org.geolatte.geom.json.GeoJsonFeatureCollection
import org.nield.kotlinstatistics.standardDeviation
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component
import java.io.File
import java.util.SortedMap
import kotlin.math.abs
import kotlin.math.roundToInt

private val LOGGER = KotlinLogging.logger {}

@Component
class MapMatchingBulkTestResultsPublisherImpl(
    val objectMapper: ObjectMapper,
    @Value("\${test.output.dir}") val outputDir: String
) : IMapMatchingBulkTestResultsPublisher {
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
        printBufferStatistics(succeeded, "Route")

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

        val outputFile: File = writeGeoJsonToFile(getFailedRoutesAsGeoJson(failed), FILENAME_FAILED_ROUTES_GEOJSON)
        LOGGER.info { "Wrote failed routes to file: ${outputFile.absolutePath}" }
    }

    fun publishStopToStopSegmentMatchResults(results: List<SegmentMatchResult>) {
        val (succeeded, failed) = partitionSegmentsBySuccess(results)

        printBasicStatistics(succeeded, failed, "Stop-to-stop segment")
        printBufferStatistics(succeeded, "Stop-to-stop segment")

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

        val geojsonFile: File =
            writeGeoJsonToFile(getFailedSegmentsAsGeoJson(failed), FILENAME_FAILED_SEGMENTS_GEOJSON)
        LOGGER.info { "Wrote failed stop-to-stop segments to GeoJSON file: ${geojsonFile.absolutePath}" }

        val failedSegmentsGeoPackageFile = File(outputDir, FILENAME_FAILED_SEGMENTS_GPKG)
        failedSegmentsGeoPackageFile.delete()
        GeoPackageUtils.createGeoPackage(failedSegmentsGeoPackageFile, failed, false)

        LOGGER.info {
            "Wrote failed stop-to-stop segments to GeoPackage file: ${
                failedSegmentsGeoPackageFile.absolutePath
            }"
        }

        val failureBuffersGeoPackageFile = File(outputDir, FILENAME_FAILED_SEGMENT_BUFFERS_GPKG)
        failureBuffersGeoPackageFile.delete()
        GeoPackageUtils.createGeoPackage(failureBuffersGeoPackageFile, failed, true)

        LOGGER.info {
            "Wrote failed stop-to-stop segment buffers to GeoPackage file: ${
                failureBuffersGeoPackageFile.absolutePath
            }"
        }
    }

    private fun writeGeoJsonToFile(
        features: GeoJsonFeatureCollection<G2D, String>,
        filename: String
    ): File {
        val geojson: String = objectMapper.writeValueAsString(features)

        val outputFile = File(outputDir, filename)
        outputFile.writeText(geojson)
        return outputFile
    }

    companion object {
        private const val FILENAME_FAILED_ROUTES_GEOJSON = "failed_routes.geojson"
        private const val FILENAME_FAILED_SEGMENTS_GEOJSON = "failed_segments.geojson"

        private const val FILENAME_FAILED_SEGMENTS_GPKG = "failed_segments.gpkg"
        private const val FILENAME_FAILED_SEGMENT_BUFFERS_GPKG = "failed_segment_buffers.gpkg"

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

            val failed: List<SegmentMatchFailure> =
                results.mapNotNull { it as? SegmentMatchFailure }

            return succeeded to failed
        }

        private fun getFailedRoutesAsGeoJson(failed: List<RouteMatchFailure>): GeoJsonFeatureCollection<G2D, String> =
            GeoJsonFeatureCollection(
                failed.map {
                    GeoJsonFeature(it.sourceRouteGeometry, it.routeId, emptyMap())
                }
            )

        private fun getFailedSegmentsAsGeoJson(
            failed: List<SegmentMatchFailure>
        ): GeoJsonFeatureCollection<G2D, String> =
            GeoJsonFeatureCollection(
                failed.map {
                    GeoJsonFeature(
                        it.sourceRouteGeometry,
                        it.routeId,
                        mapOf("referencingRoutes" to it.referencingRoutes)
                    )
                }
            )

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

        private fun <SUCCESSFUL : SuccessfulMatchResult> printBufferStatistics(
            succeeded: List<SUCCESSFUL>,
            resultSetName: String
        ) {
            val bufferRadiusSet: MutableSet<BufferRadius> = mutableSetOf()

            succeeded.forEach {
                bufferRadiusSet.addAll(it.details.lengthsOfMatchResults.keys)
            }

            val bufferRadiusList: List<BufferRadius> = bufferRadiusSet.toList().sorted()

            if (bufferRadiusList.size < 2) {
                return
            }

            val lowestBufferRadiusAppearanceCounts: SortedMap<BufferRadius, Int> =
                bufferRadiusList
                    .associateWith { bufferRadius ->
                        succeeded.count { it.getLowestBufferRadius() == bufferRadius }
                    }.toSortedMap()

            LOGGER.info {
                "$resultSetName matches with different buffer radius values: ${
                    joinToLogString(lowestBufferRadiusAppearanceCounts.entries) { (bufferRadius, matchCount) ->
                        "$bufferRadius m: $matchCount pcs"
                    }
                }"
            }

            val lengthComparisons: List<SortedMap<BufferRadius, Double>> =
                (0 until bufferRadiusList.size - 1)
                    .mapNotNull { firstBufferRadiusIndex ->
                        val remainingBufferRadiusValues: List<BufferRadius> =
                            bufferRadiusList.drop(
                                firstBufferRadiusIndex
                            )

                        val lowestBufferRadius: BufferRadius = remainingBufferRadiusValues.first()

                        val filteredList: List<SUCCESSFUL> =
                            succeeded.filter { it.getLowestBufferRadius() == lowestBufferRadius }

                        when (filteredList.size) {
                            0 -> null
                            else -> {
                                remainingBufferRadiusValues.associateWith { bufferRadius ->
                                    val lengthDifferencesOfMatchedRoutes: List<Double> =
                                        filteredList
                                            .mapNotNull { match ->
                                                val lengthOfMatchedRoute: Double? =
                                                    match.details.lengthsOfMatchResults[bufferRadius]

                                                lengthOfMatchedRoute?.let {
                                                    // Return absolute length difference.
                                                    abs(lengthOfMatchedRoute - match.sourceRouteLength)
                                                } ?: run {
                                                    LOGGER.warn {
                                                        "The length of match result not available for route: ${match.routeId}"
                                                    }
                                                    null
                                                }
                                            }

                                    // Return average of all differences.
                                    lengthDifferencesOfMatchedRoutes.average()
                                }
                            }
                        }
                    }.map { it.toSortedMap() }

            if (lengthComparisons.isNotEmpty()) {
                LOGGER.info {
                    "$resultSetName match length differences compared with regard to buffer radius: ${
                        joinToLogString(lengthComparisons) { mapOfDifferences ->
                            mapOfDifferences.entries.joinToString(
                                prefix = "[",
                                transform = { (bufferRadius, avgDiff) ->
                                    "$bufferRadius: ${roundTo2Digits(avgDiff)}"
                                },
                                postfix = "]"
                            )
                        }
                    }"
                }
            }
        }
    }
}
