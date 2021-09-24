package fi.hsl.jore4.mapmatching.config.jooq.converter

import org.geolatte.geom.ByteBuffer
import org.geolatte.geom.C2D
import org.geolatte.geom.Geometry
import org.geolatte.geom.GeometryType
import org.geolatte.geom.codec.Wkb
import org.geolatte.geom.codec.Wkt
import org.locationtech.jts.io.ParseException

class GeometryConverter(private val geometryType: GeometryType) {

    fun from(databaseObject: Any?): Geometry<C2D>? = databaseObject?.let {
        try {
            val geom: Geometry<C2D> = read(it.toString())
            require(geometryType == geom.geometryType) {
                "Unsupported geometry type: ${geom.geometryType.camelCased}"
            }
            geom
        } catch (e: ParseException) {
            throw IllegalArgumentException("Failed to parse EWKB string", e)
        }
    }

    companion object {

        fun to(geom: Geometry<C2D>?): String? = geom?.let { Wkt.toWkt(geom) }

        internal fun read(hex: String): Geometry<C2D> = Wkb.fromWkb(ByteBuffer.from(hex)) as Geometry<C2D>
    }
}
