package fi.hsl.jore4.mapmatching.repository.routing

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
    fun findNClosestNodes(points: List<Point<G2D>>,
                          vehicleType: VehicleType,
                          distanceInMeters: Double)
        : Map<Int, SnapPointToNodesDTO>

    /**
     * Resolves the best sequence of infrastructure node identifiers from the
     * given [nodeIdSequences] alternatives. This method is used while resolving
     * a route through infrastructure network.
     *
     * The best alternative is selected by the criteria that it yields the
     * shortest route through the infrastructure network.
     *
     * @param nodeIdSequences contains at most four sequences of infrastructure
     * network node identifiers of which the optimal one is to be selected and
     * returned.
     * @param vehicleType vehicle type constraint applied while resolving the
     * best sequence of node identifiers. Resulting sequence must refer to only
     * those network nodes that appear as endpoints of such infrastructure links
     * that are safely traversable by the given vehicle type.
     * @param bufferAreaRestriction contains data with which geometrical
     * restriction for the target set of infrastructure links can be defined
     * while resolving optimal nodes on a route through infrastructure network.
     *
     * @return the best fit from [nodeIdSequences] alternatives or null if the
     * conditions are not met.
     */
    fun resolveNodeSequence(nodeIdSequences: Iterable<NodeIdSequence>,
                            vehicleType: VehicleType,
                            bufferAreaRestriction: BufferAreaRestriction? = null)
        : NodeIdSequence?
}
