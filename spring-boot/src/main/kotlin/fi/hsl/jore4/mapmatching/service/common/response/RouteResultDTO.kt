package fi.hsl.jore4.mapmatching.service.common.response

import org.geolatte.geom.G2D
import org.geolatte.geom.LineString

data class RouteResultDTO(val geometry: LineString<G2D>,
                          val weight: Double,
                          val distance: Double,
                          val paths: List<LinkTraversalDTO>)
