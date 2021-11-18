package fi.hsl.jore4.mapmatching.service.node

import fi.hsl.jore4.mapmatching.model.VehicleType
import fi.hsl.jore4.mapmatching.repository.infrastructure.SnappedLinkState
import fi.hsl.jore4.mapmatching.util.CollectionUtils.filterOutConsecutiveDuplicates

/**
 * Contains a sequence of link information items based on which an optimal node
 * sequence through a topology network can be determined in order to resolve
 * the shortest path passing through the links (that have already been resolved
 * as closest to a given sequence of coordinates).
 */
data class NodeResolutionParams(val vehicleType: VehicleType, private val snappedLinks: List<SnappedLinkState>) {

    /**
     * Models a continuous sequence of node visitations on endpoints of one
     * single infrastructure link.
     *
     * Contains an identifier of an infrastructure link and a sequence of node
     * identifiers. The node identifiers are required to reference either of the
     * endpoints of the link.
     *
     * @property subsequentNodeIds may contain consecutive duplicate values
     * @property filteredNodeIds is cleaned from consecutive duplicates within
     * initialisation
     */
    data class ContinuousLinkVisitation(val infrastructureLinkId: Long, private val subsequentNodeIds: List<Long>) {

        val filteredNodeIds: List<Long>

        init {
            if (subsequentNodeIds.isEmpty()) {
                throw IllegalArgumentException("Must have at least 1 node")
            }
            filteredNodeIds = filterOutConsecutiveDuplicates(subsequentNodeIds)
        }

        val nodeCount: Int
            get() = filteredNodeIds.size

        val firstNodeId: Long
            get() = filteredNodeIds.first()

        val lastNodeId: Long
            get() = filteredNodeIds.last()
    }

    companion object {
        internal fun toContinuousVisitations(snappedLinks: List<SnappedLinkState>): List<ContinuousLinkVisitation> {
            if (snappedLinks.size == 1) {
                val link = snappedLinks[0]
                return listOf(ContinuousLinkVisitation(link.infrastructureLinkId, getNodeList(link)))
            }

            val result = mutableListOf<ContinuousLinkVisitation>()
            var prevLinkId: Long? = null
            var subsequentNodeIdsOnOneLink = mutableListOf<Long>()

            for (link in snappedLinks) {
                if (link.infrastructureLinkId != prevLinkId) {
                    if (prevLinkId != null) {
                        result.add(ContinuousLinkVisitation(prevLinkId, subsequentNodeIdsOnOneLink))
                        subsequentNodeIdsOnOneLink = mutableListOf()
                    }
                    prevLinkId = link.infrastructureLinkId
                }
                subsequentNodeIdsOnOneLink.add(link.closerNodeId)
            }

            if (prevLinkId != null) {
                result.add(ContinuousLinkVisitation(prevLinkId, subsequentNodeIdsOnOneLink))
            }

            return result
        }

        private fun getNodeList(link: SnappedLinkState): List<Long> = listOf(link.closerNodeId, link.furtherNodeId)

        internal fun getNodeSequenceCombinations(link: SnappedLinkState): List<List<Long>> {
            val nodeList: List<Long> = getNodeList(link)
            return listOf(nodeList, nodeList.reversed())
        }
    }

    private val linkVisitations: List<ContinuousLinkVisitation>

    init {
        if (snappedLinks.isEmpty()) {
            throw IllegalArgumentException("Must have at least one snapped link")
        }
        linkVisitations = toContinuousVisitations(snappedLinks)
    }

    val firstLink: SnappedLinkState
        get() = snappedLinks.first()

    val lastLink: SnappedLinkState
        get() = snappedLinks.last()

    private val firstLinkVisitation: ContinuousLinkVisitation
        get() = linkVisitations.first()

    private val lastLinkVisitation: ContinuousLinkVisitation
        get() = linkVisitations.last()

    fun resolvePossibleNodeSequences(): Set<List<Long>> {
        if (linkVisitations.size == 1) {
            return setOf(firstLinkVisitation.filteredNodeIds)
        }

        if (firstLinkVisitation.nodeCount > 1 && lastLinkVisitation.nodeCount > 1) {
            return setOf(getAllNodes())
        }

        val nodeSequences: List<List<Long>> = resolvePossibleStartNodeSequences()
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

    private fun getAllNodes(): List<Long> =
        filterOutConsecutiveDuplicates(linkVisitations.flatMap { it.filteredNodeIds })

    private fun getInterimNodes(): List<Long> = getAllNodes().drop(1).dropLast(1)

    private fun resolvePossibleStartNodeSequences(): List<List<Long>> =
        if (firstLinkVisitation.nodeCount > 1)
            listOf(listOf(firstLinkVisitation.firstNodeId))
        else
            getNodeSequenceCombinations(firstLink)

    private fun resolvePossibleEndNodeSequences(): List<List<Long>> =
        if (lastLinkVisitation.nodeCount > 1)
            listOf(listOf(lastLinkVisitation.lastNodeId))
        else
            getNodeSequenceCombinations(lastLink)

    fun toCompactString() = "{startLink=${firstLink.infrastructureLinkId}, endLink=${lastLink.infrastructureLinkId}, viaNodes=${getInterimNodes()}}"
}
