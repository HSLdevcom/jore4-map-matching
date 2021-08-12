package fi.hsl.jore4.mapmatching.model

import org.geolatte.geom.G2D
import org.geolatte.geom.LineString

data class PathTraversal(val infrastructureLinkId: Long,
                         val externalLinkRef: ExternalLinkReference,
                         val alongLinkDirection: Boolean, // along or against the direction of link geometry
                         val cost: Double,  // cost is currently the same as the 3D length of the link geometry
                         val geom: LineString<G2D>)
