package fi.hsl.jore4.mapmatching.service.matching

import fi.hsl.jore4.mapmatching.repository.infrastructure.SnappedLinkState

data class SnapStopToLinkDTO(val stopNationalId: Int, val link: SnappedLinkState)
