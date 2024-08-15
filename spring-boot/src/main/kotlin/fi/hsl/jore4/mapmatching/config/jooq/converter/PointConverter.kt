package fi.hsl.jore4.mapmatching.config.jooq.converter

import org.geolatte.geom.C2D
import org.geolatte.geom.GeometryType
import org.geolatte.geom.Point
import org.jooq.Converter

/**
 * Converts between Point geometries and PostGIS EWKT/EWKB formats.
 */
class PointConverter : Converter<Any, Point<C2D>> {
    override fun from(databaseObject: Any?) = CONVERTER.from(databaseObject) as Point?

    override fun to(userObject: Point<C2D>?): Any? = GeometryConverter.to(userObject)

    override fun fromType() = Any::class.java

    @Suppress("UNCHECKED_CAST")
    override fun toType() = Point::class.java as Class<Point<C2D>>

    companion object {
        val INSTANCE = PointConverter()

        private val CONVERTER = GeometryConverter(GeometryType.POINT)
    }
}
