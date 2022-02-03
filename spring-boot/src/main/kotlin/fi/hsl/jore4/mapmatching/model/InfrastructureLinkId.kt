package fi.hsl.jore4.mapmatching.model

import java.lang.IllegalArgumentException

@JvmInline
value class InfrastructureLinkId(val value: Long) {

    init {
        if (value < 1) {
            throw IllegalArgumentException("Infrastructure link ID must be greater than zero: $value")
        }
    }

    override fun toString() = value.toString()
}
