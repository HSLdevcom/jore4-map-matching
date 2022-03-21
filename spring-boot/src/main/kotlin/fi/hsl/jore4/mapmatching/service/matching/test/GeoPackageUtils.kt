package fi.hsl.jore4.mapmatching.service.matching.test

import org.geolatte.geom.jts.JTS
import org.geotools.data.collection.ListFeatureCollection
import org.geotools.data.simple.SimpleFeatureCollection
import org.geotools.feature.simple.SimpleFeatureBuilder
import org.geotools.feature.simple.SimpleFeatureTypeBuilder
import org.geotools.geopkg.FeatureEntry
import org.geotools.geopkg.GeoPackage
import org.geotools.referencing.crs.DefaultGeographicCRS
import org.locationtech.jts.geom.LineString
import org.opengis.feature.simple.SimpleFeature
import org.opengis.feature.simple.SimpleFeatureType
import java.io.File

object GeoPackageUtils {

    fun createGeoPackage(file: File, failedStopToStopSegments: List<SegmentMatchFailure>): GeoPackage {
        val geoPkg = GeoPackage(file)
        geoPkg.init()

        val entry = FeatureEntry()
        entry.description = "Failed stop-to-stop segments"
        geoPkg.add(entry, createFeatureCollection(failedStopToStopSegments))
        geoPkg.createSpatialIndex(entry)

        return geoPkg
    }

    private fun createFeatureCollection(failedStopToStopSegments: List<SegmentMatchFailure>): SimpleFeatureCollection {
        val type: SimpleFeatureType = createFeatureTypeForFailedStopToStopSegments()

        val features: List<SimpleFeature> = failedStopToStopSegments.map { createFeature(it, type) }

        return ListFeatureCollection(type, features)
    }

    private fun createFeatureTypeForFailedStopToStopSegments(): SimpleFeatureType {
        val builder = SimpleFeatureTypeBuilder()
        builder.name = "FailedStopToStopSegment"
        builder.crs = DefaultGeographicCRS.WGS84

        builder.add("geometry", LineString::class.java)
        builder.add("length", Double::class.java)
        builder.add("startStopId", String::class.java)
        builder.add("endStopId", String::class.java)
        builder.add("numberOfGeometryPoints", Integer::class.java)
        builder.add("numberOfRoutePoints", Integer::class.java)
        builder.add("numberOfReferencingRoutes", Integer::class.java)
        builder.add("referencingRoutes", String::class.java)

        return builder.buildFeatureType()
    }

    private fun createFeature(failedStopToStopSegment: SegmentMatchFailure, type: SimpleFeatureType): SimpleFeature {
        val builder = SimpleFeatureBuilder(type)

        failedStopToStopSegment.run {
            builder.add(JTS.to(sourceRouteGeometry))
            builder.add(sourceRouteLength)
            builder.add(startStopId)
            builder.add(endStopId)
            builder.add(sourceRouteGeometry.numPositions)
            builder.add(numberOfRoutePoints)
            builder.add(referencingRoutes.size)
            builder.add(referencingRoutes.joinToString(separator = ","))
        }

        return builder.buildFeature(failedStopToStopSegment.routeId)
    }
}
