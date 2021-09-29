package fi.hsl.jore4.mapmatching.repository.infrastructure

import fi.hsl.jore4.mapmatching.model.LatLng
import fi.hsl.jore4.mapmatching.util.MultilingualString

data class StopInfoDTO(val publicTransportStopId: Long,
                       val stopNationalId: Int,
                       val stopPoint: LatLng,
                       val locatedOnInfrastructureLinkId: Long,
                       val distanceOfStopFromStartOfLink: Double,
                       val direction: DirectionType,
                       val name: MultilingualString)
