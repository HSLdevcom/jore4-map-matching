package fi.hsl.jore4.mapmatching.repository.routing

import org.geolatte.geom.G2D
import org.geolatte.geom.LineString

data class RouteSegmentDTO(val routeSeqNum: Int,
                           val routeLegSeqNum: Int,
                           val nodeId: Int,
                           val linkId: String,
                           val cost: Double,
                           val isTraversalForwards: Boolean,
                           val geom: LineString<G2D>)
