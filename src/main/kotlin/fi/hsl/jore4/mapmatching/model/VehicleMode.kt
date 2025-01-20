package fi.hsl.jore4.mapmatching.model

enum class VehicleMode(val value: String) {
    BUS("bus"),
    TRAM("tram"),
    TRAIN("train"),
    METRO("metro"),
    FERRY("ferry");

    companion object {
        fun from(str: String): VehicleMode? = values().firstOrNull { it.value == str }
    }
}
