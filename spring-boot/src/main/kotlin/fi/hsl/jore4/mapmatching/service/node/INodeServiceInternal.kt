package fi.hsl.jore4.mapmatching.service.node

import fi.hsl.jore4.mapmatching.model.VehicleType

interface INodeServiceInternal {

    /**
     * Resolves an optimal sequence of identifiers for infrastructure network
     * nodes from the options produced by the [NodeSequenceProducer] object.
     *
     * @param nodeSequenceProducer produces node sequences from which the best
     * needs to be resolved
     * @param vehicleType vehicle type constraint to be applied while resolving
     * optimal node sequence. Only those infrastructure links should be
     * considered that are safely traversable by the given vehicle type.
     */
    fun resolveNodeSequence(nodeSequenceProducer: NodeSequenceProducer,
                            vehicleType: VehicleType): List<Long>
}
