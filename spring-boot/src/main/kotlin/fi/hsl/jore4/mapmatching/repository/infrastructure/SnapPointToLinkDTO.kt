package fi.hsl.jore4.mapmatching.repository.infrastructure

import org.geolatte.geom.G2D
import org.geolatte.geom.Point

data class SnapPointToLinkDTO(val point: Point<G2D>, val queryDistance: Double, val link: SnappedLinkState)
