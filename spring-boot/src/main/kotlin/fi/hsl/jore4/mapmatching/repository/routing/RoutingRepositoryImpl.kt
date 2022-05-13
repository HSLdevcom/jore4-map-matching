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

            val nodeIdArray: Array<Long> = nodeIdSequence.list.map(InfrastructureNodeId::value).toTypedArray()

            // Setting array parameters can only be done through a java.sql.Connection object.
            pstmt.setArray(paramIndex++, pstmt.connection.createArrayOf("bigint", nodeIdArray))

            pstmt.setDouble(paramIndex++, fractionalStartLocationOnFirstLink)
            pstmt.setDouble(paramIndex++, fractionalEndLocationOnLastLink)
        }

        val queryString: String = getQueryForFindingRouteViaNodes(bufferAreaRestriction)

        val queryResults: Map<Boolean, List<RouteLinkDTO>> = jdbcTemplate.jdbcOperations
            .queryForStream(queryString, parameterSetter) { rs: ResultSet, _: Int ->
                val trimmed = rs.getBoolean("trimmed")

                val routeSeqNum = rs.getInt("seq")
                val routeLegSeqNum = rs.getInt("path_seq")

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
                                        routeLegSeqNum,
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
         * The generated query uses '?' placeholder for bind variables since
         * there exist SQL ARRAY parameters that cannot be set via named
         * variables in Spring JDBC templates.
         */
        private fun getQueryForFindingRouteViaNodes(bufferAreaRestriction: BufferAreaRestriction?): String {
            // The produced SQL query is enclosed in quotes and passed as
            // parameter to pgRouting function. '?' is used as a bind
            // variable placeholder. Actual variable binding is left to occur
            // within initialisation of PreparedStatement.
            val linkSelectionQueryForPgRouting: String = bufferAreaRestriction
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

            return """
                WITH route_link AS (
                    SELECT
                        pgr.seq,
                        pgr.path_seq,
                        link.infrastructure_link_id,
                        (pgr.node = link.start_node_id) AS is_traversal_forwards,
                        pgr.cost,
                        src.infrastructure_source_name,
                        link.external_link_id,
                        link.name AS link_name,
                        link.geom
                    FROM pgr_dijkstraVia(
                        $linkSelectionQueryForPgRouting,
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
                        path_seq,
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
                    rl.path_seq,
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
                    ttl.path_seq,
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
}
