package fi.hsl.jore4.mapmatching.util

import org.geotools.geometry.jts.JTS
import org.geotools.referencing.CRS
import org.locationtech.jts.geom.Geometry
import org.opengis.referencing.crs.CoordinateReferenceSystem

object GeoToolsUtils {

    fun transformFrom3067To4326(geometry: Geometry): Geometry {
        val sourceCRS: CoordinateReferenceSystem = CRS.decode("EPSG:3067", true)
        val targetCRS: CoordinateReferenceSystem = CRS.decode("EPSG:4326", true)

        val transform = CRS.findMathTransform(sourceCRS, targetCRS)
        return JTS.transform(geometry, transform)
    }
}
