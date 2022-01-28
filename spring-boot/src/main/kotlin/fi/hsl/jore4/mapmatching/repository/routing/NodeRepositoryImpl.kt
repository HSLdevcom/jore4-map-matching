package fi.hsl.jore4.mapmatching.repository.routing

import fi.hsl.jore4.mapmatching.model.InfrastructureLinkId
import fi.hsl.jore4.mapmatching.model.InfrastructureNodeId
import fi.hsl.jore4.mapmatching.model.NodeIdSequence
import fi.hsl.jore4.mapmatching.model.NodeProximity
import fi.hsl.jore4.mapmatching.model.VehicleType
import fi.hsl.jore4.mapmatching.util.GeolatteUtils.toEwkb
import org.geolatte.geom.G2D
import org.geolatte.geom.Geometries.mkMultiPoint
import org.geolatte.geom.Point
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.jdbc.core.PreparedStatementCreator
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate
import org.springframework.stereotype.Repository
import java.sql.Connection
import java.sql.PreparedStatement
import java.sql.ResultSet

@Repository
class NodeRepositoryImpl @Autowired constructor(val jdbcTemplate: NamedParameterJdbcTemplate) : INodeRepository {

    override fun findNClosestNodes(points: List<Point<G2D>>, vehicleType: VehicleType, distanceInMeters: Double)
        : Map<Int, SnapPointToNodesDTO> {

        if (points.isEmpty()) {
            return emptyMap()
        }

        // List of points is transformed to binary MultiPoint format (for compact representation).
        val ewkb: ByteArray = toEwkb(mkMultiPoint(points))

        val params = MapSqlParameterSource()
            .addValue("ewkb", ewkb)
            .addValue("vehicleType", vehicleType.value)
            .addValue("distance", distanceInMeters)

        // one-based index
        val resultItems: List<Pair<Int, NodeProximity>> =
            jdbcTemplate.query(FIND_N_CLOSEST_NODES_SQL, params) { rs: ResultSet, _: Int ->
                val pointSeqNum = rs.getInt("point_seq")
                val nodeId = rs.getLong("node_id")
                val nodeDistance = rs.getDouble("node_distance")

                Pair(pointSeqNum,
                     NodeProximity(InfrastructureNodeId(nodeId), nodeDistance))
            }

        return resultItems
            .groupBy(keySelector = { it.first }, valueTransform = { it.second })
            .mapValues { entry ->
                val pointSeqNum: Int = entry.key
                val nodeList: List<NodeProximity> = entry.value

                val pointIndex: Int = pointSeqNum - 1
                val sourcePoint: Point<G2D> = points[pointIndex]

                SnapPointToNodesDTO(sourcePoint, distanceInMeters, nodeList)
            }
    }

    override fun resolveNodeSequence(startLinkId: InfrastructureLinkId,
                                     endLinkId: InfrastructureLinkId,
                                     nodeIdSequences: Iterable<NodeIdSequence>,
                                     vehicleType: VehicleType,
                                     bufferAreaRestriction: BufferAreaRestriction?)
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

        val restrictWithBufferArea: Boolean = bufferAreaRestriction != null
        val query: String = getQueryForResolvingBestNodeSequenceOf4(restrictWithBufferArea)

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

            // Set additional parameters if restricting infrastructure links
            // with a buffer area.
            bufferAreaRestriction?.run {
                pstmt.setLong(8, startLinkId.value)
                pstmt.setLong(9, endLinkId.value)
                pstmt.setBytes(10, toEwkb(lineGeometry))
                pstmt.setDouble(11, bufferRadiusInMeters)
            }

            pstmt
        }

        val result: List<InfrastructureNodeId> =
            jdbcTemplate.jdbcOperations.query(preparedStatementCreator) { rs: ResultSet, _: Int ->
                InfrastructureNodeId(rs.getLong("node_id"))
            }

        return if (result.isNotEmpty()) NodeIdSequence(result) else null
    }

    companion object {
        private val FIND_N_CLOSEST_NODES_SQL = """
            SELECT
                path[1] AS point_seq,
                close_node.id AS node_id,
                close_node.distance AS node_distance
            FROM (
                SELECT (g.gdump).path AS path, (g.gdump).geom AS geom
                FROM (
                    SELECT ST_Dump(ST_Transform(ST_GeomFromEWKB(:ewkb), 3067)) AS gdump
                ) AS g
            ) AS point, LATERAL (
                SELECT
                    node.id,
                    point.geom <-> node.the_geom AS distance
                FROM routing.infrastructure_link_vertices_pgr node
                WHERE ST_DWithin(point.geom, node.the_geom, :distance)
                    AND EXISTS (
                        SELECT 1
                        FROM routing.infrastructure_link link
                        INNER JOIN routing.infrastructure_link_safely_traversed_by_vehicle_type safe
                            ON safe.infrastructure_link_id = link.infrastructure_link_id
                        WHERE safe.vehicle_type = :vehicleType
                            AND (link.start_node_id = node.id OR link.end_node_id = node.id)
                    )
            ) AS close_node
            ORDER BY point_seq ASC, distance ASC
            """.trimIndent()

        /**
         * The generated query uses '?' placeholder for bind variables since
         * there exist SQL ARRAY parameters that cannot be set via named
         * variables in Spring JDBC templates.
         */
        private fun getQueryForResolvingBestNodeSequenceOf4(restrictWithBufferArea: Boolean): String {
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
                SELECT DISTINCT ON (start_link_id) unnest(node_arr) AS node_id
                FROM (
                    SELECT _node_seq.*
                    FROM (
                        SELECT 1 AS node_seq_id, ?::bigint[] AS node_arr
                        UNION SELECT 2, ?::bigint[]
                        UNION SELECT 3, ?::bigint[]
                        UNION SELECT 4, ?::bigint[]
                    ) _node_seq
                    WHERE cardinality(_node_seq.node_arr) > 0
                ) AS node_seq
                CROSS JOIN (
                    SELECT ? AS start_link_id, ? AS end_link_id
                ) AS terminus_links
                CROSS JOIN LATERAL (
                    SELECT max(pgr.route_agg_cost) AS route_agg_cost
                    FROM pgr_dijkstraVia(
                        $linkSelectionQueryForPgrDijkstra,
                        node_seq.node_arr,
                        directed := true,
                        strict := true,
                        U_turn_on_edge := true
                    ) AS pgr
                    GROUP BY node_seq_id
                    HAVING array_agg(edge) @> ARRAY[start_link_id, end_link_id]
                ) route_overview
                ORDER BY start_link_id, route_agg_cost;
                """.trimIndent()
        }

        private fun toSqlArray(nodeIdSequence: NodeIdSequence?, conn: Connection): java.sql.Array {
            val nodeIdArr: Array<Long> =
                nodeIdSequence?.list?.let { nodeIds -> nodeIds.map { it.value }.toTypedArray() } ?: emptyArray()

            return conn.createArrayOf("bigint", nodeIdArr)
        }
    }
}
