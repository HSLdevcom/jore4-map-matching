package fi.hsl.jore4.mapmatching.model

import java.lang.IllegalArgumentException

/**
 * @property id is the identifier of the infrastructure node
 * @property distanceToNode is the distance from an arbitrary point to the
 * infrastructure node
 */
data class NodeProximity(val id: Long, val distanceToNode: Double)
    : HasInfrastructureNodeId {

    init {
        if (distanceToNode < 0.0) {
            throw IllegalArgumentException("distanceToNode must be greater than or equal to 0.0")
        }
    }

    override fun getInfrastructureNodeId() = id
}
