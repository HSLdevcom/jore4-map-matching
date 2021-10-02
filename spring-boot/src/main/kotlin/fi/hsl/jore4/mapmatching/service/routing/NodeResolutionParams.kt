package fi.hsl.jore4.mapmatching.service.routing

import fi.hsl.jore4.mapmatching.util.CollectionUtils.filterOutConsecutiveDuplicates

data class NodeResolutionParams(private val userSelectedLinks: List<SelectedLink>) {

    data class SelectedLink(val linkId: String, val closerNodeId: Int, val furtherNodeId: Int) {

        fun toNodeList(): List<Int> = listOf(closerNodeId, furtherNodeId)

        fun toNodeListReversed(): List<Int> = listOf(furtherNodeId, closerNodeId)

        fun getNodeSequenceCombinations(): List<List<Int>> = listOf(toNodeList(), toNodeListReversed())
    }

    data class LinkAppearance(val linkId: String, private val nodeIds: List<Int>) {

        val filteredNodeIds: List<Int>

        init {
            if (nodeIds.isEmpty()) {
                throw IllegalArgumentException("Must have at least 1 node")
            }
            filteredNodeIds = filterOutConsecutiveDuplicates(nodeIds)
        }

        val nodeCount: Int
            get() = filteredNodeIds.size

        val firstNodeId: Int
            get() = filteredNodeIds.first()

        val lastNodeId: Int
            get() = filteredNodeIds.last()
    }

    companion object Transformer {
        internal fun transform(links: List<SelectedLink>): List<LinkAppearance> {
            if (links.size == 1) {
                val link = links[0]
                return listOf(LinkAppearance(link.linkId, link.toNodeList()))
            }

            val result = mutableListOf<LinkAppearance>()
            var prevLinkId: String? = null
            var nodeIds = mutableListOf<Int>()

            for (link in links) {
                if (link.linkId != prevLinkId) {
                    if (prevLinkId != null) {
                        result.add(LinkAppearance(prevLinkId, nodeIds))
                        nodeIds = mutableListOf()
                    }
                    prevLinkId = link.linkId
                }
                nodeIds.add(link.closerNodeId)
            }

            if (prevLinkId != null) {
                result.add(LinkAppearance(prevLinkId, nodeIds))
            }

            return result
        }
    }

    private val linkAppearances: List<LinkAppearance>

    init {
        if (userSelectedLinks.isEmpty()) {
            throw IllegalArgumentException("Must have at least 1 link")
        }
        linkAppearances = transform(userSelectedLinks)
    }

    val firstLink: SelectedLink
        get() = userSelectedLinks.first()

    val lastLink: SelectedLink
        get() = userSelectedLinks.last()

    private val firstLinkAppearance: LinkAppearance
        get() = linkAppearances.first()

    private val lastLinkAppearance: LinkAppearance
        get() = linkAppearances.last()

    fun resolvePossibleNodeSequences(): Set<List<Int>> {
        if (linkAppearances.size == 1) {
            return setOf(firstLinkAppearance.filteredNodeIds)
        }

        if (firstLinkAppearance.nodeCount > 1 && lastLinkAppearance.nodeCount > 1) {
            return setOf(getAllNodes())
        }

        val nodeSequences: List<List<Int>> = resolvePossibleStartNodeSequences()
            .flatMap { startNodeSeq ->
                resolvePossibleEndNodeSequences().map { endNodeSeq ->

                    val nodeIds = startNodeSeq.toMutableList()
                    nodeIds.addAll(getInterimNodes())
                    nodeIds.addAll(endNodeSeq)

                    filterOutConsecutiveDuplicates(nodeIds)
                }
            }

        return nodeSequences.toSet()
    }

    private fun getAllNodes(): List<Int> =
        filterOutConsecutiveDuplicates(linkAppearances.flatMap { it.filteredNodeIds })

    private fun getInterimNodes(): List<Int> = getAllNodes().drop(1).dropLast(1)

    private fun resolvePossibleStartNodeSequences(): List<List<Int>> =
        if (firstLinkAppearance.nodeCount > 1)
            listOf(listOf(firstLinkAppearance.firstNodeId))
        else
            firstLink.getNodeSequenceCombinations()

    private fun resolvePossibleEndNodeSequences(): List<List<Int>> =
        if (lastLinkAppearance.nodeCount > 1)
            listOf(listOf(lastLinkAppearance.lastNodeId))
        else
            lastLink.getNodeSequenceCombinations()

    fun toCompactString() = "{startLink=${firstLink.linkId}, endLink=${lastLink.linkId}, viaNodes=${getInterimNodes()}}"
}
