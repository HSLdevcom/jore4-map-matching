package fi.hsl.jore4.mapmatching.util

import org.geotools.api.referencing.crs.CoordinateReferenceSystem
import org.geotools.geometry.jts.JTS
import org.geotools.referencing.CRS
import org.locationtech.jts.geom.Geometry

object GeoToolsUtils {
    fun transformCRS(
        geometry: Geometry,
        sourceEpsgCode: String,
        targetEpsgCode: String
    ): Geometry =
        transformCRS(
            geometry,
            CRS.decode(sourceEpsgCode, true),
            CRS.decode(targetEpsgCode, true)
        )

    fun transformCRS(
        geometry: Geometry,
        sourceSRID: Int,
        targetSRID: Int
    ): Geometry =
        transformCRS(
            geometry,
            CRS.decode(getEpsgCode(sourceSRID), true),
            CRS.decode(getEpsgCode(targetSRID), true)
        )

    fun transformCRS(
        geometry: Geometry,
        sourceCRS: CoordinateReferenceSystem,
        targetCRS: CoordinateReferenceSystem
    ): Geometry {
        val transform = CRS.findMathTransform(sourceCRS, targetCRS)
        return JTS.transform(geometry, transform)
    }

    private fun getEpsgCode(srid: Int) = "EPSG:$srid"
}
