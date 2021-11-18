package fi.hsl.jore4.mapmatching.model.matching

import org.geolatte.geom.G2D
import org.geolatte.geom.Point
import javax.validation.Valid
import javax.validation.constraints.AssertTrue
import javax.validation.constraints.Pattern

/**
 * Models a route point along a route to be matched against the infrastructure
 * network provided by the system.
 */
data class RoutePoint(val type: RoutePointType,
                      val location: Point<G2D>,
                      @field:Valid val stopPointInfo: PublicTransportStopInfo?) {

    data class PublicTransportStopInfo(val nationalId: Int?,
                                       @field:Pattern(regexp = "[\\w\\d-_ ]{1,10}") val passengerId: String?)

    val isStopPoint: Boolean
        get() = type == RoutePointType.PUBLIC_TRANSPORT_STOP

    @AssertTrue(message = "false")
    fun isStopPointInfoPresentOnlyIfTypeIsPublicTransportStop(): Boolean = isStopPoint || stopPointInfo == null
}
