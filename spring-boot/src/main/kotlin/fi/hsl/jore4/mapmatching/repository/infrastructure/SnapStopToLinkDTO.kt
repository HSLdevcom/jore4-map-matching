package fi.hsl.jore4.mapmatching.repository.infrastructure

import fi.hsl.jore4.mapmatching.model.LinkSide

/**
 * Models a result of finding the closest link to a public transport stop point.
 */
data class SnapStopToLinkDTO(val stopNationalId: Int,
                             val stopSideOnLink: LinkSide,
                             val link: SnappedLinkState)
