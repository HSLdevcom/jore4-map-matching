package fi.hsl.jore4.mapmatching.model

import org.geolatte.geom.G2D
import org.geolatte.geom.LineString
import org.geolatte.geom.Geometries.mkLineString
import org.geolatte.geom.crs.CoordinateReferenceSystems.WGS84

data class GeomTraversal(
    val geometry: LineString<G2D>,
    val forwardTraversal: Boolean // along (true) or against (false) the digitised direction of the geometry
) {
    fun getGeometryAccordingToDirectionOfTraversal(): LineString<G2D> = when (forwardTraversal) {
        true -> geometry
        false -> mkLineString(geometry.positions.reverse(), WGS84)
    }
}
