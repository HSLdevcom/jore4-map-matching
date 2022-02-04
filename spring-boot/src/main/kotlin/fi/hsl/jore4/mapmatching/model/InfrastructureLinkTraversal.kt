package fi.hsl.jore4.mapmatching.model

import fi.hsl.jore4.mapmatching.util.MultilingualString

data class InfrastructureLinkTraversal(val infrastructureLinkId: Long,
                                       val externalLinkRef: ExternalLinkReference,
                                       val geomTraversal: GeomTraversal,
                                       val cost: Double,  // cost is currently the same as the 3D length of the link geometry
                                       val infrastructureLinkName: MultilingualString)
