package fi.hsl.jore4.mapmatching.repository.routing

import fi.hsl.jore4.mapmatching.model.InfrastructureLinkId
import org.geolatte.geom.G2D
import org.geolatte.geom.LineString

/**
 * Container for data that is used to geometrically restrict the target set of
 * infrastructure links in map-matching. The given line geometry is expanded in
 * all directions by the amount given by [bufferRadiusInMeters] parameter. As a
 * result, a polygon is created. The infrastructure links that are contained
 * inside the polygon area are eligible to be used as components of the
 * resulting route in map-matching.
 *
 * @property lineGeometry LineString geometry to be expanded
 * @property bufferRadiusInMeters The distance with which the LineString
 * geometry is expanded in all directions
 * @property infrastructureLinkIdAtStart The identifier of the first
 * infrastructure link on a route. The first link may lie outside the buffer
 * area. Therefore, it needs to be referenced explicitly.
 * @property infrastructureLinkIdAtEnd The identifier of the last
 * infrastructure link on a route. The last link may lie outside the buffer
 * area. Therefore, it needs to be referenced explicitly.
 */
data class BufferAreaRestriction(val lineGeometry: LineString<G2D>,
                                 val bufferRadiusInMeters: Double,
                                 val infrastructureLinkIdAtStart: InfrastructureLinkId,
                                 val infrastructureLinkIdAtEnd: InfrastructureLinkId)