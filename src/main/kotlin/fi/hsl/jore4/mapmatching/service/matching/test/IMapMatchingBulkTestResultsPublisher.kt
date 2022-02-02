package fi.hsl.jore4.mapmatching.service.matching.test

interface IMapMatchingBulkTestResultsPublisher {
    fun publishResults(matchResults: List<MatchResult>)
}
