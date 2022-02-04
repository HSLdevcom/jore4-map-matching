package fi.hsl.jore4.mapmatching.service.common.response

import fi.hsl.jore4.mapmatching.model.ExternalLinkReference
import fi.hsl.jore4.mapmatching.model.InfrastructureLinkTraversal
import fi.hsl.jore4.mapmatching.util.MultilingualString
import org.geolatte.geom.G2D
import org.geolatte.geom.LineString

data class LinkTraversalDTO(val infrastructureLinkId: Long,
                            val externalLinkRef: ExternalLinkReference,
                            val isTraversalForwards: Boolean,
                            val geometry: LineString<G2D>,
                            val weight: Double,
                            val distance: Double,
                            val infrastructureLinkName: MultilingualString) {

    companion object {
        fun from(link: InfrastructureLinkTraversal) = LinkTraversalDTO(link.infrastructureLinkId,
                                                                       link.externalLinkRef,
                                                                       link.geomTraversal.forwardTraversal,
                                                                       link.geomTraversal.geometry,
                                                                       link.cost,
                                                                       link.cost,
                                                                       link.infrastructureLinkName)
    }
}
