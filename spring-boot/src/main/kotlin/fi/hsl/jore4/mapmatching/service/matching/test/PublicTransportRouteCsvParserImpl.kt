package fi.hsl.jore4.mapmatching.service.matching.test

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import com.github.doyaaaaaken.kotlincsv.client.CsvReader
import com.github.doyaaaaaken.kotlincsv.dsl.csvReader
import fi.hsl.jore4.mapmatching.model.matching.RoutePoint
import org.geolatte.geom.G2D
import org.geolatte.geom.LineString
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Component

@Component
class PublicTransportRouteCsvParserImpl @Autowired constructor(val objectMapper: ObjectMapper)
    : IPublicTransportRouteCsvParser {

    override fun parsePublicTransportRoutes(filePath: String): List<PublicTransportRoute> {
        val routes: MutableList<PublicTransportRoute> = mutableListOf()

        CSV_READER.open(filePath) {

            readAllAsSequence()
                .drop(1) // skip header line
                .forEach { row: List<String> ->

                    require(row.size >= COLUMNS) {
                        "Expected number of columns is $COLUMNS, but actual is: ${row.size}"
                    }

                    routes.add(parseCsvRow(row))
                }
        }

        return routes
    }

    private fun parseCsvRow(row: List<String>): PublicTransportRoute {
        val routeDirectionId: String = row[2]

        val routeGeometry: LineString<G2D> = objectMapper.readValue(row[4])
        val routePoints: List<RoutePoint> = objectMapper.readValue(row[5])

        return PublicTransportRoute(routeDirectionId, routeGeometry, routePoints)
    }

    companion object {

        private val CSV_READER: CsvReader = csvReader {
            charset = Charsets.UTF_8.name()
            quoteChar = '"'
            delimiter = ','
            escapeChar = '\\'
        }

        private const val COLUMNS = 6
    }
}
