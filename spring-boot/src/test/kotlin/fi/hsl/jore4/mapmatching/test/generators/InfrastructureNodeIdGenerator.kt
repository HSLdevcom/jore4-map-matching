package fi.hsl.jore4.mapmatching.test.generators

import fi.hsl.jore4.mapmatching.model.InfrastructureNodeId
import fi.hsl.jore4.mapmatching.test.generators.CommonGenerators.discretePair
import fi.hsl.jore4.mapmatching.test.generators.CommonGenerators.discreteQuadruple
import fi.hsl.jore4.mapmatching.test.generators.CommonGenerators.discreteTriple
import fi.hsl.jore4.mapmatching.test.util.Quadruple
import org.quicktheories.core.Gen
import org.quicktheories.generators.Generate.longRange

object InfrastructureNodeIdGenerator {
    private val ID_VALUE: Gen<Long> = longRange(10_000L, 99_999L)

    fun infrastructureNodeId(): Gen<InfrastructureNodeId> = ID_VALUE.map(::InfrastructureNodeId)

    // Generate pairs of discrete node IDs e.g. for endpoints of single infrastructure link.
    fun discreteNodeIdPair(): Gen<Pair<InfrastructureNodeId, InfrastructureNodeId>> =
        discretePair(infrastructureNodeId())

    // pair of node IDs
    fun nodeIdPair(discrete: Boolean): Gen<Pair<InfrastructureNodeId, InfrastructureNodeId>> =
        if (discrete) {
            discreteNodeIdPair()
        } else {
            infrastructureNodeId().map { id -> id to id }
        }

    // triple of discrete node IDs
    fun discreteNodeIdTriple(): Gen<Triple<InfrastructureNodeId, InfrastructureNodeId, InfrastructureNodeId>> =
        discreteTriple(infrastructureNodeId())

    // quadruple of discrete node IDs
    fun discreteNodeIdQuadruple(): Gen<
        Quadruple<
            InfrastructureNodeId,
            InfrastructureNodeId,
            InfrastructureNodeId,
            InfrastructureNodeId
        >
    > =
        discreteQuadruple(infrastructureNodeId())

    /**
     * Generate a quadruple of node IDs for single infrastructure link that is
     * snapped on twice.
     *
     * @param discreteNodes is a Boolean indicating whether the link should have
     * discrete endpoint nodes.
     *
     * @return [Quadruple] containing four node identifiers.
     */
    fun nodeIdQuadrupleForSingleLink(
        discreteNodes: Boolean
    ): Gen<Quadruple<InfrastructureNodeId, InfrastructureNodeId, InfrastructureNodeId, InfrastructureNodeId>> =
        if (discreteNodes) {
            discreteNodeIdPair().map { (id1, id2) -> Quadruple(id1, id2, id1, id2) }
        } else {
            infrastructureNodeId().map { id -> Quadruple(id, id, id, id) }
        }

    /**
     * Generate quadruple of node IDs for two infrastructure links.
     *
     * @param discreteNodesOnFirstLink is a Boolean indicating whether the first
     * link should have discrete endpoint nodes.
     * @param discreteNodesOnSecondLink is a Boolean indicating whether the
     * second link should have discrete endpoint nodes.
     * @param isCommonNode is a Boolean indicating whether the two links should
     * be connected via a common node.
     *
     * @return [Quadruple] containing four node identifiers.
     */
    fun nodeIdQuadrupleForTwoLinks(
        discreteNodesOnFirstLink: Boolean,
        discreteNodesOnSecondLink: Boolean,
        isCommonNode: Boolean
    ): Gen<Quadruple<InfrastructureNodeId, InfrastructureNodeId, InfrastructureNodeId, InfrastructureNodeId>> =
        if (discreteNodesOnFirstLink && discreteNodesOnSecondLink) {
            if (isCommonNode) {
                discreteNodeIdTriple().map { (id1, id2, id3) -> Quadruple(id1, id2, id2, id3) }
            } else {
                discreteNodeIdQuadruple()
            }
        } else if (discreteNodesOnFirstLink) {
            if (isCommonNode) {
                discreteNodeIdPair().map { (id1, id2) -> Quadruple(id1, id2, id2, id2) }
            } else {
                discreteNodeIdTriple().map { (id1, id2, id3) -> Quadruple(id1, id2, id3, id3) }
            }
        } else if (discreteNodesOnSecondLink) {
            if (isCommonNode) {
                discreteNodeIdPair().map { (id1, id2) -> Quadruple(id1, id1, id1, id2) }
            } else {
                discreteNodeIdTriple().map { (id1, id2, id3) -> Quadruple(id1, id1, id2, id3) }
            }
        } else {
            if (isCommonNode) {
                infrastructureNodeId().map { id -> Quadruple(id, id, id, id) }
            } else {
                discreteNodeIdPair().map { (id1, id2) -> Quadruple(id1, id1, id2, id2) }
            }
        }
}
