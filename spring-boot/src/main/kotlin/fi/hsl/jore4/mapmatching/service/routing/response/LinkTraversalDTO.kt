package fi.hsl.jore4.mapmatching.service.routing.response

import fi.hsl.jore4.mapmatching.model.ExternalLinkReference
import fi.hsl.jore4.mapmatching.model.PathTraversal
import org.geolatte.geom.G2D
import org.geolatte.geom.LineString

data class LinkTraversalDTO(val infrastructureLinkId: Long,
                            val externalLinkRef: ExternalLinkReference,
                            val isTraversalForwards: Boolean,
                            val geometry: LineString<G2D>,
                            val weight: Double,
                            val distance: Double) {

    companion object {
        fun from(path: PathTraversal) = LinkTraversalDTO(path.infrastructureLinkId,
                                                         path.externalLinkRef,
                                                         path.alongLinkDirection,
                                                         path.geom,
                                                         path.cost,
                                                         path.cost)
    }
}
