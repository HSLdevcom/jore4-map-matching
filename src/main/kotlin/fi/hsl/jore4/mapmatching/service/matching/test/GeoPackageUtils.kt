package fi.hsl.jore4.mapmatching.service.matching.test

import fi.hsl.jore4.mapmatching.util.GeoToolsUtils.transformCRS
import org.geolatte.geom.jts.JTS
import org.geotools.api.feature.simple.SimpleFeature
import org.geotools.api.feature.simple.SimpleFeatureType
import org.geotools.data.collection.ListFeatureCollection
import org.geotools.feature.simple.SimpleFeatureBuilder
import org.geotools.feature.simple.SimpleFeatureTypeBuilder
import org.geotools.geometry.jts.Geometries
import org.geotools.geopkg.FeatureEntry
import org.geotools.geopkg.GeoPackage
import org.geotools.referencing.crs.DefaultGeographicCRS
import org.locationtech.jts.geom.Geometry
import org.locationtech.jts.geom.LineString
import java.io.File

object GeoPackageUtils {
    private const val GEOMETRY_COLUMN_NAME = "geometry"

    fun createGeoPackage(
        file: File,
        failedSegments: List<SegmentMatchFailure>,
        isBufferPolygonInsteadOfLineString: Boolean
    ): GeoPackage {
        val geoPkg = GeoPackage(file)
        geoPkg.init()

        try {
            failedSegments.forEach { segment ->
                val featureTypeName: String = segment.routeId
                val featureType: SimpleFeatureType =
                    createFeatureTypeForFailedSegment(featureTypeName, isBufferPolygonInsteadOfLineString)

                val entry = FeatureEntry()
                entry.identifier = segment.routeId
                entry.description =
                    if (isBufferPolygonInsteadOfLineString) {
                        "Buffered geometry for failed ${segment.routeId} segment within map-matching"
                    } else {
                        "LineString for failed ${segment.routeId} segment within map-matching"
                    }

                geoPkg.add(entry, createFeatureCollection(segment, featureType, isBufferPolygonInsteadOfLineString))

                // Not sure, if this is really needed.
                geoPkg.createSpatialIndex(entry)
            }

            return geoPkg
        } catch (ex: Exception) {
            geoPkg.close()
            throw ex
        }
    }

    private fun createFeatureTypeForFailedSegment(
        name: String,
        isBufferPolygonInsteadOfLineString: Boolean
    ): SimpleFeatureType {
        val builder = SimpleFeatureTypeBuilder()
        builder.name = name

        val geomType = if (isBufferPolygonInsteadOfLineString) Geometries.POLYGON else Geometries.LINESTRING

        builder.add(GEOMETRY_COLUMN_NAME, geomType.binding, DefaultGeographicCRS.WGS84)
        builder.add("length", Double::class.java)
        builder.add("startStopId", String::class.java)
        builder.add("endStopId", String::class.java)
        builder.add("numberOfGeometryPoints", Integer::class.java)
        builder.add("numberOfRoutePoints", Integer::class.java)
        builder.add("numberOfRoutesPassingThrough", Integer::class.java)
        builder.add("routesPassingThrough", String::class.java)

        return builder.buildFeatureType()
    }

    private fun createFeatureCollection(
        failedSegment: SegmentMatchFailure,
        featureType: SimpleFeatureType,
        isBufferPolygonInsteadOfLineString: Boolean
    ): ListFeatureCollection {
        val feature: SimpleFeature = createFeature(failedSegment, featureType, isBufferPolygonInsteadOfLineString)

        return ListFeatureCollection(featureType, feature)
    }

    private fun createFeature(
        failedSegment: SegmentMatchFailure,
        type: SimpleFeatureType,
        isBufferPolygonInsteadOfLineString: Boolean
    ): SimpleFeature {
        val builder = SimpleFeatureBuilder(type)

        failedSegment.run {
            val lineString: LineString = JTS.to(sourceRouteGeometry)

            if (isBufferPolygonInsteadOfLineString) {
                val lineString3067: Geometry = transformCRS(lineString, 4326, 3067)
                val bufferPolygon3067: Geometry = lineString3067.buffer(bufferRadius.value)
                val bufferPolygon: Geometry = transformCRS(bufferPolygon3067, 3067, 4326)

                builder.add(bufferPolygon)
            } else {
                builder.add(lineString)
            }

            builder.add(sourceRouteLength)
            builder.add(startStopId)
            builder.add(endStopId)
            builder.add(sourceRouteGeometry.numPositions)
            builder.add(numberOfRoutePoints)
            builder.add(referencingRoutes.size)
            builder.add(referencingRoutes.joinToString(separator = " "))
        }

        return builder.buildFeature(failedSegment.routeId)
    }
}
