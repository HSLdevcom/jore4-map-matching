package fi.hsl.jore4.mapmatching.model

enum class VehicleType(val vehicleMode: VehicleMode, val value: String) {
    GENERIC_BUS(VehicleMode.BUS, "generic_bus"),
    GENERIC_TRAM(VehicleMode.TRAM, "generic_tram"),
    GENERIC_TRAIN(VehicleMode.TRAIN, "generic_train"),
    GENERIC_METRO(VehicleMode.METRO, "generic_metro"),
    GENERIC_FERRY(VehicleMode.FERRY, "generic_ferry"),
    TALL_ELECTRIC_BUS(VehicleMode.BUS, "tall_electric_bus");

    companion object {
        fun from(str: String): VehicleType? = values().firstOrNull { it.value == str }
    }
}
