package fi.hsl.jore4.mapmatching.model

/**
 * @property id is the identifier of the infrastructure node
 * @property distanceToNode is the distance from an arbitrary point to the
 * infrastructure node
 */
data class NodeProximity(val id: InfrastructureNodeId, val distanceToNode: Double) :
    HasInfrastructureNodeId {
    init {
        require(distanceToNode >= 0.0) { "distanceToNode must be greater than or equal to 0.0" }
    }

    override fun getInfrastructureNodeId() = id
}
