package fi.hsl.jore4.mapmatching.config.jooq.converter

import org.jooq.Converter
import org.locationtech.jts.geom.Geometry
import org.locationtech.jts.geom.LineString

/**
 * Converts between LineString geometries and PostGIS EWKT/EWKB formats.
 */
class LineStringConverter : Converter<Any, LineString> {

    override fun from(databaseObject: Any?) = CONVERTER.from(databaseObject) as LineString?

    override fun to(userObject: LineString): Any? = GeometryConverter.to(userObject)

    override fun fromType() = Any::class.java

    override fun toType() = LineString::class.java

    companion object {
        val INSTANCE = LineStringConverter()

        private val CONVERTER = GeometryConverter(Geometry.TYPENAME_LINESTRING)
    }
}
