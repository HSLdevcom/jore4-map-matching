package fi.hsl.jore4.mapmatching.repository.routing

interface NodeRepository {

    fun resolveSimpleNodeSequence(startLinkId: String, endLinkId: String, nodeSequences: Iterable<List<Int>>): List<Int>?
}
