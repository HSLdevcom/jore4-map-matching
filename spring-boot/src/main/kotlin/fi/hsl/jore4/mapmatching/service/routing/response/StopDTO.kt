package fi.hsl.jore4.mapmatching.service.routing.response

import fi.hsl.jore4.mapmatching.model.LocalisedName
import org.geolatte.geom.G2D
import org.geolatte.geom.Point

data class StopDTO(val location: Point<G2D>, val nationalId: Int, val name: LocalisedName)
