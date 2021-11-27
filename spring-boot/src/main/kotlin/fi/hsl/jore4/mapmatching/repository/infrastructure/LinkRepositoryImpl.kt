package fi.hsl.jore4.mapmatching.repository.infrastructure

import fi.hsl.jore4.mapmatching.model.LatLng
import fi.hsl.jore4.mapmatching.model.VehicleType
import fi.hsl.jore4.mapmatching.util.GeolatteUtils.toEwkb
import fi.hsl.jore4.mapmatching.util.GeolatteUtils.toMultiPoint
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate
import org.springframework.stereotype.Repository
import org.springframework.transaction.annotation.Transactional
import java.sql.ResultSet

@Repository
class LinkRepositoryImpl @Autowired constructor(val jdbcTemplate: NamedParameterJdbcTemplate) : ILinkRepository {

    private data class ClosestLinkResult(val coordinateSeqNum: Int,
                                         val infrastructureLinkId: Long,
                                         val closestDistance: Double,
                                         val startNodeId: Long,
                                         val endNodeId: Long,
                                         val distanceToStartNode: Double,
                                         val distanceToEndNode: Double)

    @Transactional(readOnly = true)
    override fun findClosestLinks(coordinates: List<LatLng>,
                                  vehicleType: VehicleType,
                                  distanceInMeters: Double): Map<Int, SnapPointToLinkDTO> {

        if (coordinates.isEmpty()) {
            return emptyMap()
        }

        // Input coordinates are transformed to binary MultiPoint format (for compact representation).
        val ewkb: ByteArray = toEwkb(toMultiPoint(coordinates))

        val params = MapSqlParameterSource()
            .addValue("ewkb", ewkb)
            .addValue("vehicleType", vehicleType.value)
            .addValue("distance", distanceInMeters)

        val resultItems: List<ClosestLinkResult> =
            jdbcTemplate.query(FIND_CLOSEST_LINKS_SQL, params) { rs: ResultSet, _: Int ->
                val coordinateSeqNum = rs.getInt("seq")
                val infrastructureLinkId = rs.getLong("infrastructure_link_id")
                val closestDistance = rs.getDouble("closest_distance")
                val startNodeId = rs.getLong("start_node_id")
                val endNodeId = rs.getLong("end_node_id")
                val distanceToStartNode = rs.getDouble("start_node_distance")
                val distanceToEndNode = rs.getDouble("end_node_distance")

                ClosestLinkResult(coordinateSeqNum,
                                  infrastructureLinkId,
                                  closestDistance,
                                  startNodeId,
                                  endNodeId,
                                  distanceToStartNode,
                                  distanceToEndNode)
            }

        return resultItems.associateBy(keySelector = { it.coordinateSeqNum }, valueTransform = {
            val coordinateIndex = it.coordinateSeqNum - 1
            val point = coordinates[coordinateIndex]

            SnapPointToLinkDTO(point,
                               distanceInMeters,
                               SnappedLinkState(it.infrastructureLinkId,
                                                it.closestDistance,
                                                it.startNodeId,
                                                it.endNodeId,
                                                it.distanceToStartNode,
                                                it.distanceToEndNode))
        })
    }

    companion object {
        private const val FIND_CLOSEST_LINKS_SQL =
            "SELECT \n" +
                "    path[1] AS seq, \n" +
                "    closest_link.infrastructure_link_id, \n" +
                "    closest_link.distance AS closest_distance, \n" +
                "    closest_link.start_node_id, \n" +
                "    closest_link.end_node_id, \n" +
                "    coords.geom <-> start_node.the_geom AS start_node_distance, \n" +
                "    coords.geom <-> end_node.the_geom AS end_node_distance \n" +
                "FROM ( \n" +
                "    SELECT (g.gdump).path AS path, (g.gdump).geom AS geom \n" +
                "    FROM ( \n" +
                "        SELECT ST_Dump(ST_Transform(ST_GeomFromEWKB(:ewkb), 3067)) AS gdump \n" +
                "    ) AS g \n" +
                ") AS coords, LATERAL ( \n" +
                "    SELECT \n" +
                "        link.infrastructure_link_id, \n" +
                "        link.start_node_id, \n" +
                "        link.end_node_id, \n" +
                "        coords.geom <-> link.geom AS distance \n" +
                "    FROM routing.infrastructure_link link \n" +
                "    INNER JOIN routing.infrastructure_link_safely_traversed_by_vehicle_type safe \n" +
                "        ON safe.infrastructure_link_id = link.infrastructure_link_id \n" +
                "    WHERE ST_DWithin(coords.geom, link.geom, :distance) \n" +
                "        AND safe.vehicle_type = :vehicleType \n" +
                "    ORDER BY distance \n" +
                "    LIMIT 1 \n" +
                ") AS closest_link \n" +
                "INNER JOIN routing.infrastructure_link_vertices_pgr start_node ON start_node.id = closest_link.start_node_id \n" +
                "INNER JOIN routing.infrastructure_link_vertices_pgr end_node ON end_node.id = closest_link.end_node_id \n" +
                "ORDER BY seq ASC; \n"
    }
}
