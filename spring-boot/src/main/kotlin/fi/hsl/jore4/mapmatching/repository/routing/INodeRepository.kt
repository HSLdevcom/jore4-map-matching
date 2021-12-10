package fi.hsl.jore4.mapmatching.repository.routing

import fi.hsl.jore4.mapmatching.model.VehicleType

interface INodeRepository {

    /**
     * Resolves the best sequence of infrastructure node identifiers from the
     * given [nodeSequences] alternatives. This method is used while resolving
     * a route through infrastructure network.
     *
     * The best alternative is selected by the criteria that it yields the
     * shortest route through the infrastructure network such that the following
     * conditions are met:
     * - It is required that identifiers for both endpoint nodes of the first
     * and last link are included in the resulting list as the first two and
     * last two entries. This guarantees that the first and last link on the
     * route are traversed end-to-end since it is not sufficient that the route
     * just starts from one end of the first link and terminates at one end of
     * the last link. At first, this may sound trivial, but it requires
     * attention within algorithmic implementation.
     *
     * @param startLinkId identifier for the first infrastructure link on the
     * route. It is required that the first two entries in the result list
     * include the identifiers for both endpoint nodes of the referenced link.
     * @param endLinkId identifier for the last infrastructure link on the
     * route. It is required that the last two entries in the result list
     * include the identifiers for both endpoint nodes of the referenced link.
     * @param nodeSequences contains at most four lists of infrastructure
     * network node identifiers of which the optimal one is to be selected and
     * returned.
     * @param vehicleType vehicle type constraint applied while resolving the
     * best sequence of node identifiers. Resulting list must refer to only
     * those network nodes that appear as endpoints of such infrastructure links
     * that are safely traversable by the given vehicle type.
     *
     * @return the best fit from [nodeSequences] alternatives or null if the
     * conditions are not met.
     */
    fun resolveNodeSequence(startLinkId: Long,
                            endLinkId: Long,
                            nodeSequences: Iterable<List<Long>>,
                            vehicleType: VehicleType)
        : List<Long>?
}
