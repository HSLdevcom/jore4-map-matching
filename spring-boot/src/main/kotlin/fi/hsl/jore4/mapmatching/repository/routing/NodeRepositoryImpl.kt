package fi.hsl.jore4.mapmatching.repository.routing

import fi.hsl.jore4.mapmatching.model.InfrastructureLinkId
import fi.hsl.jore4.mapmatching.model.InfrastructureNodeId
import fi.hsl.jore4.mapmatching.model.NodeIdSequence
import fi.hsl.jore4.mapmatching.model.VehicleType
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.jdbc.core.PreparedStatementCreator
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate
import org.springframework.stereotype.Repository
import java.sql.Connection
import java.sql.PreparedStatement
import java.sql.ResultSet

@Repository
class NodeRepositoryImpl @Autowired constructor(val jdbcTemplate: NamedParameterJdbcTemplate) : INodeRepository {

    override fun resolveNodeSequence(startLinkId: InfrastructureLinkId,
                                     endLinkId: InfrastructureLinkId,
                                     nodeIdSequences: Iterable<NodeIdSequence>,
                                     vehicleType: VehicleType)
        : NodeIdSequence? {

        // There are at most four sequences of infrastructure network node identifiers that can be iterated over.
        val iter: Iterator<NodeIdSequence> = nodeIdSequences.iterator()

        if (!iter.hasNext()) {
            return null
        }

        val seq1: NodeIdSequence = iter.next()
        val seq2: NodeIdSequence? = if (iter.hasNext()) iter.next() else null
        val seq3: NodeIdSequence? = if (iter.hasNext()) iter.next() else null
        val seq4: NodeIdSequence? = if (iter.hasNext()) iter.next() else null

        if (iter.hasNext()) {
            throw IllegalArgumentException("Maximum of 4 node sequences exceeded")
        }

        val query: String = getQueryForResolvingBestNodeSequenceOf4()

        val preparedStatementCreator = PreparedStatementCreator { conn ->
            val pstmt: PreparedStatement = conn.prepareStatement(query)

            // Setting array parameters can only be done through a java.sql.Connection object.
            pstmt.setArray(1, toSqlArray(seq1, conn))
            pstmt.setArray(2, toSqlArray(seq2, conn))
            pstmt.setArray(3, toSqlArray(seq3, conn))
            pstmt.setArray(4, toSqlArray(seq4, conn))

            pstmt.setLong(5, startLinkId.value)
            pstmt.setLong(6, endLinkId.value)

            pstmt.setString(7, vehicleType.value)

            pstmt
        }

        val result: List<InfrastructureNodeId> =
            jdbcTemplate.jdbcOperations.query(preparedStatementCreator) { rs: ResultSet, _: Int ->
                InfrastructureNodeId(rs.getLong("node_id"))
            }

        return if (result.isNotEmpty()) NodeIdSequence(result) else null
    }

    companion object {
        /**
         * The generated query uses '?' placeholder for bind variables since
         * there exist SQL ARRAY parameters that cannot be set via named
         * variables in Spring JDBC templates.
         */
        private fun getQueryForResolvingBestNodeSequenceOf4(): String {
            // The produced SQL query is enclosed in quotes and passed as
            // parameter to pgr_dijkstraVia() function. '?' is used as a bind
            // variable placeholder. Actual variable binding is left to occur
            // within initialisation of PreparedStatement.
            val linkSelectionQueryForPgrDijkstra: String = QueryHelper.getVehicleTypeConstrainedLinksQuery()

            return "SELECT DISTINCT ON (start_link_id) unnest(node_arr) AS node_id \n" +
                "FROM ( \n" +
                "    SELECT _node_seq.* \n" +
                "    FROM ( \n" +
                "        SELECT 1 AS node_seq_id, ?::bigint[] AS node_arr \n" +
                "        UNION SELECT 2, ?::bigint[] \n" +
                "        UNION SELECT 3, ?::bigint[] \n" +
                "        UNION SELECT 4, ?::bigint[] \n" +
                "    ) _node_seq \n" +
                "    WHERE cardinality(_node_seq.node_arr) > 0 \n" +
                ") AS node_seq \n" +
                "CROSS JOIN ( \n" +
                "    SELECT ? AS start_link_id, ? AS end_link_id \n" +
                ") AS terminus_links \n" +
                "CROSS JOIN LATERAL ( \n" +
                "    SELECT max(pgr.route_agg_cost) AS route_agg_cost \n" +
                "    FROM pgr_dijkstraVia( \n" +
                "        $linkSelectionQueryForPgrDijkstra, \n" +
                "        node_seq.node_arr, \n" +
                "        directed := true, \n" +
                "        strict := true, \n" +
                "        U_turn_on_edge := true \n" +
                "    ) AS pgr \n" +
                "    GROUP BY node_seq_id \n" +
                "    HAVING array_agg(edge) @> ARRAY[start_link_id, end_link_id] \n" +
                ") route_overview \n" +
                "ORDER BY start_link_id, route_agg_cost; \n"
        }

        private fun toSqlArray(nodeIdSequence: NodeIdSequence?, conn: Connection): java.sql.Array {
            val nodeIdArr: Array<Long> =
                nodeIdSequence?.list?.let { nodeIds -> nodeIds.map { it.value }.toTypedArray() } ?: emptyArray()

            return conn.createArrayOf("bigint", nodeIdArr)
        }
    }
}
