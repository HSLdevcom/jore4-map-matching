package fi.hsl.jore4.mapmatching.model

import org.geolatte.geom.G2D
import org.geolatte.geom.Point
import org.geolatte.geom.builder.DSL.g
import org.geolatte.geom.builder.DSL.point
import org.geolatte.geom.crs.CoordinateReferenceSystems.WGS84

data class LatLng(val lat: Double, val lng: Double) {

    fun toGeolattePoint(): Point<G2D> = point(WGS84, g(lng, lat))

    companion object {
        fun fromPointG2D(point: Point<G2D>) = LatLng(point.position.lat, point.position.lon)
    }
}
