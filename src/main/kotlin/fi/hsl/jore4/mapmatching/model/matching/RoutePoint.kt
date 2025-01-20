package fi.hsl.jore4.mapmatching.model.matching

import com.fasterxml.jackson.annotation.JsonSubTypes
import com.fasterxml.jackson.annotation.JsonTypeInfo
import jakarta.validation.constraints.Pattern
import org.geolatte.geom.G2D
import org.geolatte.geom.Point

/**
 * Models a route point along a route to be matched against the infrastructure
 * network provided by the system.
 */
@JsonTypeInfo(
    use = JsonTypeInfo.Id.NAME,
    include = JsonTypeInfo.As.PROPERTY,
    property = "type"
)
@JsonSubTypes(
    JsonSubTypes.Type(value = RouteStopPoint::class, name = "PUBLIC_TRANSPORT_STOP"),
    JsonSubTypes.Type(value = RouteJunctionPoint::class, name = "ROAD_JUNCTION"),
    JsonSubTypes.Type(value = RouteOtherPoint::class, name = "OTHER")
)
sealed interface RoutePoint {
    val location: Point<G2D>
}

data class RouteStopPoint(
    override val location: Point<G2D>,
    val projectedLocation: Point<G2D>?,
    @field:Pattern(regexp = "[\\w\\d-_ ]{1,10}") val passengerId: String,
    val nationalId: Int?
) : RoutePoint

data class RouteJunctionPoint(
    override val location: Point<G2D>
) : RoutePoint

data class RouteOtherPoint(
    override val location: Point<G2D>
) : RoutePoint
