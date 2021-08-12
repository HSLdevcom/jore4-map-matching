package fi.hsl.jore4.mapmatching.service.routing.response

import org.geolatte.geom.G2D
import org.geolatte.geom.LineString

data class LinkDTO(val digiroadLinkId: String,
                   val isTraversalForwards: Boolean,
                   val geometry: LineString<G2D>,
                   val weight: Double,
                   val distance: Double)
