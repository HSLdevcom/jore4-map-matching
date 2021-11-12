package fi.hsl.jore4.mapmatching.repository.routing

import fi.hsl.jore4.mapmatching.model.VehicleType

interface INodeRepository {

    /**
     * Resolves an optimal sequence of infrastructure node IDs based on the
     * parameters.
     *
     * It is required that:
     * (1) IDs of both endpoint nodes for the first and last link (referenced by
     * IDs) are included in the list. This may sound trivial but requires
     * attention to detail.
     */
    fun resolveNodeSequence(startLinkId: Long,
                            endLinkId: Long,
                            nodeSequences: Iterable<List<Long>>,
                            vehicleType: VehicleType): List<Long>?
}
