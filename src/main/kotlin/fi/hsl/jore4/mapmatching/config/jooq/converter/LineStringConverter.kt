package fi.hsl.jore4.mapmatching.config.jooq.converter

import org.geolatte.geom.C2D
import org.geolatte.geom.GeometryType
import org.geolatte.geom.LineString
import org.jooq.Converter

/**
 * Converts between LineString geometries and PostGIS EWKT/EWKB formats.
 */
class LineStringConverter : Converter<Any, LineString<C2D>> {
    override fun from(databaseObject: Any?) = CONVERTER.from(databaseObject) as LineString?

    override fun to(userObject: LineString<C2D>): Any? = GeometryConverter.to(userObject)

    override fun fromType() = Any::class.java

    @Suppress("UNCHECKED_CAST")
    override fun toType() = LineString::class.java as Class<LineString<C2D>>

    companion object {
        val INSTANCE = LineStringConverter()

        private val CONVERTER = GeometryConverter(GeometryType.LINESTRING)
    }
}
