package fi.hsl.jore4.mapmatching.model

enum class TrafficFlowDirectionType(private val dbValue: Int) {

    BIDIRECTIONAL(2),
    AGAINST_DIGITISED_DIRECTION(3),
    ALONG_DIGITISED_DIRECTION(4);

    companion object {
        fun from(dbValue: Int): TrafficFlowDirectionType = values()
            .find { it.dbValue == dbValue }
            ?: throw IllegalArgumentException("Could not find TrafficFlowDirection by dbValue: $dbValue")
    }
}
