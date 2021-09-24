package fi.hsl.jore4.mapmatching.config.jooq.converter

import org.geolatte.geom.C2D
import org.geolatte.geom.Geometry
import org.geolatte.geom.GeometryType
import org.geolatte.geom.LineString
import org.geolatte.geom.Point
import org.geolatte.geom.builder.DSL.c
import org.geolatte.geom.builder.DSL.linestring
import org.geolatte.geom.builder.DSL.point
import org.geolatte.geom.crs.CrsRegistry
import org.hamcrest.MatcherAssert.assertThat
import org.hamcrest.Matchers.`is`
import org.hamcrest.Matchers.equalTo
import org.hamcrest.Matchers.notNullValue
import org.hamcrest.Matchers.nullValue
import org.junit.jupiter.api.Test
import kotlin.test.assertFailsWith

class GeometryConverterTest {

    companion object {
        private val EPSG_3067 = CrsRegistry.getProjectedCoordinateReferenceSystemForEPSG(3067)

        private const val EWKB_POINT = "0101000020FB0B0000666666660C8C1741CDCCCC4CCE735941"
        private const val EWKB_LINESTRING =
            "0102000020FB0B000002000000666666660C8C1741CDCCCC4CCE73594100000000B0931741000000C0B9745941"
    }

    @Test
    fun testFrom_withPoint_whenTypeNameIsSetAsPoint() {
        val point: Geometry<C2D>? = GeometryConverter(GeometryType.POINT).from(EWKB_POINT)

        assertThat(point, `is`(notNullValue()))
        assertThat(point, equalTo(point(EPSG_3067, c(385795.1, 6672185.2))))
    }

    @Test
    fun testFrom_withPoint_whenTypeNameIsSetAsLineString() {
        val exception = assertFailsWith<IllegalArgumentException> {
            GeometryConverter(GeometryType.LINESTRING).from(EWKB_POINT)
        }
        assertThat(exception.message, equalTo("Unsupported geometry type: Point"))
    }

    @Test
    fun testFrom_withLineString_whenTypeNameIsSetAsLineString() {
        val lineString: Geometry<C2D>? = GeometryConverter(GeometryType.LINESTRING).from(EWKB_LINESTRING)

        assertThat(lineString, `is`(notNullValue()))
        assertThat(lineString,
                   equalTo(linestring(EPSG_3067, c(385795.1, 6672185.2), c(386284.0, 6673127.0))))
    }

    @Test
    fun testFrom_withLineString_whenTypeNameIsSetAsPoint() {
        val exception = assertFailsWith<IllegalArgumentException> {
            GeometryConverter(GeometryType.POINT).from(EWKB_LINESTRING)
        }
        assertThat(exception.message, equalTo("Unsupported geometry type: LineString"))
    }

    @Test
    fun testFrom_withNull() {
        assertThat(GeometryConverter(GeometryType.POINT).from(null), `is`(nullValue()))
        assertThat(GeometryConverter(GeometryType.LINESTRING).from(null), `is`(nullValue()))
    }

    @Test
    fun testTo_withPoint() {
        val point: Point<C2D> = point(EPSG_3067, c(385795.1, 6672185.2))

        assertThat(GeometryConverter.to(point), equalTo("SRID=3067;POINT(385795.1 6672185.2)"))
    }

    @Test
    fun testTo_withLineString() {
        val lineString: LineString<C2D> = linestring(EPSG_3067, c(385795.1, 6672185.2), c(386284.0, 6673127.0))

        assertThat(GeometryConverter.to(lineString),
                   equalTo("SRID=3067;LINESTRING(385795.1 6672185.2,386284 6673127)"))
    }

    @Test
    fun testTo_withNull() {
        assertThat(GeometryConverter.to(null), `is`(nullValue()))
    }
}
