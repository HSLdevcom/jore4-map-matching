package fi.hsl.jore4.mapmatching.util

import fi.hsl.jore4.mapmatching.util.GeolatteUtils.EPSG_3067
import fi.hsl.jore4.mapmatching.util.GeolatteUtils.transformFrom3067To4326
import org.geolatte.geom.C2D
import org.geolatte.geom.Point
import org.geolatte.geom.builder.DSL.c
import org.geolatte.geom.builder.DSL.g
import org.geolatte.geom.builder.DSL.point
import org.geolatte.geom.crs.CoordinateReferenceSystems.WGS84
import org.hamcrest.CoreMatchers.equalTo
import org.hamcrest.MatcherAssert.assertThat
import org.junit.jupiter.api.Test

class GeolatteUtilsTest {

    @Test
    fun testTransformFrom3067To4326() {
        val point: Point<C2D> = point(EPSG_3067, c(385795.1, 6672185.2))

        assertThat(transformFrom3067To4326(point),
                   equalTo(point(WGS84, g(24.941672264478964, 60.17055107308204))))
    }
}
