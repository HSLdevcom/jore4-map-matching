package fi.hsl.jore4.mapmatching.repository.routing

import fi.hsl.jore4.mapmatching.model.NodeProximity
import org.geolatte.geom.G2D
import org.geolatte.geom.Point

data class SnapPointToNodesResult(val point: Point<G2D>, val queryDistance: Double, val nodes: List<NodeProximity>)
