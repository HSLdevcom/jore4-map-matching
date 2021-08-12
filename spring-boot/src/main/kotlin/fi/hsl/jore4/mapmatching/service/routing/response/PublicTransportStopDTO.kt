package fi.hsl.jore4.mapmatching.service.routing.response

import fi.hsl.jore4.mapmatching.model.ExternalLinkReference
import fi.hsl.jore4.mapmatching.util.MultilingualString
import org.geolatte.geom.G2D
import org.geolatte.geom.Point

data class PublicTransportStopDTO(val publicTransportStopId: Long,
                                  val publicTransportStopNationalId: Int,
                                  val location: Point<G2D>,
                                  val name: MultilingualString,
                                  val locatedOnLink: LinkReferenceDTO) {

    data class LinkReferenceDTO(val infrastructureLinkId: Long, val externalLinkRef: ExternalLinkReference)
}
