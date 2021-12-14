package fi.hsl.jore4.mapmatching.service.node

import fi.hsl.jore4.mapmatching.repository.infrastructure.SnappedLinkState

object SnappedLinkStateExtension {

    fun SnappedLinkState.toNodeIdList(): List<Long> = listOf(closerNodeId, furtherNodeId)

    fun SnappedLinkState.getNodeIdSequenceCombinations(): List<List<Long>> {
        val nodeList: List<Long> = toNodeIdList()

        return listOf(nodeList, nodeList.reversed())
    }
}
