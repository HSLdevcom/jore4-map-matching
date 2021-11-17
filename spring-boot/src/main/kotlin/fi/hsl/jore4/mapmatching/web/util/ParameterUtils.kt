package fi.hsl.jore4.mapmatching.web.util

import fi.hsl.jore4.mapmatching.model.LatLng
import java.util.regex.Matcher
import java.util.regex.Pattern

object ParameterUtils {

    private const val DECIMAL = "\\d+(?:\\.\\d+)?"
    private const val COORDINATE = "$DECIMAL,$DECIMAL"
    private const val FORMAT = "\\.json"
    private const val COORDINATE_LIST: String = "($COORDINATE)((?:~$COORDINATE)*)((?:$FORMAT)?)"

    private val COORDINATE_PATTERN: Pattern = Pattern.compile(COORDINATE_LIST)

    fun parseCoordinates(coordinates: String): List<LatLng> {
        val matcher: Matcher = COORDINATE_PATTERN.matcher(coordinates)

        if (!matcher.matches()) {
            throw IllegalArgumentException("Invalid coordinate sequence: \"$coordinates\"")
        }

        // Strip out optional format (.json). It is supported to mimic OSRM API.
        val stringToBeParsed = if (matcher.groupCount() > 2)
            matcher.group(1) + matcher.group(2)
        else
            coordinates

        return stringToBeParsed
            .split("~")
            .map { str ->
                val lngLat: List<String> = str.split(",")

                try {
                    val lng = lngLat[0].toDouble()
                    val lat = lngLat[1].toDouble()
                    LatLng(lat, lng)
                } catch (ex: RuntimeException) {
                    throw IllegalArgumentException("Invalid coordinate: \"$str\"")
                }
            }
    }
}
