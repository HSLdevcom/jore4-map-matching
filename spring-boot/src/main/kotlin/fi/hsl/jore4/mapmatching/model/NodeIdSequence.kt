package fi.hsl.jore4.mapmatching.model

import fi.hsl.jore4.mapmatching.util.CollectionUtils.filterOutConsecutiveDuplicates

@JvmInline
value class NodeIdSequence(val list: List<InfrastructureNodeId>) {
    val size: Int
        get() = list.count()

    fun isEmpty() = list.isEmpty()

    fun concat(other: NodeIdSequence): NodeIdSequence {
        return if (list.isEmpty()) {
            other
        } else if (other.list.isEmpty()) {
            this
        } else {
            NodeIdSequence(list + other.list)
        }
    }

    fun duplicatesRemoved() = NodeIdSequence(filterOutConsecutiveDuplicates(list))

    fun firstNodeOrThrow(): InfrastructureNodeId = list.first()

    fun lastNodeOrThrow(): InfrastructureNodeId = list.last()

    override fun toString() = list.toString()

    companion object {
        private val EMPTY = NodeIdSequence(emptyList())

        fun empty() = EMPTY
    }
}
