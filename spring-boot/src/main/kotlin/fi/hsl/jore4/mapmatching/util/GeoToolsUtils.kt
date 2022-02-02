package fi.hsl.jore4.mapmatching.util

import org.geotools.geometry.jts.JTS
import org.geotools.referencing.CRS
import org.locationtech.jts.geom.Geometry
import org.opengis.referencing.crs.CoordinateReferenceSystem

object GeoToolsUtils {

    fun transformCRS(geometry: Geometry, sourceEpsgCode: String, targetEpsgCode: String): Geometry {
        return transformCRS(geometry,
                            CRS.decode(sourceEpsgCode, true),
                            CRS.decode(targetEpsgCode, true))
    }

    fun transformCRS(geometry: Geometry, sourceSRID: Int, targetSRID: Int): Geometry {
        return transformCRS(geometry,
                            CRS.decode(getEpsgCode(sourceSRID), true),
                            CRS.decode(getEpsgCode(targetSRID), true))
    }

    fun transformCRS(geometry: Geometry,
                     sourceCRS: CoordinateReferenceSystem,
                     targetCRS: CoordinateReferenceSystem): Geometry {

        val transform = CRS.findMathTransform(sourceCRS, targetCRS)
        return JTS.transform(geometry, transform)
    }

    private fun getEpsgCode(srid: Int) = "EPSG:$srid"
}
