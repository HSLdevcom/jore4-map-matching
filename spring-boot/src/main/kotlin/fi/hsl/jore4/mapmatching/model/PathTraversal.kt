package fi.hsl.jore4.mapmatching.model

import org.geolatte.geom.G2D
import org.geolatte.geom.LineString

data class PathTraversal(
    val geometry: LineString<G2D>,
    val forwardTraversal: Boolean // along (true) or against (false) the digitised direction of the geometry
)
