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
class NodeRepositoryImpl
    @Autowired
    constructor(
        val jdbcTemplate: NamedParameterJdbcTemplate
    ) : INodeRepository {
        override fun findNClosestNodes(
            points: List<Point<G2D>>,
            vehicleType: VehicleType,
            distanceInMeters: Double
        ): Map<Int, SnapPointToNodesResult> {
            if (points.isEmpty()) {
                return emptyMap()
            }

            // List of points is transformed to binary MultiPoint format (for compact representation).
            val ewkb: ByteArray = toEwkb(mkMultiPoint(points))

            val params =
                MapSqlParameterSource()
                    .addValue("ewkb", ewkb)
                    .addValue("vehicleType", vehicleType.value)
                    .addValue("distance", distanceInMeters)

            // one-based index
            val resultItems: List<Pair<Int, NodeProximity>> =
                jdbcTemplate.query(FIND_N_CLOSEST_NODES_SQL, params) { rs: ResultSet, _: Int ->
                    val pointSeqNum = rs.getInt("point_seq")
                    val nodeId = rs.getLong("node_id")
                    val nodeDistance = rs.getDouble("node_distance")

                    Pair(
                        pointSeqNum,
                        NodeProximity(InfrastructureNodeId(nodeId), nodeDistance)
                    )
                }

            return resultItems
                .groupBy(Pair<Int, *>::first, Pair<*, NodeProximity>::second)
                .mapValues { entry ->
                    val pointSeqNum: Int = entry.key
                    val nodeList: List<NodeProximity> = entry.value

                    val pointIndex: Int = pointSeqNum - 1
                    val sourcePoint: Point<G2D> = points[pointIndex]

                    SnapPointToNodesResult(sourcePoint, distanceInMeters, nodeList)
                }
        }

        override fun resolveBestNodeSequences(
            nodeSequenceCandidates: List<NodeSequenceCandidate>,
            vehicleType: VehicleType,
            bufferAreaRestriction: BufferAreaRestriction?
        ): Map<Pair<InfrastructureLinkId, InfrastructureLinkId>, NodeIdSequence> {
            val numCandidates: Int = nodeSequenceCandidates.size

            if (numCandidates == 0) {
                return emptyMap()
            }

            val query: String = getQueryForResolvingBestNodeSequences(numCandidates, bufferAreaRestriction)

            val preparedStatementCreator =
                PreparedStatementCreator { conn ->
                    val pstmt: PreparedStatement = conn.prepareStatement(query)

                    var paramIndex = 1

                    nodeSequenceCandidates.withIndex().forEach { (index, nodeIdSeq) ->

                        pstmt.setInt(paramIndex++, index + 1)
                        pstmt.setLong(paramIndex++, nodeIdSeq.startLinkId.value)
                        pstmt.setLong(paramIndex++, nodeIdSeq.endLinkId.value)

                        // Setting array parameters can only be done through a java.sql.Connection object.
                        pstmt.setArray(paramIndex++, toSqlArray(nodeIdSeq.nodeIdSequence, conn))
                    }

                    pstmt.setString(paramIndex++, vehicleType.value)

                    // Set additional parameters if restricting infrastructure links with a buffer area.
                    bufferAreaRestriction?.run {
                        explicitLinkReferences?.run {
                            idsOfCandidatesForTerminusLinks.forEach {
                                pstmt.setLong(paramIndex++, it.value)
                            }
                            repeat(2) {
                                // node IDs need to be set twice, separately for start and end nodes
                                idsOfCandidatesForTerminusNodes.forEach {
                                    pstmt.setLong(paramIndex++, it.value)
                                }
                            }
                        }
                        pstmt.setBytes(paramIndex++, toEwkb(lineGeometry))
                        pstmt.setDouble(paramIndex++, bufferRadiusInMeters)
                    }

                    pstmt
                }

            return jdbcTemplate.jdbcOperations
                .query(preparedStatementCreator) { rs: ResultSet, _: Int ->
                    Triple(
                        rs.getLong("start_link_id"),
                        rs.getLong("end_link_id"),
                        InfrastructureNodeId(rs.getLong("node_id"))
                    )
                }.groupBy(
                    keySelector = { it.first to it.second },
                    valueTransform = { it.third }
                ).mapKeys { entry ->
                    val startLinkId = InfrastructureLinkId(entry.key.first)
                    val endLinkId = InfrastructureLinkId(entry.key.second)

                    startLinkId to endLinkId
                }.mapValues { NodeIdSequence(it.value) }
        }

        companion object {
            private val FIND_N_CLOSEST_NODES_SQL =
                """
                SELECT
                    path[1] AS point_seq,
                    close_node.id AS node_id,
                    close_node.distance AS node_distance
                FROM (
                    SELECT (g.gdump).path AS path, (g.gdump).geom AS geom
                    FROM (
                        SELECT ST_Dump(ST_Transform(ST_GeomFromEWKB(:ewkb), 3067)) AS gdump
                    ) g
                ) point, LATERAL (
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
                ) close_node
                ORDER BY point_seq ASC, distance ASC
                """.trimIndent()

            /**
             * The generated query uses '?' placeholder for bind variables since
             * there exist SQL ARRAY parameters that cannot be set via named
             * variables in Spring JDBC templates.
             */
            private fun getQueryForResolvingBestNodeSequences(
                numberOfSequenceCandidates: Int,
                bufferAreaRestriction: BufferAreaRestriction?
            ): String {
                require(numberOfSequenceCandidates in 1..100) { "numberOfSequenceCandidates must be in range 1..100" }

                // The produced SQL query is enclosed in quotes and passed as parameter to
                // pgRouting function. '?' is used as a bind variable placeholder. Actual
                // variable binding is left to occur within initialisation of PreparedStatement.
                val linkSelectionQueryForPgRouting: String =
                    bufferAreaRestriction
                        ?.run {
                            explicitLinkReferences
                                ?.run {
                                    PgRoutingEdgeQueries.getVehicleTypeAndBufferAreaConstrainedLinksQuery(
                                        idsOfCandidatesForTerminusLinks.size,
                                        idsOfCandidatesForTerminusNodes.size
                                    )
                                }
                                ?: PgRoutingEdgeQueries.getVehicleTypeAndBufferAreaConstrainedLinksQuery(0, 0)
                        }
                        ?: PgRoutingEdgeQueries.getVehicleTypeConstrainedLinksQuery()

                fun createUnionSubquery(numberOfSequenceCandidates: Int): String =
                    (1..numberOfSequenceCandidates)
                        .joinToString(
                            transform = {
                                "SELECT ? AS node_seq_id, ? AS start_link_id, ? AS end_link_id, ?::bigint[] AS node_arr"
                            },
                            // Indent in separator should match the main query string below in order to have
                            // properly formatted SQL result.
                            separator = "\n                        UNION "
                        )

                return """
                    SELECT start_link_id, end_link_id, unnest(node_arr) AS node_id
                    FROM (
                        SELECT DISTINCT ON (start_link_id, end_link_id) start_link_id, end_link_id, node_arr
                        FROM (
                            ${createUnionSubquery(numberOfSequenceCandidates)}
                        ) node_seq
                        CROSS JOIN LATERAL (
                            SELECT max(pgr.route_agg_cost) AS route_agg_cost
                            FROM pgr_dijkstraVia(
                                $linkSelectionQueryForPgRouting,
                                node_seq.node_arr,
                                directed := true,
                                strict := true,
                                U_turn_on_edge := true
                            ) pgr
                            GROUP BY node_seq_id
                        ) route_overview
                        ORDER BY start_link_id, end_link_id, route_agg_cost
                    ) res;
                    """.trimIndent()
            }

            private fun toSqlArray(
                nodeIdSequence: NodeIdSequence?,
                conn: Connection
            ): java.sql.Array {
                val nodeIdArr: Array<Long> =
                    nodeIdSequence
                        ?.list
                        ?.run { map(InfrastructureNodeId::value).toTypedArray() }
                        ?: emptyArray()

                return conn.createArrayOf("bigint", nodeIdArr)
            }
        }
    }
