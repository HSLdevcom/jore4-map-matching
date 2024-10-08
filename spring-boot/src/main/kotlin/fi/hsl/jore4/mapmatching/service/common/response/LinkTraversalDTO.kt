package fi.hsl.jore4.mapmatching.service.common.response

import fi.hsl.jore4.mapmatching.model.ExternalLinkReference
import fi.hsl.jore4.mapmatching.model.InfrastructureLinkTraversal
import fi.hsl.jore4.mapmatching.util.MultilingualString
import org.geolatte.geom.G2D
import org.geolatte.geom.LineString

data class LinkTraversalDTO(
    val infrastructureLinkId: Long,
    val externalLinkRef: ExternalLinkReference,
    val isTraversalForwards: Boolean,
    val geometry: LineString<G2D>, // geometry of the infrastructure link as it is, never reversed
    val weight: Double,
    val distance: Double,
    val infrastructureLinkName: MultilingualString
) {
    companion object {
        fun from(link: InfrastructureLinkTraversal) =
            LinkTraversalDTO(
                link.infrastructureLinkId.value,
                link.externalLinkRef,
                link.isTraversalForwards,
                link.linkGeometry,
                link.traversedDistance,
                link.linkLength,
                link.infrastructureLinkName
            )
    }
}
