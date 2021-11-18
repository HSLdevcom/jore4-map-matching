package fi.hsl.jore4.mapmatching.service.node

import fi.hsl.jore4.mapmatching.model.NodeIdSequence
import fi.hsl.jore4.mapmatching.model.VehicleType
import fi.hsl.jore4.mapmatching.repository.routing.BufferAreaRestriction

interface INodeServiceInternal {

    /**
     * Resolves an optimal sequence of identifiers for infrastructure network
     * nodes from the options provided by [nodeSequenceAlternatives] object.
     *
     * @param nodeSequenceAlternatives provides node sequence alternatives from
     * which the best needs to be resolved
     * @param vehicleType vehicle type constraint to be applied while resolving
     * optimal node sequence. Only those infrastructure links should be
     * considered that are safely traversable by the given vehicle type.
     * @param bufferAreaRestriction contains data that with which geometrical
     * restriction for the target set of infrastructure links can be defined
     * while resolving optimal node sequence.
     *
     * @throws [IllegalStateException] if node identifier sequence could not be
     * resolved
     */
    fun resolveNodeIdSequence(nodeSequenceAlternatives: NodeSequenceAlternatives,
                              vehicleType: VehicleType,
                              bufferAreaRestriction: BufferAreaRestriction? = null)
        : NodeIdSequence
}
