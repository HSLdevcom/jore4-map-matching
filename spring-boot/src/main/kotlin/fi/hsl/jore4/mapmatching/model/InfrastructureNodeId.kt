package fi.hsl.jore4.mapmatching.model

import java.lang.IllegalArgumentException

data class InfrastructureNodeId(val value: Long) {

    init {
        if (value < 1) {
            throw IllegalArgumentException("Infrastructure node ID must be greater than zero: $value")
        }
    }

    override fun toString() = "NodeId($value)"
}
