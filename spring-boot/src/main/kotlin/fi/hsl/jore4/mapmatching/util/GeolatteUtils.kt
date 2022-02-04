package fi.hsl.jore4.mapmatching.util

import fi.hsl.jore4.mapmatching.model.GeomTraversal
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

    fun mergeContinuousTraversals(traversals: List<GeomTraversal>): LineString<G2D> {
        if (traversals.isEmpty()) {
            throw IllegalArgumentException("Must have at least one GeomTraversal")
        }

        val linesToMerge: List<LineString<G2D>> =
            traversals.map(GeomTraversal::getGeometryAccordingToDirectionOfTraversal)

        val positionSequenceBuilder = PositionSequenceBuilders.variableSized(G2D::class.java)

        val firstLine: LineString<G2D> = linesToMerge.first()

        // Add all positions of the first line.
        firstLine.positions.forEach(positionSequenceBuilder::add)

        var prevLineLastPosition: G2D = firstLine.endPosition

        // Do not include the first line because it was already added.
        linesToMerge.drop(1).forEach { line ->

            // New line must start from the same position as the previous line ended at.
            if (line.startPosition != prevLineLastPosition) {
                throw IllegalStateException("Not topologically continuous sequence of lines")
            }

            // Add all positions except the first one (which was already added within processing of previous line).
            line.positions.drop(1).forEach(positionSequenceBuilder::add)

            prevLineLastPosition = line.endPosition
        }

        return mkLineString(positionSequenceBuilder.toPositionSequence(), WGS84)
    }
}
