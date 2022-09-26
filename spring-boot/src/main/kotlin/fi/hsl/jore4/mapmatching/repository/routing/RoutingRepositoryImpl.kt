package fi.hsl.jore4.mapmatching.repository.routing

import fi.hsl.jore4.mapmatching.model.ExternalLinkReference
import fi.hsl.jore4.mapmatching.model.GeomTraversal
import fi.hsl.jore4.mapmatching.model.InfrastructureLinkTraversal
import fi.hsl.jore4.mapmatching.model.InfrastructureNodeId
import fi.hsl.jore4.mapmatching.model.NodeIdSequence
import fi.hsl.jore4.mapmatching.model.VehicleType
import fi.hsl.jore4.mapmatching.util.GeolatteUtils.extractLineStringG2D
import fi.hsl.jore4.mapmatching.util.GeolatteUtils.fromEwkb
import fi.hsl.jore4.mapmatching.util.GeolatteUtils.toEwkb
import fi.hsl.jore4.mapmatching.util.MultilingualString
import fi.hsl.jore4.mapmatching.util.component.IJsonbConverter
import org.geolatte.geom.G2D
import org.geolatte.geom.Geometry
import org.geolatte.geom.LineString
import org.jooq.JSONB
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.jdbc.core.PreparedStatementSetter
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate
import org.springframework.stereotype.Repository
import org.springframework.transaction.annotation.Transactional
import java.sql.PreparedStatement
import java.sql.ResultSet
import java.util.stream.Collectors.groupingBy
import java.util.stream.Collectors.mapping
import java.util.stream.Collectors.toList

@Repository
class RoutingRepositoryImpl @Autowired constructor(val jdbcTemplate: NamedParameterJdbcTemplate,
                                                   val jsonbConverter: IJsonbConverter)
    : IRoutingRepository {

    @Transactional(readOnly = true)
    override fun findRouteViaNetworkNodes(nodeIdSequence: NodeIdSequence,
                                          vehicleType: VehicleType,
                                          fractionalStartLocationOnFirstLink: Double,
                                          fractionalEndLocationOnLastLink: Double,
                                          bufferAreaRestriction: BufferAreaRestriction?)
        : RouteDTO {

        if (nodeIdSequence.isEmpty()) {
            return RouteDTO.EMPTY
        }

        val parameterSetter = PreparedStatementSetter { pstmt ->

            pstmt.setString(1, vehicleType.value)

            var paramIndex = 2

            // Set additional parameters if restricting infrastructure links with a buffer area.
            bufferAreaRestriction?.run {
                paramIndex = setParametersForBufferAreaRestriction(pstmt, this, paramIndex)
            }

            val nodeIdArray: Array<Long> = nodeIdSequence.list.map(InfrastructureNodeId::value).toTypedArray()

            // Setting array parameters can only be done through a java.sql.Connection object.
            pstmt.setArray(paramIndex++, pstmt.connection.createArrayOf("bigint", nodeIdArray))

            pstmt.setDouble(paramIndex++, fractionalStartLocationOnFirstLink)
            pstmt.setDouble(paramIndex++, fractionalEndLocationOnLastLink)
        }

        val queryString: String = getQueryForFindingRouteViaNodes(bufferAreaRestriction)

        return executeQueryAndTransformToResult(queryString, parameterSetter)
    }

    @Transactional(readOnly = true)
    override fun findRouteViaPoints(points: List<RoutingPoint>,
                                    vehicleType: VehicleType,
                                    bufferAreaRestriction: BufferAreaRestriction?)
        : RouteDTO {

        if (points.isEmpty()) {
            return RouteDTO.EMPTY
        }

        val parameterSetter = PreparedStatementSetter { pstmt ->

            var paramIndex = 1

            val linkIdValues: Array<Int> = points.map { it.linkId.value.toInt() }.toTypedArray()
            val pointFractions: Array<Double> = points.map { it.fractionalLocation }.toTypedArray()

            // Setting array parameters can only be done through a java.sql.Connection object.
            pstmt.setArray(paramIndex++, pstmt.connection.createArrayOf("bigint", linkIdValues))
            pstmt.setArray(paramIndex++, pstmt.connection.createArrayOf("float", pointFractions))

            pstmt.setString(paramIndex++, vehicleType.value)

            // Set additional parameters if restricting infrastructure links with a buffer area.
            bufferAreaRestriction?.run {
                paramIndex = setParametersForBufferAreaRestriction(pstmt, this, paramIndex)
            }
        }

        val queryString: String = getQueryForFindingRouteViaPoints(bufferAreaRestriction)

        return executeQueryAndTransformToResult(queryString, parameterSetter)
    }

    // Returns the next parameter index after all parameters related to buffer area are set.
    private fun setParametersForBufferAreaRestriction(pstmt: PreparedStatement,
                                                      bufferAreaRestriction: BufferAreaRestriction,
                                                      firstParameterIndex: Int)
        : Int {

        var paramIndex = firstParameterIndex

        bufferAreaRestriction?.run {
            explicitLinkReferences?.run {
                idsOfCandidatesForTerminusLinks.forEach {
                    pstmt.setLong(paramIndex++, it.value)
                }
                repeat(2) { // node IDs need to be set twice, separately for start and end nodes
                    idsOfCandidatesForTerminusNodes.forEach {
                        pstmt.setLong(paramIndex++, it.value)
                    }
                }
            }
            pstmt.setBytes(paramIndex++, toEwkb(lineGeometry))
            pstmt.setDouble(paramIndex++, bufferRadiusInMeters)
        }

        return paramIndex
    }

    private fun executeQueryAndTransformToResult(queryString: String,
                                                 parameterSetter: PreparedStatementSetter): RouteDTO {

        val queryResults: Map<Boolean, List<RouteLinkDTO>> = jdbcTemplate.jdbcOperations
            .queryForStream(queryString, parameterSetter) { rs: ResultSet, _: Int ->
                val trimmed = rs.getBoolean("trimmed")

                val routeSeqNum = rs.getInt("seq")

                val infrastructureLinkId = rs.getLong("infrastructure_link_id")
                val forwardTraversal = rs.getBoolean("is_traversal_forwards")
                val cost = rs.getDouble("cost")

                val infrastructureSource = rs.getString("infrastructure_source_name")
                val externalLinkId = rs.getString("external_link_id")

                val linkNameJson = JSONB.jsonb(rs.getString("link_name"))
                val linkName = jsonbConverter.fromJson(linkNameJson, MultilingualString::class.java)

                val linkBytes: ByteArray = rs.getBytes("geom")

                val geom: Geometry<*> = fromEwkb(linkBytes)
                val lineString: LineString<G2D> = extractLineStringG2D(geom)

                trimmed to RouteLinkDTO(routeSeqNum,
                                        InfrastructureLinkTraversal(
                                            infrastructureLinkId,
                                            ExternalLinkReference(infrastructureSource, externalLinkId),
                                            GeomTraversal(lineString, forwardTraversal),
                                            cost,
                                            linkName))

            }
            .collect(groupingBy({ it.first },
                                mapping({ it.second }, toList())))

        val routeLinks: List<RouteLinkDTO> = queryResults
            .getOrDefault(false, emptyList())
            .sortedBy(RouteLinkDTO::routeSeqNum)

        val trimmedTerminusLinks: List<RouteLinkDTO> = queryResults
            .getOrDefault(true, emptyList())
            .sortedBy(RouteLinkDTO::routeSeqNum)

        return RouteDTO(routeLinks,
                        trimmedTerminusLinks.firstOrNull()?.takeIf { it.routeSeqNum == 1 },
                        trimmedTerminusLinks.lastOrNull()?.takeUnless { it.routeSeqNum == 1 })
    }

    companion object {

        /**
         * The produced SQL query is enclosed in quotes and passed as parameter
         * to pgRouting function. '?' is used as a bind variable placeholder.
         * Actual variable binding is left to occur within initialisation of
         * PreparedStatement.
         */
        private fun createLinkSelectionQueryForPgRouting(bufferAreaRestriction: BufferAreaRestriction?): String =
            bufferAreaRestriction
                ?.run {
                    explicitLinkReferences
                        ?.run {
                            PgRoutingEdgeQueries.getVehicleTypeAndBufferAreaConstrainedLinksQuery(
                                idsOfCandidatesForTerminusLinks.size,
                                idsOfCandidatesForTerminusNodes.size)
                        }
                        ?: PgRoutingEdgeQueries.getVehicleTypeAndBufferAreaConstrainedLinksQuery(0, 0)
                }
                ?: PgRoutingEdgeQueries.getVehicleTypeConstrainedLinksQuery()

        /**
         * The generated query uses '?' placeholder for bind variables since
         * there exist SQL ARRAY parameters that cannot be set via named
         * variables in Spring JDBC templates.
         */
        private fun getQueryForFindingRouteViaNodes(bufferAreaRestriction: BufferAreaRestriction?): String =
            """
            WITH route_link AS (
                SELECT
                    pgr.seq,
                    link.infrastructure_link_id,
                    (pgr.node = link.start_node_id) AS is_traversal_forwards,
                    pgr.cost,
                    src.infrastructure_source_name,
                    link.external_link_id,
                    link.name AS link_name,
                    link.geom
                FROM pgr_dijkstraVia(
                    ${createLinkSelectionQueryForPgRouting(bufferAreaRestriction)},
                    ?::bigint[],
                    directed := true,
                    strict := true,
                    U_turn_on_edge := true
                ) pgr
                INNER JOIN routing.infrastructure_link link ON link.infrastructure_link_id = pgr.edge
                INNER JOIN routing.infrastructure_source src ON src.infrastructure_source_id = link.infrastructure_source_id
            ),
            trimmed_terminus_link AS (
                SELECT
                    seq,
                    infrastructure_link_id,
                    is_traversal_forwards,
                    infrastructure_source_name,
                    external_link_id,
                    link_name,
                    CASE
                        WHEN max_seq = 1 THEN CASE -- only one link
                            WHEN is_traversal_forwards = true AND start_link_fractional < end_link_fractional 
                                THEN ST_LineSubstring(geom, start_link_fractional, end_link_fractional)
                            WHEN is_traversal_forwards = false AND start_link_fractional > end_link_fractional 
                                THEN ST_LineSubstring(geom, end_link_fractional, start_link_fractional)
                            ELSE NULL
                        END
                        WHEN seq = 1 THEN CASE -- start link
                            WHEN is_traversal_forwards = true AND start_link_fractional < 1.0
                                THEN ST_LineSubstring(geom, start_link_fractional, 1.0)
                            WHEN is_traversal_forwards = false AND start_link_fractional > 0.0
                                THEN ST_LineSubstring(geom, 0.0, start_link_fractional)
                            ELSE NULL
                        END
                        ELSE CASE -- end link
                            WHEN is_traversal_forwards = true AND end_link_fractional > 0.0
                                THEN ST_LineSubstring(geom, 0.0, end_link_fractional)
                            WHEN is_traversal_forwards = false AND end_link_fractional < 1.0
                                THEN ST_LineSubstring(geom, end_link_fractional, 1.0)
                            ELSE NULL
                        END
                    END AS geom
                FROM (
                    SELECT min(seq) AS min_seq, max(seq) AS max_seq FROM route_link
                ) min_max_seq
                INNER JOIN route_link ON seq IN (min_seq, max_seq)
                CROSS JOIN (
                    SELECT ? AS start_link_fractional, ? AS end_link_fractional
                ) substring_param
            )
            SELECT false AS trimmed,
                rl.seq,
                rl.infrastructure_link_id,
                rl.is_traversal_forwards,
                rl.cost,
                rl.infrastructure_source_name,
                rl.external_link_id,
                rl.link_name,
                ST_AsEWKB(ST_Transform(rl.geom, 4326)) as geom
            FROM route_link rl
            UNION ALL
            SELECT true AS trimmed,
                ttl.seq,
                ttl.infrastructure_link_id,
                ttl.is_traversal_forwards,
                ST_Length(ttl.geom) AS cost,
                ttl.infrastructure_source_name,
                ttl.external_link_id,
                ttl.link_name,
                ST_AsEWKB(ST_Transform(ttl.geom, 4326)) as geom
            FROM trimmed_terminus_link ttl
            WHERE ttl.geom IS NOT NULL
            ORDER BY seq, trimmed;
            """.trimIndent()

        /**
         * Returns an SQL query that finds the shortest path through infrastructure network via
         * points along infrastructure links given as parameters. Each point is represented as a
         * fractional location [0,1] on a link that is referenced by its ID. There is an embedded
         * query enclosed in quotes that restricts the infrastructure links that are considered
         * viable when finding the shortest path. The embedded query has its own set of parameters
         * that are handled in [PgRoutingEdgeQueries] object.
         *
         * The query utilises pgRouting's pgr_trspViaEdges function. Currently, turn restrictions
         * that the function itself supports (via a query given as parameter) is not applied/
         * supported.
         *
         * TODO NOTE! This version of query that uses pgr_trspViaEdges does not correctly handle
         *  bi-directional closed loop links that are traversed backwards (against the digitised
         *  direction). This will be fixed by switching to use pgr_withPointsVia function which
         *  will be available in the upcoming 3.4.0 release of pgRouting.
         *
         * The query contains sub-queries that process the output of pgRouting in several stages.
         * The processing involves recognising the direction of traversal on each link which
         * information pgRouting itself does not provide. Because the points given as parameters
         * appear as extra infrastructure nodes in the pgRouting output, the infrastructure links
         * along which the points are located appear more times in the output than what is desired
         * in the final output. The processing removes consecutive duplicate appearances of links in
         * one direction of traversal. In addition, closed-loop links require special treatment
         * which is done in the processing.
         *
         * For each link appearing in the shortest path, the geometry of whole link is returned
         * independent of the given point parameters. However, for terminus links also trimmed
         * versions of geometries are included that are affected by the first and the last point
         * parameter.
         *
         * Since the query is quite long and complex a commentary in form of a sample data
         * transformation is provided below for sub-queries transforming the output of pgRouting.
         * By glancing over sample data transformations one can get a better idea what is going on
         * while executing the query.
         *
         * Sample output after "pgr" sub-query. "edge" means the ID of an infrastructure link and
         * "node" denotes either:
         * - a given point along an infrastructure link, if negative
         * - an infrastructure node, if positive
         *
         *  seq |  edge  |  node  |        cost
         * -----+--------+--------+--------------------
         *    1 | 238709 |     -1 | 27.689157697826758
         *    2 |     -1 |     -2 |                  0
         *    3 | 238709 |     -2 | 27.689157697826765
         *    4 |     -1 |     -3 |                  0
         *    5 | 238709 |     -3 |  83.06747309348029
         *    6 | 238714 | 115666 | 13.741292778420942
         *    7 | 238712 | 115667 | 21.790898535769582
         *    8 | 238707 | 115663 |  21.20953754741562
         *    9 |     -1 |     -4 |                  0
         *   10 | 238707 |     -4 |  21.20953754741562
         *   11 |     -2 |     -5 |                  0
         *
         * Sample output after "pgr_transform1" sub-query. Start and end points are added. Negative
         * points refer to given point parameters and positive points are the actual infrastructure
         * nodes.
         *
         *  seq |  edge  |        cost        | start_point | end_point
         * -----+--------+--------------------+-------------+-----------
         *    1 | 238709 | 27.689157697826758 |          -1 |        -2
         *    3 | 238709 | 27.689157697826765 |          -2 |        -3
         *    5 | 238709 |  83.06747309348029 |          -3 |    115666
         *    6 | 238714 | 13.741292778420942 |      115666 |    115667
         *    7 | 238712 | 21.790898535769582 |      115667 |    115663
         *    8 | 238707 |  21.20953754741562 |      115663 |        -4
         *   10 | 238707 |  21.20953754741562 |          -4 |        -5
         *
         * Sample output after "pgr_transform2" sub-query. Start and end fractions are derived for
         * each link traversal.
         *
         *  seq |  edge  | start_fraction | end_fraction
         * -----+--------+----------------+--------------
         *    1 | 238709 |        0.75000 |      0.50000
         *    3 | 238709 |        0.50000 |      0.25000
         *    5 | 238709 |        0.25000 |          1.0
         *    6 | 238714 |            0.0 |          1.0
         *    7 | 238712 |            0.0 |          1.0
         *    8 | 238707 |            0.0 |      0.33000
         *   10 | 238707 |        0.33000 |      0.66000
         *
         * Sample output after "pgr_transform3" sub-query. Direction of traversal is determined
         * based on start and end fractions.
         *
         *  seq |  edge  | is_traversal_forwards | start_fraction | end_fraction
         * -----+--------+-----------------------+----------------+--------------
         *    1 | 238709 | f                     |        0.75000 |      0.50000
         *    3 | 238709 | f                     |        0.50000 |      0.25000
         *    5 | 238709 | t                     |        0.25000 |          1.0
         *    6 | 238714 | t                     |            0.0 |          1.0
         *    7 | 238712 | t                     |            0.0 |          1.0
         *    8 | 238707 | t                     |            0.0 |      0.33000
         *   10 | 238707 | t                     |        0.33000 |      0.66000
         *
         * Sample output after "pgr_transform4" sub-query. Consecutive duplicates of links in one
         * direction of traversal are removed.
         *
         *  seq |  edge  | is_traversal_forwards | start_fraction | end_fraction
         * -----+--------+-----------------------+----------------+--------------
         *    1 | 238709 | f                     |        0.75000 |      0.50000
         *    5 | 238709 | t                     |        0.25000 |          1.0
         *    6 | 238714 | t                     |            0.0 |          1.0
         *    7 | 238712 | t                     |            0.0 |          1.0
         *    8 | 238707 | t                     |            0.0 |      0.33000
         *
         * Sample output after "pgr_transform5" sub-query. End fractions are corrected after removal
         * of duplicate links in the previous stage. In addition, sequence numbers are reallocated.
         *
         *  seq |  edge  | is_traversal_forwards | start_fraction | end_fraction
         * -----+--------+-----------------------+----------------+--------------
         *    1 | 238709 | f                     |        0.75000 |      0.25000
         *    2 | 238709 | t                     |        0.25000 |          1.0
         *    3 | 238714 | t                     |            0.0 |          1.0
         *    4 | 238712 | t                     |            0.0 |          1.0
         *    5 | 238707 | t                     |            0.0 |      0.66000
         */
        private fun getQueryForFindingRouteViaPoints(bufferAreaRestriction: BufferAreaRestriction?): String =
            """
            WITH point_params AS (
                SELECT
                    ?::int[] AS edge_ids,
                    ?::numeric[] AS fractions
            ),
            custom_point AS (
                SELECT
                    edge_index AS pid,
                    edge_id,
                    fraction
                FROM (
                    SELECT *, row_number() OVER () AS edge_index
                    FROM (
                        SELECT unnest(edge_ids) AS edge_id FROM point_params
                    ) u
                ) edges
                INNER JOIN (
                    SELECT *, row_number() OVER () AS fraction_index
                    FROM (
                        SELECT unnest(fractions) AS fraction FROM point_params
                    ) u
                ) fractions ON fraction_index = edge_index
            ),
            pgr AS (
                SELECT seq, id3 AS edge, id2 AS node, cost
                FROM point_params
                CROSS JOIN pgr_trspViaEdges(
                    ${createLinkSelectionQueryForPgRouting(bufferAreaRestriction)},
                    edge_ids,
                    fractions,
                    true,
                    true
                ) pgr
            ),
            pgr_transform1 AS (
                SELECT seq, edge, cost, node AS start_point, end_point
                FROM (
                    SELECT *, lead(node) OVER (ORDER BY seq) AS end_point FROM pgr
                ) sub
                WHERE edge >= 0
            ),
            pgr_transform2 AS (
                SELECT seq, edge,
                    CASE
                        WHEN pgr.start_point < 0 THEN p1.fraction
                        ELSE CASE
                            WHEN l.start_node_id = l.end_node_id THEN ( -- closed loop
                                CASE
                                    WHEN l.traffic_flow_direction_type = 3 THEN 1.0
                                    WHEN l.traffic_flow_direction_type = 4 THEN 0.0
                                    ELSE ( -- bi-directional closed loop, determine direction by matching to pgr cost
                                        SELECT CASE WHEN forward_cost_diff < backward_cost_diff THEN 0.0 ELSE 1.0 END
                                        FROM (
                                            SELECT
                                                abs(pgr.cost - l.cost * p2.fraction) AS forward_cost_diff,
                                                abs(pgr.cost - l.cost * (1.0 - p2.fraction)) AS backward_cost_diff
                                        ) diffs
                                    )
                                END
                            )
                            WHEN pgr.start_point = l.start_node_id THEN 0.0
                            ELSE 1.0
                        END
                    END AS start_fraction,
                    CASE
                        WHEN pgr.end_point < 0 THEN p2.fraction
                        ELSE CASE
                            WHEN l.start_node_id = l.end_node_id THEN ( -- closed loop
                                CASE
                                    WHEN l.traffic_flow_direction_type = 3 THEN 0.0
                                    WHEN l.traffic_flow_direction_type = 4 THEN 1.0
                                    ELSE ( -- bi-directional closed loop, determine direction by matching to pgr cost
                                        SELECT CASE WHEN forward_cost_diff < backward_cost_diff THEN 1.0 ELSE 0.0 END
                                        FROM (
                                            SELECT
                                                abs(pgr.cost - l.cost * (1.0 - p1.fraction)) AS forward_cost_diff,
                                                abs(pgr.cost - l.cost * p1.fraction) AS backward_cost_diff
                                        ) diffs
                                    )
                                END
                            )
                            WHEN pgr.end_point = l.start_node_id THEN 0.0
                            ELSE 1.0
                        END
                    END AS end_fraction
                FROM pgr_transform1 pgr
                INNER JOIN routing.infrastructure_link l ON l.infrastructure_link_id = pgr.edge
                LEFT JOIN custom_point p1 ON (pgr.start_point < 0 AND p1.pid = -pgr.start_point)
                LEFT JOIN custom_point p2 ON (pgr.end_point < 0 AND p2.pid = -pgr.end_point)
            ),
            terminus_fractions AS (
                SELECT first.start_fraction AS first_start_fraction, last.end_fraction AS last_end_fraction
                FROM (
                    SELECT start_fraction FROM pgr_transform2 WHERE seq = 1
                ) first
                CROSS JOIN (
                    SELECT end_fraction FROM pgr_transform2 WHERE seq = (SELECT max(seq) FROM pgr_transform2)
                ) last
            ),
            pgr_transform3 AS (
                SELECT seq, edge, (end_fraction > start_fraction) AS is_traversal_forwards, start_fraction, end_fraction
                FROM pgr_transform2
                WHERE start_fraction <> end_fraction -- sanity check
            ),
            pgr_transform4 AS (
                SELECT seq, edge, is_traversal_forwards, start_fraction, end_fraction
                FROM (
                    SELECT *,
                        lag(edge) OVER (ORDER BY seq) AS prev_edge,
                        lag(is_traversal_forwards) OVER (ORDER BY seq) AS prev_traversal_forwards
                    FROM pgr_transform3
                ) sub
                WHERE
                    seq = 1
                    OR edge <> prev_edge
                    OR is_traversal_forwards <> prev_traversal_forwards
                    -- closed loop cases (including multi traversals) ->
                    OR start_fraction IN (0.0, 1.0)
            ),
            pgr_transform5 AS (
                SELECT
                    row_number() OVER (ORDER BY pgr.seq) AS seq,
                    edge,
                    is_traversal_forwards,
                    start_fraction,
                    CASE
                        WHEN next_edge = edge THEN (
                            CASE
                                WHEN end_fraction = 1.0 AND next_start_fraction = 0.0 OR end_fraction = 0.0 AND next_start_fraction = 1.0 THEN end_fraction
                                ELSE next_start_fraction
                            END
                        )
                        WHEN next_edge <> edge THEN ( CASE WHEN is_traversal_forwards THEN 1.0 ELSE 0.0 END )
                        ELSE ( -- next_edge IS NULL
                            SELECT last_end_fraction FROM terminus_fractions
                        )
                    END AS end_fraction
                FROM pgr_transform4 pgr
                INNER JOIN (
                    SELECT
                        seq,
                        lead(edge) OVER (ORDER BY seq) AS next_edge,
                        lead(start_fraction) OVER (ORDER BY seq) AS next_start_fraction
                    FROM pgr_transform4
                ) sub ON sub.seq = pgr.seq
            ),
            route_link AS (
                SELECT
                    pgr.seq,
                    link.infrastructure_link_id,
                    pgr.is_traversal_forwards,
                    CASE
                        WHEN pgr.is_traversal_forwards THEN link.cost
                        ELSE link.reverse_cost
                    END AS cost,
                    src.infrastructure_source_name,
                    link.external_link_id,
                    link.name AS link_name,
                    link.geom
                FROM pgr_transform5 pgr
                INNER JOIN routing.infrastructure_link link ON link.infrastructure_link_id = pgr.edge
                INNER JOIN routing.infrastructure_source src ON src.infrastructure_source_id = link.infrastructure_source_id
            ),
            trimmed_terminus_link AS (
                SELECT
                    seq,
                    infrastructure_link_id,
                    is_traversal_forwards,
                    infrastructure_source_name,
                    external_link_id,
                    link_name,
                    CASE
                        WHEN max_seq = 1 THEN CASE -- single link route
                            WHEN is_traversal_forwards AND first_start_fraction < last_end_fraction 
                                THEN ST_LineSubstring(geom, first_start_fraction, last_end_fraction)
                            WHEN NOT is_traversal_forwards AND first_start_fraction > last_end_fraction 
                                THEN ST_LineSubstring(geom, last_end_fraction, first_start_fraction)
                            ELSE NULL
                        END
                        WHEN seq = 1 THEN CASE -- start link
                            WHEN is_traversal_forwards AND first_start_fraction < 1.0
                                THEN ST_LineSubstring(geom, first_start_fraction, 1.0)
                            WHEN NOT is_traversal_forwards AND first_start_fraction > 0.0
                                THEN ST_LineSubstring(geom, 0.0, first_start_fraction)
                            ELSE NULL
                        END
                        ELSE CASE -- end link
                            WHEN is_traversal_forwards AND last_end_fraction > 0.0
                                THEN ST_LineSubstring(geom, 0.0, last_end_fraction)
                            WHEN NOT is_traversal_forwards AND last_end_fraction < 1.0
                                THEN ST_LineSubstring(geom, last_end_fraction, 1.0)
                            ELSE NULL
                        END
                    END AS geom
                FROM terminus_fractions
                CROSS JOIN (
                    SELECT min(seq) AS min_seq, max(seq) AS max_seq FROM route_link
                ) min_max_seq
                INNER JOIN route_link ON seq IN (min_seq, max_seq)
            )
            SELECT false AS trimmed,
                rl.seq,
                rl.infrastructure_link_id,
                rl.is_traversal_forwards,
                rl.cost,
                rl.infrastructure_source_name,
                rl.external_link_id,
                rl.link_name,
                ST_AsEWKB(ST_Transform(rl.geom, 4326)) as geom
            FROM route_link rl
            UNION ALL
            SELECT true AS trimmed,
                ttl.seq,
                ttl.infrastructure_link_id,
                ttl.is_traversal_forwards,
                ST_Length(ttl.geom) AS cost,
                ttl.infrastructure_source_name,
                ttl.external_link_id,
                ttl.link_name,
                ST_AsEWKB(ST_Transform(ttl.geom, 4326)) as geom
            FROM trimmed_terminus_link ttl
            WHERE ttl.geom IS NOT NULL
            ORDER BY seq, trimmed;
            """.trimIndent()
    }
}
