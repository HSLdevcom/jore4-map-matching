package fi.hsl.jore4.mapmatching.repository.infrastructure

import fi.hsl.jore4.mapmatching.model.LatLng

data class SnapPointToLinkDTO(val point: LatLng, val queryDistance: Double, val link: SnappedLinkState)
