package fi.hsl.jore4.mapmatching.service.matching.test

interface IMapMatchingBulkTestResultsPublisher {
    fun publishMatchResultsForRoutesAndStopToStopSegments(
        routeResults: List<MatchResult>,
        stopToStopSegmentResults: List<SegmentMatchResult>
    )
}
