package fi.hsl.jore4.mapmatching.repository.infrastructure

import fi.hsl.jore4.mapmatching.model.LatLng

data class NearestLinkResultDTO(val fromCoordinate: LatLng,
                                val linkId: String,
                                val closestDistance: Double,
                                val closerNodeId: Int,
                                val furtherNodeId: Int) {

    fun getNetworkNodeIds() = Pair(closerNodeId, furtherNodeId)
}
