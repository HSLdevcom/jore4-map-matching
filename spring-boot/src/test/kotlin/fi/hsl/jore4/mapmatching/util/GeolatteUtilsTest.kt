package fi.hsl.jore4.mapmatching.util

import org.assertj.core.api.Assertions.assertThat
import org.geolatte.geom.G2D
import org.geolatte.geom.Geometries.mkLineString
import org.geolatte.geom.LineString
import org.geolatte.geom.PositionSequenceBuilders
import org.geolatte.geom.crs.CoordinateReferenceSystems.WGS84
import org.junit.jupiter.api.Test

class GeolatteUtilsTest {

    @Test
    fun testRoundCoordinates() {
        val testCoordinates = PositionSequenceBuilders.variableSized(G2D::class.java)
            .add(12.3,        34.5)
            .add(12.34,       34.56)
            .add(12.345,      34.567)
            .add(12.3456,     34.5678)
            .add(12.34567,    34.56789)
            .add(12.345678,   34.567890)
            .add(12.3456789,  34.5678901)

        val testLine: LineString<G2D> = mkLineString(testCoordinates.toPositionSequence(), WGS84)

        val outputLine: LineString<G2D> = GeolatteUtils.roundCoordinates(testLine, 5)

        val expectedCoordinates = PositionSequenceBuilders.variableSized(G2D::class.java)
            .add(12.3,     34.5)
            .add(12.34,    34.56)
            .add(12.345,   34.567)
            .add(12.3456,  34.5678)
            .add(12.34567, 34.56789)
            .add(12.34568, 34.56789)
            .add(12.34568, 34.56789)

        val expectedLine: LineString<G2D> = mkLineString(expectedCoordinates.toPositionSequence(), WGS84)

        assertThat(outputLine).isEqualTo(expectedLine)
    }
}
