package fi.hsl.jore4.mapmatching.repository.routing

import fi.hsl.jore4.mapmatching.model.InfrastructureLinkId
import fi.hsl.jore4.mapmatching.model.NodeIdSequence
import fi.hsl.jore4.mapmatching.model.VehicleType
import org.geolatte.geom.G2D
import org.geolatte.geom.Point

interface INodeRepository {
    /**
     * Find the closest network nodes for every given point within the given
     * distance.
     *
     * @param points list of points for which node matches are to be found
     * @param vehicleType vehicle type constraint for node results. The returned
     * nodes must be associated with at least one such infrastructure link that
     * is safely traversable by the given vehicle type.
     * @param distanceInMeters the distance in meters within which the matches
     * are required to be found
     *
     * @return one-based index of the closest node matches for the given
     * points as [Map]. For each resolved node list an entry is added to
     * the result map. Entry key is derived as one-based index for point
     * appearing in the parameter list. If the closest nodes for a point
     * could not be found, no entry for the index of the point is added to
     * the result map. The result map contains at most the same amount of
     * entries as there are points in the parameter list.
     */
    fun findNClosestNodes(
        points: List<Point<G2D>>,
        vehicleType: VehicleType,
        distanceInMeters: Double
    ): Map<Int, SnapPointToNodesResult>

    /**
     * Resolves the best possible sequences of infrastructure node identifiers
     * and terminus links out of given [nodeSequenceCandidates]. For each given
     * unique pair of start/end link the node sequence yielding the shortest
     * route is selected and returned. The result list is filtered out the node
     * sequences on the basis of which it is not possible to create a route when
     * applying the conditions given by parameters [vehicleType] and
     * [bufferAreaRestriction].
     *
     * @param nodeSequenceCandidates the node sequence candidates to be tested
     * @param vehicleType vehicle type constraint to be applied when testing
     * candidates. The returned results should contain only the sequences that
     * consist of network nodes that appear as endpoints of infrastructure links
     * that are safely traversable by the given vehicle type.
     * @param bufferAreaRestriction contains data with which geometrical
     * restriction for the target set of infrastructure links can be applied
     * while testing candidates.
     *
     * @return [NodeIdSequence] indexed by pair of infrastructure link
     * identifiers associated with the sequence.
     */
    fun resolveBestNodeSequences(
        nodeSequenceCandidates: List<NodeSequenceCandidate>,
        vehicleType: VehicleType,
        bufferAreaRestriction: BufferAreaRestriction? = null
    ): Map<Pair<InfrastructureLinkId, InfrastructureLinkId>, NodeIdSequence>
}
