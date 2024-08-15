package fi.hsl.jore4.mapmatching.model

@JvmInline
value class InfrastructureLinkId(val value: Long) {
    init {
        require(value >= 1) { "Infrastructure link ID must be greater than zero: $value" }
    }

    override fun toString() = value.toString()
}
