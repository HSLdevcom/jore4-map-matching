package fi.hsl.jore4.mapmatching.service.matching.test

import org.geolatte.geom.jts.JTS
import org.geotools.api.feature.simple.SimpleFeature
import org.geotools.api.feature.simple.SimpleFeatureType
import org.geotools.data.collection.ListFeatureCollection
import org.geotools.feature.simple.SimpleFeatureBuilder
import org.geotools.feature.simple.SimpleFeatureTypeBuilder
import org.geotools.geopkg.FeatureEntry
import org.geotools.geopkg.GeoPackage
import org.geotools.referencing.crs.DefaultGeographicCRS
import org.locationtech.jts.geom.LineString
import java.io.File

object GeoPackageUtils {
    fun createGeoPackage(
        file: File,
        failedSegments: List<SegmentMatchFailure>
    ): GeoPackage {
        val geoPkg = GeoPackage(file)
        geoPkg.init()

        try {
            failedSegments.forEach { segment ->
                val featureTypeName: String = segment.routeId
                val featureType: SimpleFeatureType = createFeatureTypeForFailedSegment(featureTypeName)

                val entry = FeatureEntry()
                entry.identifier = segment.routeId
                entry.description = "Failed segment within map-matching"

                geoPkg.add(entry, createFeatureCollection(segment, featureType))

                // Not sure, if this is really needed.
                geoPkg.createSpatialIndex(entry)
            }

            return geoPkg
        } catch (ex: Exception) {
            geoPkg.close()
            throw ex
        }
    }

    private fun createFeatureTypeForFailedSegment(name: String): SimpleFeatureType {
        val builder = SimpleFeatureTypeBuilder()
        builder.name = name
        builder.crs = DefaultGeographicCRS.WGS84

        builder.add("geometry", LineString::class.java)
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
        featureType: SimpleFeatureType
    ): ListFeatureCollection {
        val feature: SimpleFeature = createFeature(failedSegment, featureType)

        return ListFeatureCollection(featureType, feature)
    }

    private fun createFeature(
        failedSegment: SegmentMatchFailure,
        type: SimpleFeatureType
    ): SimpleFeature {
        val builder = SimpleFeatureBuilder(type)

        failedSegment.run {
            val lineString: LineString = JTS.to(sourceRouteGeometry)

            builder.add(lineString)
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
