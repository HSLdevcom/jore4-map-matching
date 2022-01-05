package fi.hsl.jore4.mapmatching.model

import java.lang.IllegalArgumentException

data class InfrastructureLinkId(val value: Long) {

    init {
        if (value < 1) {
            throw IllegalArgumentException("Infrastructure link ID must be greater than zero: $value")
        }
    }

    override fun toString() = "LinkId($value)"
}
