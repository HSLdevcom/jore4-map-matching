package fi.hsl.jore4.mapmatching.repository.infrastructure

import fi.hsl.jore4.mapmatching.model.LatLng

data class SnapToLinkDTO(val point: LatLng,
                         val queryDistance: Double,
                         val infrastructureLinkId: Long,
                         val distance: Double,
                         val closerNodeId: Long,
                         val furtherNodeId: Long) {

    fun getNetworkNodeIds() = Pair(closerNodeId, furtherNodeId)
}
