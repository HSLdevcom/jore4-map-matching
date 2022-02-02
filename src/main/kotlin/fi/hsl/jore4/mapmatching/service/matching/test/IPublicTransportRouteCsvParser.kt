package fi.hsl.jore4.mapmatching.service.matching.test

interface IPublicTransportRouteCsvParser {
    fun parsePublicTransportRoutes(filePath: String): List<PublicTransportRoute>
}
