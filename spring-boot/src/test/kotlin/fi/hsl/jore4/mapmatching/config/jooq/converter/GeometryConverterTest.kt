package fi.hsl.jore4.mapmatching.config.jooq.converter

import org.hamcrest.MatcherAssert.assertThat
import org.hamcrest.Matchers.`is`
import org.hamcrest.Matchers.equalTo
import org.hamcrest.Matchers.notNullValue
import org.hamcrest.Matchers.nullValue
import org.junit.jupiter.api.Test
import org.locationtech.jts.geom.Coordinate
import org.locationtech.jts.geom.Geometry
import org.locationtech.jts.geom.GeometryFactory
import org.locationtech.jts.geom.LineString
import org.locationtech.jts.geom.Point
import org.locationtech.jts.geom.PrecisionModel
import kotlin.test.assertFailsWith

class GeometryConverterTest {

    companion object {
        private const val EWKB_POINT = "01010000A0E6100000E9254A6BA1154E404A9E1B3507F338400000000000000000"
        private const val EWKB_LINESTRING =
            "01020000A0E610000002000000E9254A6BA1154E404A9E1B3507F338400000000000000000BE9150659E154E40103A46840FF238400000000000000000"
    }

    private val geomFactory = GeometryFactory(PrecisionModel(), 4326)

    @Test
    fun testFrom_withPoint_whenTypeNameIsSetAsPoint() {
        val point: Geometry? = GeometryConverter(Geometry.TYPENAME_POINT).from(EWKB_POINT)

        assertThat(point, `is`(notNullValue()))
        assertThat(point, equalTo(geomFactory.createPoint(Coordinate(60.16898862, 24.949328727))))
    }

    @Test
    fun testFrom_withPoint_whenTypeNameIsSetAsLineString() {
        val exception = assertFailsWith<IllegalArgumentException> {
            GeometryConverter(Geometry.TYPENAME_LINESTRING).from(EWKB_POINT)
        }
        assertThat(exception.message, equalTo("Unsupported geometry type: Point"))
    }

    @Test
    fun testFrom_withLineString_whenTypeNameIsSetAsLineString() {
        val lineString: Geometry? = GeometryConverter(Geometry.TYPENAME_LINESTRING).from(EWKB_LINESTRING)

        assertThat(lineString, `is`(notNullValue()))
        assertThat(lineString,
                   equalTo(geomFactory.createLineString(arrayOf(Coordinate(60.168988620, 24.949328727),
                                                                Coordinate(60.168896355, 24.945549266)))))
    }

    @Test
    fun testFrom_withLineString_whenTypeNameIsSetAsPoint() {
        val exception = assertFailsWith<IllegalArgumentException> {
            GeometryConverter(Geometry.TYPENAME_POINT).from(EWKB_LINESTRING)
        }
        assertThat(exception.message, equalTo("Unsupported geometry type: LineString"))
    }

    @Test
    fun testFrom_withNull() {
        assertThat(GeometryConverter(Geometry.TYPENAME_POINT).from(null), `is`(nullValue()))
        assertThat(GeometryConverter(Geometry.TYPENAME_LINESTRING).from(null), `is`(nullValue()))
    }

    @Test
    fun testTo_withPoint() {
        val point: Point = geomFactory.createPoint(Coordinate(24.123, 60.456))

        assertThat(GeometryConverter.to(point), equalTo("SRID=4326;POINT (24.123 60.456)"))
    }

    @Test
    fun testTo_withLineString() {
        val lineString: LineString =
            geomFactory.createLineString(arrayOf(Coordinate(24.123, 60.234), Coordinate(24.345, 60.456)))

        assertThat(
            GeometryConverter.to(lineString),
            equalTo("SRID=4326;LINESTRING (24.123 60.234, 24.345 60.456)"))
    }

    @Test
    fun testTo_withNull() {
        assertThat(GeometryConverter.to(null), `is`(nullValue()))
    }
}
