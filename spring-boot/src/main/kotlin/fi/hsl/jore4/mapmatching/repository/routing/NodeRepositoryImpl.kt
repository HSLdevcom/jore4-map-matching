package fi.hsl.jore4.mapmatching.repository.routing

import org.springframework.beans.factory.annotation.Autowired
import org.springframework.jdbc.core.PreparedStatementCreator
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate
import org.springframework.stereotype.Repository
import java.sql.PreparedStatement
import java.sql.ResultSet

@Repository
class NodeRepositoryImpl @Autowired constructor(val jdbcTemplate: NamedParameterJdbcTemplate) : INodeRepository {

    override fun resolveNodeSequence(startLinkId: Long,
                                     endLinkId: Long,
                                     nodeSequences: Iterable<List<Long>>): List<Long>? {

        val iter: Iterator<List<Long>> = nodeSequences.iterator()

        if (!iter.hasNext()) {
            return null
        }

        val seq1: List<Long> = iter.next()
        val seq2: List<Long> = if (iter.hasNext()) iter.next() else emptyList()
        val seq3: List<Long> = if (iter.hasNext()) iter.next() else emptyList()
        val seq4: List<Long> = if (iter.hasNext()) iter.next() else emptyList()

        if (iter.hasNext()) {
            throw IllegalArgumentException("Maximum of 4 node sequences exceeded")
        }

        val preparedStatementCreator = PreparedStatementCreator { conn ->
            val stmt: PreparedStatement = conn.prepareStatement(RESOLVE_BEST_NODE_SEQUENCE_OF_4_SQL)

            // Setting array parameters can only be done through a java.sql.Connection object.
            stmt.setArray(1, conn.createArrayOf("bigint", seq1.toTypedArray()))
            stmt.setArray(2, conn.createArrayOf("bigint", seq2.toTypedArray()))
            stmt.setArray(3, conn.createArrayOf("bigint", seq3.toTypedArray()))
            stmt.setArray(4, conn.createArrayOf("bigint", seq4.toTypedArray()))

            stmt.setLong(5, startLinkId)
            stmt.setLong(6, endLinkId)

            stmt
        }

        val result: List<Long> =
            jdbcTemplate.jdbcOperations.query(preparedStatementCreator) { rs: ResultSet, _: Int ->
                rs.getLong("node_id")
            }

        return result.ifEmpty { null }
    }

    companion object {
        private const val RESOLVE_BEST_NODE_SEQUENCE_OF_4_SQL =
            "SELECT DISTINCT ON (start_link_id) unnest(node_arr) AS node_id \n" +
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
                ") AS terminal_links \n" +
                "CROSS JOIN LATERAL ( \n" +
                "    SELECT max(pgr.route_agg_cost) AS route_agg_cost \n" +
                "    FROM pgr_dijkstraVia( \n" +
                "        'SELECT infrastructure_link_id AS id, start_node_id AS source, end_node_id AS target, cost, reverse_cost FROM routing.infrastructure_link', \n" +
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
}
