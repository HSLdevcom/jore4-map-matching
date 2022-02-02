package fi.hsl.jore4.mapmatching.util

import org.assertj.core.api.Assertions.assertThat
import fi.hsl.jore4.mapmatching.util.GeolatteUtils.EPSG_3067
import fi.hsl.jore4.mapmatching.util.GeolatteUtils.transformFromGeographicToProjected
import org.geolatte.geom.C2D
import org.geolatte.geom.G2D
import org.geolatte.geom.Geometries.mkLineString
import org.geolatte.geom.LineString
import org.geolatte.geom.Point
import org.geolatte.geom.PositionSequenceBuilders
import org.geolatte.geom.builder.DSL.c
import org.geolatte.geom.builder.DSL.g
import org.geolatte.geom.builder.DSL.point
import org.geolatte.geom.crs.CoordinateReferenceSystems.WGS84
import org.hamcrest.CoreMatchers.equalTo
import org.hamcrest.MatcherAssert.assertThat
import org.junit.jupiter.api.Test

class GeolatteUtilsTest {

    @Test
    fun testRoundCoordinates() {
        val testCoordinates = PositionSequenceBuilders.variableSized(G2D::class.java)
            .add(12.3, 34.5)
            .add(12.34, 34.56)
            .add(12.345, 34.567)
            .add(12.3456, 34.5678)
            .add(12.34567, 34.56789)
            .add(12.345678, 34.567890)
            .add(12.3456789, 34.5678901)

        val testLine: LineString<G2D> = mkLineString(testCoordinates.toPositionSequence(), WGS84)

        val outputLine: LineString<G2D> = GeolatteUtils.roundCoordinates(testLine, 5)

        val expectedCoordinates = PositionSequenceBuilders.variableSized(G2D::class.java)
            .add(12.3, 34.5)
            .add(12.34, 34.56)
            .add(12.345, 34.567)
            .add(12.3456, 34.5678)
            .add(12.34567, 34.56789)
            .add(12.34568, 34.56789)
            .add(12.34568, 34.56789)

        val expectedLine: LineString<G2D> = mkLineString(expectedCoordinates.toPositionSequence(), WGS84)

        assertThat(outputLine).isEqualTo(expectedLine)
    }

    @Test
    fun testTransformFromGeographicToProjected() {
        val geographicPoint: Point<G2D> = point(WGS84, g(24.94167, 60.17055))

        val expectedProjectedPoint: Point<C2D> = point(EPSG_3067, c(385794.9708466177, 6672185.090453222))

        assertThat(transformFromGeographicToProjected(geographicPoint, WGS84, EPSG_3067),
                   equalTo(expectedProjectedPoint))
    }
}
