package fi.hsl.jore4.mapmatching.model

@JvmInline
value class InfrastructureNodeId(
    val value: Long
) {
    init {
        require(value >= 1) { "Infrastructure node ID must be greater than zero: $value" }
    }

    override fun toString() = value.toString()
}
