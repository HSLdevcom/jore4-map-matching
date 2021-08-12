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
class LinkRepositoryImpl @Autowired constructor(val jdbcTemplate: NamedParameterJdbcTemplate) : LinkRepository {

    private data class NearestLinkRow(val coordinateSeqNum: Int,
                                      val linkId: String,
                                      val closestDistance: Double,
                                      val closerNodeId: Int,
                                      val furtherNodeId: Int)

    @Transactional(readOnly = true)
    override fun findNearestLinks(coordinates: List<LatLng>, searchRadius: Int): Map<Int, NearestLinkResultDTO> {
        if (coordinates.isEmpty()) {
            return emptyMap()
        }

        // Input coordinates are transformed to binary MultiPoint format (for compact representation).
        val ewkb: ByteArray = toEwkb(toMultiPoint(coordinates))

        val params = MapSqlParameterSource()
            .addValue("ewkb", ewkb)
            .addValue("search_radius", searchRadius)

        val dbResults: List<NearestLinkRow> =
            jdbcTemplate.query(FIND_NEAREST_LINKS_SQL, params) { rs: ResultSet, _: Int ->
                val coordinateSeqNum = rs.getInt("seq")
                val linkId = rs.getString("link_id")
                val closestDistance = rs.getDouble("closest_distance")
                val closerNodeId = rs.getInt("closer_node_id")
                val furtherNodeId = rs.getInt("further_node_id")

                NearestLinkRow(coordinateSeqNum, linkId, closestDistance, closerNodeId, furtherNodeId)
            }

        return dbResults.associateBy(keySelector = { it.coordinateSeqNum }, valueTransform = {
            val coordinateIndex = it.coordinateSeqNum - 1
            val fromCoordinate = coordinates[coordinateIndex]

            NearestLinkResultDTO(fromCoordinate, it.linkId, it.closestDistance, it.closerNodeId, it.furtherNodeId)
        })
    }

    companion object {
        private const val FIND_NEAREST_LINKS_SQL =
            "SELECT \n" +
                "    left(right(path::text, -1), -1)::int AS seq, \n" +
                "    link_id, \n" +
                "    closest_link.distance AS closest_distance, \n" +
                "    CASE \n" +
                "        WHEN node_distance.source <= node_distance.target THEN source_node.id \n" +
                "        ELSE target_node.id \n" +
                "    END AS closer_node_id, \n" +
                "    CASE \n" +
                "        WHEN node_distance.source > node_distance.target THEN source_node.id \n" +
                "        ELSE target_node.id \n" +
                "    END AS further_node_id \n" +
                "FROM ( \n" +
                "    SELECT (g.gdump).path AS path, (g.gdump).geom AS geom \n" +
                "    FROM ( \n" +
                "        SELECT ST_Dump(ST_Transform(ST_GeomFromEWKB(:ewkb), 3067)) AS gdump \n" +
                "    ) AS g \n" +
                ") AS coords, LATERAL ( \n" +
                "    SELECT \n" +
                "        link.link_id, \n" +
                "        link.source, \n" +
                "        link.target, \n" +
                "        coords.geom <-> link.geom AS distance \n" +
                "    FROM routing.dr_linkki link \n" +
                "    WHERE ST_DWithin(coords.geom, link.geom, :search_radius) \n" +
                "    ORDER BY distance \n" +
                "    LIMIT 1 \n" +
                ") AS closest_link \n" +
                "INNER JOIN routing.dr_linkki_vertices_pgr source_node ON source_node.id = closest_link.source \n" +
                "INNER JOIN routing.dr_linkki_vertices_pgr target_node ON target_node.id = closest_link.target \n" +
                "CROSS JOIN LATERAL ( \n" +
                "    SELECT \n" +
                "        coords.geom <-> source_node.the_geom AS source, \n" +
                "        coords.geom <-> target_node.the_geom AS target \n" +
                ") AS node_distance \n" +
                "ORDER BY seq ASC; \n"
    }
}
