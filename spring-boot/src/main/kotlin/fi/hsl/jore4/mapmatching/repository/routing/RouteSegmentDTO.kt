package fi.hsl.jore4.mapmatching.repository.routing

import org.geolatte.geom.G2D
import org.geolatte.geom.LineString

data class RouteSegmentDTO(val seqNum: Int,
                           val segmentSeqNum: Int,
                           val nodeId: Int,
                           val linkGid: Int,
                           val linkId: String,
                           val cost: Double,
                           val isTraversalForwards: Boolean,
                           val geom: LineString<G2D>)
