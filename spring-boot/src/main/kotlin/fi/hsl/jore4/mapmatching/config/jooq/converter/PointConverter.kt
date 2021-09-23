package fi.hsl.jore4.mapmatching.config.jooq.converter

import org.jooq.Converter
import org.locationtech.jts.geom.Geometry
import org.locationtech.jts.geom.Point

/**
 * Converts between Point geometries and PostGIS EWKT/EWKB formats.
 */
class PointConverter : Converter<Any, Point> {

    override fun from(databaseObject: Any?) = CONVERTER.from(databaseObject) as Point?

    override fun to(userObject: Point?): Any? = GeometryConverter.to(userObject)

    override fun fromType() = Any::class.java

    override fun toType() = Point::class.java

    companion object {
        val INSTANCE = PointConverter()

        private val CONVERTER = GeometryConverter(Geometry.TYPENAME_POINT)
    }
}
