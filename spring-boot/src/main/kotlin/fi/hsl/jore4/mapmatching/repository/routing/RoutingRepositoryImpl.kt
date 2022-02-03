package fi.hsl.jore4.mapmatching.repository.routing

import fi.hsl.jore4.mapmatching.model.ExternalLinkReference
import fi.hsl.jore4.mapmatching.model.InfrastructureLinkTraversal
import fi.hsl.jore4.mapmatching.model.InfrastructureNodeId
import fi.hsl.jore4.mapmatching.model.NodeIdSequence
import fi.hsl.jore4.mapmatching.model.PathTraversal
import fi.hsl.jore4.mapmatching.model.VehicleType
import fi.hsl.jore4.mapmatching.util.GeolatteUtils.extractLineStringG2D
import fi.hsl.jore4.mapmatching.util.GeolatteUtils.fromEwkb
import fi.hsl.jore4.mapmatching.util.GeolatteUtils.toEwkb
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
    override fun findRouteViaNetworkNodes(nodeIdSequence: NodeIdSequence,
                                          vehicleType: VehicleType,
                                          bufferAreaRestriction: BufferAreaRestriction?)
        : List<RouteLinkDTO> {

        val parameterSetter = PreparedStatementSetter { pstmt ->
            val conn: Connection = pstmt.connection

            pstmt.setString(1, vehicleType.value)

            // Set additional parameters if restricting infrastructure links
            // with a buffer area.
            bufferAreaRestriction?.run {
                pstmt.setLong(2, infrastructureLinkIdAtStart.value)
                pstmt.setLong(3, infrastructureLinkIdAtEnd.value)
                pstmt.setBytes(4, toEwkb(lineGeometry))
                pstmt.setDouble(5, bufferRadiusInMeters)
            }

            val nodeIdsParamIndex: Int = when (bufferAreaRestriction != null) {
                true -> 6
                false -> 2
            }

            val nodeIdArray: Array<Long> = nodeIdSequence.list.map(InfrastructureNodeId::value).toTypedArray()

            // Setting array parameters can only be done through a java.sql.Connection object.
            pstmt.setArray(nodeIdsParamIndex, conn.createArrayOf("bigint", nodeIdArray))
        }

        val restrictWithBufferArea: Boolean = bufferAreaRestriction != null
        val query: String = getQueryForFindingRouteViaNodes(restrictWithBufferArea)

        return jdbcTemplate.jdbcOperations.query(query, parameterSetter) { rs: ResultSet, _: Int ->
            val routeSeqNum = rs.getInt("seq")
            val routeLegSeqNum = rs.getInt("path_seq")

            val startNodeId = rs.getLong("start_node_id")

            val infrastructureLinkId = rs.getLong("infrastructure_link_id")
            val externalLinkId = rs.getString("external_link_id")
            val infrastructureSource = rs.getString("infrastructure_source_name")

            val forwardTraversal = rs.getBoolean("is_traversal_forwards")
            val cost = rs.getDouble("cost")

            val nameJson = JSONB.jsonb(rs.getString("name"))
            val name = jsonbConverter.fromJson(nameJson, MultilingualString::class.java)

            val linkBytes: ByteArray = rs.getBytes("geom")
            val geom: LineString<G2D> = extractLineStringG2D(fromEwkb(linkBytes))

            RouteLinkDTO(routeSeqNum,
                         routeLegSeqNum,
                         startNodeId,
                         InfrastructureLinkTraversal(
                             infrastructureLinkId,
                             ExternalLinkReference(infrastructureSource,
                                                   externalLinkId),
                             PathTraversal(geom,
                                           forwardTraversal),
                             cost,
                             name))
        }
    }

    companion object {
        /**
         * The generated query uses '?' placeholder for bind variables since
         * there exist SQL ARRAY parameters that cannot be set via named
         * variables in Spring JDBC templates.
         */
        private fun getQueryForFindingRouteViaNodes(restrictWithBufferArea: Boolean): String {
            // The produced SQL query is enclosed in quotes and passed as
            // parameter to pgr_dijkstraVia() function. '?' is used as a bind
            // variable placeholder. Actual variable binding is left to occur
            // within initialisation of PreparedStatement.
            val linkSelectionQueryForPgrDijkstra: String =
                if (restrictWithBufferArea)
                    QueryHelper.getVehicleTypeAndBufferAreaConstrainedLinksQuery()
                else
                    QueryHelper.getVehicleTypeConstrainedLinksQuery()

            return """
                SELECT
                    pt.seq,
                    pt.path_seq,
                    pt.node AS start_node_id,
                    link.infrastructure_link_id,
                    src.infrastructure_source_name,
                    link.external_link_id,
                    (pt.node = link.start_node_id) AS is_traversal_forwards,
                    pt.cost,
                    link.name,
                    ST_AsEWKB(CASE
                        WHEN pt.node = link.start_node_id THEN ST_Transform(link.geom, 4326)
                        ELSE ST_Transform(ST_Reverse(link.geom), 4326)
                    END) AS geom
                FROM (
                    SELECT seq, path_seq, node, edge, pgr.cost
                    FROM pgr_dijkstraVia(
                        $linkSelectionQueryForPgrDijkstra,
                        ?::bigint[],
                        directed := true,
                        strict := true,
                        U_turn_on_edge := true
                    ) AS pgr
                ) AS pt
                INNER JOIN routing.infrastructure_link link ON pt.edge = link.infrastructure_link_id
                INNER JOIN routing.infrastructure_source src ON src.infrastructure_source_id = link.infrastructure_source_id;
                """.trimIndent()
        }
    }
}
