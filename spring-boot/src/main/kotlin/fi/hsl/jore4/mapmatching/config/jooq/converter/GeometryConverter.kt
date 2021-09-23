package fi.hsl.jore4.mapmatching.config.jooq.converter

import org.locationtech.jts.geom.Geometry
import org.locationtech.jts.io.ParseException
import org.locationtech.jts.io.WKBReader
import org.locationtech.jts.io.WKTWriter

class GeometryConverter(private val typeName: String) {

    fun from(databaseObject: Any?): Geometry? = databaseObject?.let {
        try {
            val geom: Geometry = read(it.toString())
            require(typeName == geom.geometryType) { "Unsupported geometry type: ${geom.geometryType}" }
            geom
        } catch (e: ParseException) {
            throw IllegalArgumentException("Failed to parse EWKB string", e)
        }
    }

    companion object {

        private val WKB_READER = WKBReader()
        private val WKT_WRITER = WKTWriter(2)

        fun to(geom: Geometry?): String? = geom?.let {
            val wkt = WKT_WRITER.write(geom)

            // Note that the WKTWriter does _not_ support the PostGIS EWKT format.
            // Hence, we must manually add the SRID prefix.
            return "SRID=${geom.srid};$wkt"
        }

        internal fun read(hex: String): Geometry = WKB_READER.read(WKBReader.hexToBytes(hex))
    }
}
