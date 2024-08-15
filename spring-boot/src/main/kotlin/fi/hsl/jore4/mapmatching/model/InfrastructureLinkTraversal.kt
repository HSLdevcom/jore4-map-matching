package fi.hsl.jore4.mapmatching.model

import fi.hsl.jore4.mapmatching.util.MultilingualString
import org.geolatte.geom.G2D
import org.geolatte.geom.LineString

data class InfrastructureLinkTraversal(
    val infrastructureLinkId: InfrastructureLinkId,
    val externalLinkRef: ExternalLinkReference,
    val linkGeometry: LineString<G2D>,
    val traversedGeometry: LineString<G2D>, // partial or reversed when compared to linkGeometry
    val isTraversalForwards: Boolean,
    val linkLength: Double,
    val traversedDistance: Double,
    val isClosedLoop: Boolean,
    val infrastructureLinkName: MultilingualString
)
