package fi.hsl.jore4.mapmatching.repository.infrastructure

import fi.hsl.jore4.mapmatching.model.LinkSide

/**
 * Models the result of finding the nearest infrastructure link to a public
 * transport stop point.
 */
data class SnapStopToLinkResult(val stopNationalId: Int,
                                val stopSideOnLink: LinkSide,
                                val pointOnLink: SnappedPointOnLink)
