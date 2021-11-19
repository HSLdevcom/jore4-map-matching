package fi.hsl.jore4.mapmatching.web.util

import fi.hsl.jore4.mapmatching.model.LatLng
import java.util.regex.Matcher
import java.util.regex.Pattern

object ParameterUtils {

    private const val DECIMAL = "\\d+(?:\\.\\d+)?"
    private const val COORDINATE = "$DECIMAL,$DECIMAL"
    const val COORDINATE_LIST: String = "$COORDINATE(?:~$COORDINATE)*"

    private val COORDINATE_PATTERN: Pattern = Pattern.compile(COORDINATE_LIST)

    fun parseCoordinates(coordinates: String): List<LatLng> {
        val matcher: Matcher = COORDINATE_PATTERN.matcher(coordinates)

        if (!matcher.matches()) {
            throw IllegalArgumentException("Invalid coordinate sequence: \"$coordinates\"")
        }

        return coordinates
            .split("~")
            .map { coordinateToken ->
                val lngLat: List<String> = coordinateToken.split(",")

                try {
                    val lng = lngLat[0].toDouble()
                    val lat = lngLat[1].toDouble()
                    LatLng(lat, lng)
                } catch (ex: RuntimeException) {
                    throw IllegalArgumentException("Invalid coordinate: \"$coordinateToken\"")
                }
            }
    }
}
