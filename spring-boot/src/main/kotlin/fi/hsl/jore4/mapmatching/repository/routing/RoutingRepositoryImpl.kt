package fi.hsl.jore4.mapmatching.repository.routing

import fi.hsl.jore4.mapmatching.util.GeolatteUtils.extractLineStringG2D
import fi.hsl.jore4.mapmatching.util.GeolatteUtils.fromEwkb
import org.geolatte.geom.G2D
import org.geolatte.geom.LineString
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
    override fun findRouteViaNetworkNodes(nodeIds: List<Int>): List<RouteSegmentDTO> {
        val parameterSetter = PreparedStatementSetter { stmt ->
            val conn: Connection = stmt.connection

            // Setting array parameters can only be done through a java.sql.Connection object.
            stmt.setArray(1, conn.createArrayOf("integer", nodeIds.toTypedArray()))
        }

        return jdbcTemplate.jdbcOperations.query(FIND_ROUTE_VIA_NODES_SQL, parameterSetter) { rs: ResultSet, _: Int ->
            val routeSeqNum = rs.getInt("seq")
            val routeLegSeqNum = rs.getInt("path_seq")
            val nodeId = rs.getInt("node")
            val linkId = rs.getString("link_id")
            val cost = rs.getDouble("cost")
            val isTraversalForwards = rs.getBoolean("is_traversal_forwards")

            val linkBytes: ByteArray = rs.getBytes("geom")
            val geom: LineString<G2D> = extractLineStringG2D(fromEwkb(linkBytes))

            RouteSegmentDTO(routeSeqNum, routeLegSeqNum, nodeId, linkId, cost, isTraversalForwards, geom)
        }
    }

    companion object {
        private const val FIND_ROUTE_VIA_NODES_SQL =
            "SELECT \n" +
                "    pt.seq, pt.path_seq, pt.node, link.link_id, pt.cost, \n" +
                "    (pt.node = link.source) AS is_traversal_forwards, \n" +
                "    ST_AsEWKB(CASE \n" +
                "        WHEN pt.node = link.source THEN ST_Transform(link.geom, 4326) \n" +
                "        ELSE ST_Transform(ST_Reverse(link.geom), 4326) \n" +
                "    END) AS geom \n" +
                "FROM ( \n" +
                "    SELECT seq, path_seq, node, edge, pgr.cost \n" +
                "    FROM pgr_dijkstraVia( \n" +
                "        'SELECT gid AS id, source, target, cost, reverse_cost FROM routing.dr_linkki', \n" +
                "        ?::int[], \n" +
                "        directed := true, \n" +
                "        strict := true, \n" +
                "        U_turn_on_edge := true \n" +
                "    ) AS pgr \n" +
                ") AS pt \n" +
                "INNER JOIN routing.dr_linkki link ON pt.edge = link.gid; \n"
    }
}
