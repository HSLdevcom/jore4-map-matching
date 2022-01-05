package fi.hsl.jore4.mapmatching.model

import fi.hsl.jore4.mapmatching.util.CollectionUtils.filterOutConsecutiveDuplicates

data class NodeIdSequence(val list: List<InfrastructureNodeId>) {

    val size: Int
        get() = list.count()

    fun isEmpty() = list.isEmpty()

    fun concat(other: NodeIdSequence): NodeIdSequence {
        return if (isEmpty())
            other
        else if (other.isEmpty())
            this
        else
            NodeIdSequence(list + other.list)
    }

    fun duplicatesRemoved() = NodeIdSequence(filterOutConsecutiveDuplicates(list))

    override fun toString(): String {
        return list.joinToString(prefix = "NodeIdSequence(", postfix = ")", transform = { it.value.toString() })
    }

    companion object {
        private val EMPTY = NodeIdSequence(emptyList())

        fun empty() = EMPTY
    }
}
