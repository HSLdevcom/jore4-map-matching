package fi.hsl.jore4.mapmatching.repository.routing

import fi.hsl.jore4.mapmatching.util.GeolatteUtils.extractLineStringG2D
import fi.hsl.jore4.mapmatching.util.GeolatteUtils.fromEwkb
import org.geolatte.geom.G2D
import org.geolatte.geom.LineString
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.jdbc.core.PreparedStatementSetter
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate
import org.springframework.stereotype.Repository
import org.springframework.transaction.annotation.Transactional
import java.sql.Connection
import java.sql.ResultSet

@Repository
class RoutingRepositoryImpl @Autowired constructor(val jdbcTemplate: NamedParameterJdbcTemplate) : RoutingRepository {

    @Transactional(readOnly = true)
    override fun findRouteViaNetworkNodes(params: NetworkNodeParams): List<RouteSegmentDTO> {
        val nodeSequences: NetworkNodeSequences = params.getNodeSequenceVariants()

        if (LOGGER.isDebugEnabled) {
            LOGGER.debug("Trying to find route using node sequences:\n  $nodeSequences")
        }

        val parameterSetter = PreparedStatementSetter { stmt ->
            val conn: Connection = stmt.connection

            // Setting array parameters can only be done through a java.sql.Connection object.
            stmt.setArray(1, conn.createArrayOf("integer", nodeSequences.seq1.toTypedArray()))
            stmt.setArray(2, conn.createArrayOf("integer", nodeSequences.seq2.toTypedArray()))
            stmt.setArray(3, conn.createArrayOf("integer", nodeSequences.seq3.toTypedArray()))
            stmt.setArray(4, conn.createArrayOf("integer", nodeSequences.seq4.toTypedArray()))
        }

        val results: List<RouteSegmentDTO> =
            jdbcTemplate.jdbcOperations.query(FIND_ROUTE_VIA_NODES_SQL, parameterSetter) { rs: ResultSet, _: Int ->
                val routeSeqNum = rs.getInt("seq")
                val routeLegSeqNum = rs.getInt("path_seq")
                val nodeId = rs.getInt("node_id")
                val linkGid = rs.getInt("link_gid")
                val linkId = rs.getString("link_id")
                val cost = rs.getDouble("cost")
                val isTraversalForwards = rs.getBoolean("is_traversal_forwards")

                val linkBytes: ByteArray = rs.getBytes("geom")
                val geom: LineString<G2D> = extractLineStringG2D(fromEwkb(linkBytes))

                RouteSegmentDTO(routeSeqNum, routeLegSeqNum, nodeId, linkGid, linkId, cost, isTraversalForwards, geom)
            }

        return results
    }

    companion object {
        private val LOGGER: Logger = LoggerFactory.getLogger(RoutingRepositoryImpl::class.java)

        private const val FIND_ROUTE_VIA_NODES_SQL =
            "with node_sequences AS ( \n" +
                "    SELECT 1 AS input_seq, ? AS arr \n" +
                "    UNION SELECT 2, ? \n" +
                "    UNION SELECT 3, ? \n" +
                "    UNION SELECT 4, ? \n" +
                "), \n" +
                "route_variants AS ( \n" +
                "    SELECT ns.input_seq, seq, path_seq, node, edge, pgr.cost \n" +
                "    FROM node_sequences ns, pgr_dijkstraVia( \n" +
                "        'SELECT gid AS id, source, target, cost, reverse_cost FROM routing.dr_linkki', \n" +
                "        ns.arr, \n" +
                "        directed := true, \n" +
                "        strict := true, \n" +
                "        U_turn_on_edge := true \n" +
                "    ) AS pgr \n" +
                "    INNER JOIN routing.dr_linkki link ON pgr.edge = link.gid \n" +
                "), \n" +
                "route_link_counts AS ( \n" +
                "    SELECT input_seq, count(seq) AS link_count \n" +
                "    FROM route_variants \n" +
                "    GROUP BY input_seq \n" +
                "), \n" +
                "route_result AS ( \n" +
                "    SELECT \n" +
                "        rv.seq, \n" +
                "        rv.path_seq, \n" +
                "        rv.node AS node_id, \n" +
                "        link.gid AS link_gid, \n" +
                "        link.link_id, \n" +
                "        rv.cost, \n" +
                "        (rv.node = link.source) AS is_traversal_forwards, \n" +
                "        ST_AsEWKB(CASE \n" +
                "            WHEN rv.node = link.source THEN ST_Transform(link.geom, 4326) \n" +
                "            ELSE ST_Transform(ST_Reverse(link.geom), 4326) \n" +
                "        END) AS geom \n" +
                "    FROM ( \n" +
                "        SELECT input_seq \n" +
                "        FROM route_link_counts \n" +
                "        ORDER BY link_count DESC \n" +
                "        LIMIT 1 \n" +
                "    ) t \n" +
                "    INNER JOIN route_variants rv ON rv.input_seq = t.input_seq \n" +
                "    INNER JOIN routing.dr_linkki link ON rv.edge = link.gid \n" +
                "    ORDER BY seq \n" +
                ") \n" +
                "SELECT * \n" +
                "FROM route_result; \n"
    }
}
