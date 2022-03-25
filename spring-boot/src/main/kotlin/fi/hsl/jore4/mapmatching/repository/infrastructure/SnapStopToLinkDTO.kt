package fi.hsl.jore4.mapmatching.repository.infrastructure

// Result of finding the closest link to a point
data class SnapStopToLinkDTO(val stopNationalId: Int, val link: SnappedLinkState)
