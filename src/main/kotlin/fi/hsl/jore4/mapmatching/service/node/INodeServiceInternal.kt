package fi.hsl.jore4.mapmatching.service.node

import fi.hsl.jore4.mapmatching.model.VehicleType
import fi.hsl.jore4.mapmatching.repository.routing.BufferAreaRestriction

interface INodeServiceInternal {
    /**
     * Resolves optimal sequence of infrastructure network nodes from the given
     * [nodeSequenceCandidates]. In addition to node identifiers, the result
     * will also include the original snap points on infrastructure links at
     * route's terminus points.
     *
     * @param nodeSequenceCandidates contains node sequence candidates for
     * multiple (unique) pairs of start/end link from which to select one node
     * sequence
     * @param vehicleType vehicle type constraint to be applied while resolving
     * an optimal node sequence. Only such infrastructure links are considered
     * that are safely traversable by the given vehicle type.
     * @param bufferAreaRestriction contains data with which geometrical
     * restriction for the target set of infrastructure links can be defined
     * while resolving an optimal node sequence.
     */
    fun resolveNodeIdSequence(
        nodeSequenceCandidates: List<NodeSequenceCandidatesBetweenSnappedLinks>,
        vehicleType: VehicleType,
        bufferAreaRestriction: BufferAreaRestriction? = null
    ): NodeSequenceResolutionResult
}
