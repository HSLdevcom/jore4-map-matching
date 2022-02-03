package fi.hsl.jore4.mapmatching.util

import fi.hsl.jore4.mapmatching.model.LatLng
import org.geolatte.geom.ByteBuffer
import org.geolatte.geom.G2D
import org.geolatte.geom.Geometries.mkLineString
import org.geolatte.geom.Geometry
import org.geolatte.geom.GeometryType
import org.geolatte.geom.LineString
import org.geolatte.geom.Point
import org.geolatte.geom.PositionSequenceBuilders
import org.geolatte.geom.codec.Wkb
import org.geolatte.geom.crs.CoordinateReferenceSystems.WGS84

object GeolatteUtils {

    fun toPoints(coords: List<LatLng>): List<Point<G2D>> = coords.map(LatLng::toGeolattePoint)

    fun toEwkb(geometry: Geometry<*>): ByteArray = Wkb.toWkb(geometry).toByteArray()

    fun fromEwkb(wkb: ByteArray): Geometry<*> = Wkb.fromWkb(ByteBuffer.from(wkb))

    fun extractLineStringG2D(geometry: Geometry<*>): LineString<G2D> {
        if (geometry.geometryType != GeometryType.LINESTRING && geometry.dimension != 2) {
            throw IllegalArgumentException("Geometry does not represent a 2D LineString: $geometry")
        }

        return geometry.`as`(G2D::class.java) as LineString<G2D>
    }

    fun mergeContinuousLines(lines: List<LineString<G2D>>): LineString<G2D> {
        if (lines.isEmpty()) {
            throw IllegalArgumentException("Parameter list must contain at least one LineString")
        }

        val positionSequenceBuilder = PositionSequenceBuilders.variableSized(G2D::class.java)

        // Add all positions of the first line.
        lines.first().positions.forEach(positionSequenceBuilder::add)

        var prevLineLastPosition: G2D = lines.first().endPosition

        // Drop 1 because the first line was already added.
        lines.drop(1).forEach { line ->

            // New line must have same position as the first element than what was the last
            // position of the previous line.
            if (line.startPosition != prevLineLastPosition) {
                throw IllegalStateException("Not continuos line sequence")
            }

            // Add all positions except the first one (which was already added within previous line).
            line.positions.drop(1).forEach(positionSequenceBuilder::add)

            prevLineLastPosition = line.endPosition
        }

        return mkLineString(positionSequenceBuilder.toPositionSequence(), WGS84)
    }
}
