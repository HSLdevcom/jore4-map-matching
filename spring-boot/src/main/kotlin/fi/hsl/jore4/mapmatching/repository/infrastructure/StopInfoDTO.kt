package fi.hsl.jore4.mapmatching.repository.infrastructure

import fi.hsl.jore4.mapmatching.model.LatLng
import fi.hsl.jore4.mapmatching.model.LocalisedName

data class StopInfoDTO(val nationalId: Int,
                       val linkId: String,
                       val location: LatLng,
                       val distanceFromLinkStart: Double,
                       val direction: DirectionType,
                       val name: LocalisedName)
