package fi.hsl.jore4.mapmatching.util

import fi.hsl.jore4.mapmatching.util.GeoToolsUtils.transformFrom3067To4326
import org.hamcrest.CoreMatchers.equalTo
import org.hamcrest.MatcherAssert.assertThat
import org.junit.jupiter.api.Test
import org.locationtech.jts.geom.Coordinate
import org.locationtech.jts.geom.GeometryFactory
import org.locationtech.jts.geom.Point
import org.locationtech.jts.geom.PrecisionModel

class GeoToolsUtilsTest {

    @Test
    fun testTransformFrom3067To4326() {
        val precisionModel = PrecisionModel()
        val srcGeomFactory = GeometryFactory(precisionModel, 3067)
        val dstGeomFactory = GeometryFactory(precisionModel, 4326)

        val point: Point = srcGeomFactory.createPoint(Coordinate(385795.1, 6672185.2))

        assertThat(transformFrom3067To4326(point),
                   equalTo(dstGeomFactory.createPoint(Coordinate(24.941672264478964, 60.17055107308204))))
    }
}
