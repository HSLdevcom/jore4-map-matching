package fi.hsl.jore4.mapmatching.repository.routing

import fi.hsl.jore4.mapmatching.model.ExternalLinkReference
import fi.hsl.jore4.mapmatching.model.PathTraversal
import fi.hsl.jore4.mapmatching.model.VehicleType
import fi.hsl.jore4.mapmatching.util.GeolatteUtils.extractLineStringG2D
import fi.hsl.jore4.mapmatching.util.GeolatteUtils.fromEwkb
import fi.hsl.jore4.mapmatching.util.MultilingualString
import fi.hsl.jore4.mapmatching.util.component.IJsonbConverter
import org.geolatte.geom.G2D
import org.geolatte.geom.LineString
import org.jooq.JSONB
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.jdbc.core.PreparedStatementSetter
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate
import org.springframework.stereotype.Repository
import org.springframework.transaction.annotation.Transactional
import java.sql.Connection
import java.sql.ResultSet

@Repository
class RoutingRepositoryImpl @Autowired constructor(val jdbcTemplate: NamedParameterJdbcTemplate,
                                                   val jsonbConverter: IJsonbConverter) : IRoutingRepository {

    @Transactional(readOnly = true)
    override fun findRouteViaNetworkNodes(vehicleType: VehicleType, nodeIds: List<Long>): List<RouteLinkDTO> {
        val parameterSetter = PreparedStatementSetter { pstmt ->
            val conn: Connection = pstmt.connection

            // Setting array parameters can only be done through a java.sql.Connection object.
            pstmt.setArray(1, conn.createArrayOf("bigint", nodeIds.toTypedArray()))
        }

        val query: String = getQueryForFindingRouteViaNodes(vehicleType)

        return jdbcTemplate.jdbcOperations.query(query, parameterSetter) { rs: ResultSet, _: Int ->
            val routeSeqNum = rs.getInt("seq")
            val routeLegSeqNum = rs.getInt("path_seq")

            val startNodeId = rs.getLong("start_node_id")

            val infrastructureLinkId = rs.getLong("infrastructure_link_id")
            val externalLinkId = rs.getString("external_link_id")
            val infrastructureSource = rs.getString("infrastructure_source_name")

            val alongLinkDirection = rs.getBoolean("is_traversal_forwards")
            val cost = rs.getDouble("cost")

            val nameJson = JSONB.jsonb(rs.getString("name"))
            val name = jsonbConverter.fromJson(nameJson, MultilingualString::class.java)

            val linkBytes: ByteArray = rs.getBytes("geom")
            val geom: LineString<G2D> = extractLineStringG2D(fromEwkb(linkBytes))

            RouteLinkDTO(routeSeqNum,
                         routeLegSeqNum,
                         startNodeId,
                         PathTraversal(
                             infrastructureLinkId,
                             ExternalLinkReference(infrastructureSource, externalLinkId),
                             alongLinkDirection,
                             cost,
                             name,
                             geom))
        }
    }

    companion object {
        private fun getQueryForFindingRouteViaNodes(vehicleType: VehicleType): String =
            "SELECT \n" +
                "    pt.seq, \n" +
                "    pt.path_seq, \n" +
                "    pt.node AS start_node_id, \n" +
                "    link.infrastructure_link_id, \n" +
                "    src.infrastructure_source_name, \n" +
                "    link.external_link_id, \n" +
                "    (pt.node = link.start_node_id) AS is_traversal_forwards, \n" +
                "    pt.cost, \n" +
                "    link.name, \n" +
                "    ST_AsEWKB(CASE \n" +
                "        WHEN pt.node = link.start_node_id THEN ST_Transform(link.geom, 4326) \n" +
                "        ELSE ST_Transform(ST_Reverse(link.geom), 4326) \n" +
                "    END) AS geom \n" +
                "FROM ( \n" +
                "    SELECT seq, path_seq, node, edge, pgr.cost \n" +
                "    FROM pgr_dijkstraVia( \n" +
                "        ${QueryHelper.getVehicleTypeConstrainedQueryForPgrDijkstra(vehicleType)}, \n" +
                "        ?::bigint[], \n" +
                "        directed := true, \n" +
                "        strict := true, \n" +
                "        U_turn_on_edge := true \n" +
                "    ) AS pgr \n" +
                ") AS pt \n" +
                "INNER JOIN routing.infrastructure_link link ON pt.edge = link.infrastructure_link_id \n" +
                "INNER JOIN routing.infrastructure_source src ON src.infrastructure_source_id = link.infrastructure_source_id; \n";
    }
}
