package fi.hsl.jore4.mapmatching.util

import fi.hsl.jore4.mapmatching.util.GeoToolsUtils.transformCRS
import org.hamcrest.CoreMatchers.equalTo
import org.hamcrest.MatcherAssert.assertThat
import org.junit.jupiter.api.Test
import org.locationtech.jts.geom.Coordinate
import org.locationtech.jts.geom.GeometryFactory
import org.locationtech.jts.geom.Point
import org.locationtech.jts.geom.PrecisionModel

class GeoToolsUtilsTest {

    @Test
    fun testTransformCRS_withEpsgCode_from4326To3067() {
        val expectedDstPoint: Point =
            JTS_GEOM_FACTORY_3067.createPoint(Coordinate(385794.9708466177, 6672185.090453222))

        assertThat(transformCRS(SAMPLE_POINT_4326, "EPSG:4326", "EPSG:3067"),
                   equalTo(expectedDstPoint))
    }

    @Test
    fun testTransformCRS_withSRID_from4326To3067() {
        val expectedDstPoint: Point =
            JTS_GEOM_FACTORY_3067.createPoint(Coordinate(385794.9708466177, 6672185.090453222))

        assertThat(transformCRS(SAMPLE_POINT_4326, 4326, 3067),
                   equalTo(expectedDstPoint))
    }

    companion object {
        private val PRECISION_MODEL = PrecisionModel()

        private val JTS_GEOM_Factory_4326 = GeometryFactory(PRECISION_MODEL, 4326)
        private val JTS_GEOM_FACTORY_3067 = GeometryFactory(PRECISION_MODEL, 3067)

        private val SAMPLE_POINT_4326: Point = JTS_GEOM_Factory_4326.createPoint(Coordinate(24.94167, 60.17055))
    }
}
