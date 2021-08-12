package fi.hsl.jore4.mapmatching.repository.infrastructure

import fi.hsl.jore4.mapmatching.model.LatLng
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
                                         val closerNodeId: Long,
                                         val furtherNodeId: Long)

    @Transactional(readOnly = true)
    override fun findClosestLinks(coordinates: List<LatLng>, distanceInMeters: Double): Map<Int, SnapToLinkDTO> {
        if (coordinates.isEmpty()) {
            return emptyMap()
        }

        // Input coordinates are transformed to binary MultiPoint format (for compact representation).
        val ewkb: ByteArray = toEwkb(toMultiPoint(coordinates))

        val params = MapSqlParameterSource()
            .addValue("ewkb", ewkb)
            .addValue("distance", distanceInMeters)

        val resultItems: List<ClosestLinkResult> =
            jdbcTemplate.query(FIND_CLOSEST_LINKS_SQL, params) { rs: ResultSet, _: Int ->
                val coordinateSeqNum = rs.getInt("seq")
                val infrastructureLinkId = rs.getLong("infrastructure_link_id")
                val closestDistance = rs.getDouble("closest_distance")
                val closerNodeId = rs.getLong("closer_node_id")
                val furtherNodeId = rs.getLong("further_node_id")

                ClosestLinkResult(coordinateSeqNum, infrastructureLinkId, closestDistance, closerNodeId, furtherNodeId)
            }

        return resultItems.associateBy(keySelector = { it.coordinateSeqNum }, valueTransform = {
            val coordinateIndex = it.coordinateSeqNum - 1
            val point = coordinates[coordinateIndex]

            SnapToLinkDTO(point,
                          distanceInMeters,
                          it.infrastructureLinkId,
                          it.closestDistance,
                          it.closerNodeId,
                          it.furtherNodeId)
        })
    }

    companion object {
        private const val FIND_CLOSEST_LINKS_SQL =
            "SELECT \n" +
                "    path[1] AS seq, \n" +
                "    closest_link.infrastructure_link_id, \n" +
                "    closest_link.distance AS closest_distance, \n" +
                "    CASE \n" +
                "        WHEN node_distance.start_node <= node_distance.end_node THEN start_node.id \n" +
                "        ELSE end_node.id \n" +
                "    END AS closer_node_id, \n" +
                "    CASE \n" +
                "        WHEN node_distance.start_node > node_distance.end_node THEN start_node.id \n" +
                "        ELSE end_node.id \n" +
                "    END AS further_node_id \n" +
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
                "    WHERE ST_DWithin(coords.geom, link.geom, :distance) \n" +
                "    ORDER BY distance \n" +
                "    LIMIT 1 \n" +
                ") AS closest_link \n" +
                "INNER JOIN routing.infrastructure_link_vertices_pgr start_node ON start_node.id = closest_link.start_node_id \n" +
                "INNER JOIN routing.infrastructure_link_vertices_pgr end_node ON end_node.id = closest_link.end_node_id \n" +
                "CROSS JOIN LATERAL ( \n" +
                "    SELECT \n" +
                "        coords.geom <-> start_node.the_geom AS start_node, \n" +
                "        coords.geom <-> end_node.the_geom AS end_node \n" +
                ") AS node_distance \n" +
                "ORDER BY seq ASC; \n"
    }
}
