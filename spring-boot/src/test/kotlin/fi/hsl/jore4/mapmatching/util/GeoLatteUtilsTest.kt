package fi.hsl.jore4.mapmatching.util

import fi.hsl.jore4.mapmatching.util.GeolatteUtils.EPSG_3067
import fi.hsl.jore4.mapmatching.util.GeolatteUtils.transformFromGeographicToProjected
import org.geolatte.geom.C2D
import org.geolatte.geom.G2D
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
    fun testTransformFromGeographicToProjected() {
        val geographicPoint: Point<G2D> = point(WGS84, g(24.94167, 60.17055))

        val expectedProjectedPoint: Point<C2D> = point(EPSG_3067, c(385794.9708466177, 6672185.090453222))

        assertThat(transformFromGeographicToProjected(geographicPoint, WGS84, EPSG_3067),
                   equalTo(expectedProjectedPoint))
    }
}
