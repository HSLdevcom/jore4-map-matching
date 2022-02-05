package fi.hsl.jore4.mapmatching.web.util

import fi.hsl.jore4.mapmatching.model.LatLng
import fi.hsl.jore4.mapmatching.model.VehicleMode
import fi.hsl.jore4.mapmatching.model.VehicleType
import java.util.regex.Matcher
import java.util.regex.Pattern

object ParameterUtils {

    private const val DECIMAL = "\\d+(?:\\.\\d+)?"
    private const val COORDINATE = "$DECIMAL,$DECIMAL"
    const val COORDINATE_LIST: String = "$COORDINATE(?:~$COORDINATE)*"

    private val COORDINATE_PATTERN: Pattern = Pattern.compile(COORDINATE_LIST)

    fun parseCoordinates(coordinates: String): List<LatLng> {
        val matcher: Matcher = COORDINATE_PATTERN.matcher(coordinates)

        require(matcher.matches()) { """Invalid coordinate sequence: "$coordinates"""" }

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

    fun findVehicleType(transportationModeParam: String, vehicleTypeParam: String?): VehicleType? {
        return VehicleMode.from(transportationModeParam)?.let { vehicleMode: VehicleMode ->
            findVehicleType(vehicleMode, vehicleTypeParam)
        }
    }

    fun findVehicleType(vehicleMode: VehicleMode, vehicleTypeParam: String?): VehicleType? {
        if (vehicleTypeParam != null) {
            // Vehicle type must match with its vehicle mode.
            return VehicleType.from(vehicleTypeParam)?.takeIf { it.vehicleMode == vehicleMode }
        }

        // When given vehicleType is null, resolve default deduced from vehicleMode.
        return when (vehicleMode) {
            VehicleMode.BUS -> VehicleType.GENERIC_BUS
            VehicleMode.FERRY -> VehicleType.GENERIC_FERRY
            VehicleMode.METRO -> VehicleType.GENERIC_METRO
            VehicleMode.TRAIN -> VehicleType.GENERIC_TRAIN
            VehicleMode.TRAM -> VehicleType.GENERIC_TRAM
        }
    }
}
