package fi.hsl.jore4.mapmatching.util

import fi.hsl.jore4.mapmatching.model.LatLng
import org.geolatte.geom.ByteBuffer
import org.geolatte.geom.G2D
import org.geolatte.geom.Geometries.mkLineString
import org.geolatte.geom.Geometries.mkPoint
import org.geolatte.geom.Geometry
import org.geolatte.geom.GeometryType
import org.geolatte.geom.LineString
import org.geolatte.geom.Point
import org.geolatte.geom.PositionSequenceBuilders
import org.geolatte.geom.codec.Wkb
import org.geolatte.geom.crs.CoordinateReferenceSystems.WGS84
import java.math.BigDecimal
import java.math.RoundingMode.HALF_UP
import kotlin.math.max

object GeolatteUtils {
    fun toPoint(position: G2D): Point<G2D> = mkPoint(position, WGS84)

    fun toPoints(coords: List<LatLng>): List<Point<G2D>> = coords.map(LatLng::toGeolattePoint)

    fun toEwkb(geometry: Geometry<*>): ByteArray = Wkb.toWkb(geometry).toByteArray()

    fun fromEwkb(wkb: ByteArray): Geometry<*> = Wkb.fromWkb(ByteBuffer.from(wkb))

    fun extractLineStringG2D(geometry: Geometry<*>): LineString<G2D> {
        require(geometry.geometryType == GeometryType.LINESTRING) {
            "Geometry does not represent a 2D LineString: $geometry"
        }

        return geometry.`as`(G2D::class.java) as LineString<G2D>
    }

    fun mergeContinuousLines(linesToMerge: List<LineString<G2D>>): LineString<G2D> {
        require(linesToMerge.isNotEmpty()) { "Must have at least one LineString" }

        val positionSequenceBuilder = PositionSequenceBuilders.variableSized(G2D::class.java)

        val firstLine: LineString<G2D> = linesToMerge.first()

        // Add all positions of the first line.
        firstLine.positions.forEach(positionSequenceBuilder::add)

        var prevLineLastPosition: G2D = firstLine.endPosition

        // Do not include the first line because it was already added.
        linesToMerge.drop(1).forEach { line ->

            // New line must start from the same position as the previous line ended at.
            require(line.startPosition == prevLineLastPosition) {
                "Not topologically continuous sequence of lines"
            }

            // Add all positions except the first one (which was already added within processing of previous line).
            line.positions.drop(1).forEach(positionSequenceBuilder::add)

            prevLineLastPosition = line.endPosition
        }

        return mkLineString(positionSequenceBuilder.toPositionSequence(), WGS84)
    }

    fun roundCoordinates(
        line: LineString<G2D>,
        decimalPrecision: Int
    ): LineString<G2D> {
        val positionSequenceBuilder = PositionSequenceBuilders.variableSized(G2D::class.java)

        fun round(num: Double): Double {
            val bd = BigDecimal.valueOf(num)
            val numDecimals: Int = max(0, bd.stripTrailingZeros().scale())

            return if (numDecimals <= decimalPrecision) num else bd.setScale(decimalPrecision, HALF_UP).toDouble()
        }

        line.positions.forEach { pos: G2D ->
            positionSequenceBuilder.add(G2D(round(pos.lon), round(pos.lat)))
        }

        return mkLineString(positionSequenceBuilder.toPositionSequence(), WGS84)
    }
}
