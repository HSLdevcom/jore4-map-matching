package fi.hsl.jore4.mapmatching.repository.routing

import org.springframework.beans.factory.annotation.Autowired
import org.springframework.jdbc.core.PreparedStatementCreator
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate
import org.springframework.stereotype.Repository
import java.sql.PreparedStatement
import java.sql.ResultSet

@Repository
class NodeRepositoryImpl @Autowired constructor(val jdbcTemplate: NamedParameterJdbcTemplate) : NodeRepository {

    override fun resolveSimpleNodeSequence(startLinkId: String,
                                           endLinkId: String,
                                           nodeSequences: Iterable<List<Int>>): List<Int>? {

        val iter: Iterator<List<Int>> = nodeSequences.iterator()

        if (!iter.hasNext()) {
            return null
        }

        val seq1: List<Int> = iter.next()
        val seq2: List<Int> = if (iter.hasNext()) iter.next() else emptyList()
        val seq3: List<Int> = if (iter.hasNext()) iter.next() else emptyList()
        val seq4: List<Int> = if (iter.hasNext()) iter.next() else emptyList()

        if (iter.hasNext()) {
            throw IllegalArgumentException("Maximum of 4 node sequences allowed")
        }

        val preparedStatementCreator = PreparedStatementCreator { conn ->
            val stmt: PreparedStatement = conn.prepareStatement(RESOLVE_BEST_NODE_SEQUENCE_OF_4_SQL)

            // Setting array parameters can only be done through a java.sql.Connection object.
            stmt.setArray(1, conn.createArrayOf("integer", seq1.toTypedArray()))
            stmt.setArray(2, conn.createArrayOf("integer", seq2.toTypedArray()))
            stmt.setArray(3, conn.createArrayOf("integer", seq3.toTypedArray()))
            stmt.setArray(4, conn.createArrayOf("integer", seq4.toTypedArray()))

            stmt.setString(5, startLinkId)
            stmt.setString(6, endLinkId)

            stmt
        }

        val result: List<Int> =
            jdbcTemplate.jdbcOperations.query(preparedStatementCreator) { rs: ResultSet, _: Int ->
                rs.getInt("node_id")
            }

        return result.ifEmpty { null }
    }

    companion object {
        private const val RESOLVE_BEST_NODE_SEQUENCE_OF_4_SQL =
            "SELECT DISTINCT ON (start_link_gid) unnest(node_arr) AS node_id \n" +
                "FROM ( \n" +
                "    SELECT _node_seq.* \n" +
                "    FROM ( \n" +
                "        SELECT 1 AS node_seq_id, ?::int[] AS node_arr \n" +
                "        UNION SELECT 2, ?::int[] \n" +
                "        UNION SELECT 3, ?::int[] \n" +
                "        UNION SELECT 4, ?::int[] \n" +
                "    ) _node_seq \n" +
                "    WHERE cardinality(_node_seq.node_arr) > 0 \n" +
                ") AS node_seq \n" +
                "CROSS JOIN ( \n" +
                "    SELECT gid::bigint AS start_link_gid FROM routing.dr_linkki where link_id = ? \n" +
                ") AS start_link \n" +
                "CROSS JOIN ( \n" +
                "    SELECT gid::bigint AS end_link_gid FROM routing.dr_linkki where link_id = ? \n" +
                ") AS end_link \n" +
                "CROSS JOIN LATERAL ( \n" +
                "    SELECT max(pgr.route_agg_cost) AS route_agg_cost \n" +
                "    FROM pgr_dijkstraVia( \n" +
                "        'SELECT gid AS id, source, target, cost, reverse_cost FROM routing.dr_linkki', \n" +
                "        node_seq.node_arr, \n" +
                "        directed := true, \n" +
                "        strict := true, \n" +
                "        U_turn_on_edge := true \n" +
                "    ) AS pgr \n" +
                "    GROUP BY node_seq_id \n" +
                "    HAVING array_agg(edge) @> ARRAY[start_link_gid, end_link_gid] \n" +
                ") route_overview \n" +
                "ORDER BY start_link_gid, route_agg_cost; \n"
    }
}
