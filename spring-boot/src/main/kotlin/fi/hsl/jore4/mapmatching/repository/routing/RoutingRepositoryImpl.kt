package fi.hsl.jore4.mapmatching.repository.routing

import fi.hsl.jore4.mapmatching.model.ExternalLinkReference
import fi.hsl.jore4.mapmatching.model.InfrastructureLinkId
import fi.hsl.jore4.mapmatching.model.InfrastructureLinkTraversal
import fi.hsl.jore4.mapmatching.model.InfrastructureNodeId
import fi.hsl.jore4.mapmatching.model.LinkSide
import fi.hsl.jore4.mapmatching.model.NodeIdSequence
import fi.hsl.jore4.mapmatching.model.VehicleType
import fi.hsl.jore4.mapmatching.util.CollectionUtils.filterOutConsecutiveDuplicates
import fi.hsl.jore4.mapmatching.util.GeolatteUtils.extractLineStringG2D
import fi.hsl.jore4.mapmatching.util.GeolatteUtils.fromEwkb
import fi.hsl.jore4.mapmatching.util.GeolatteUtils.toEwkb
import fi.hsl.jore4.mapmatching.util.MathUtils.isWithinTolerance
import fi.hsl.jore4.mapmatching.util.MultilingualString
import fi.hsl.jore4.mapmatching.util.component.IJsonbConverter
import org.geolatte.geom.G2D
import org.geolatte.geom.Geometries.mkLineString
import org.geolatte.geom.Geometry
import org.geolatte.geom.LineString
import org.geolatte.geom.crs.CoordinateReferenceSystems.WGS84
import org.jooq.JSONB
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.jdbc.core.PreparedStatementSetter
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate
import org.springframework.stereotype.Repository
import org.springframework.transaction.annotation.Transactional
import java.math.BigDecimal
import java.sql.PreparedStatement
import java.sql.ResultSet
import kotlin.math.pow
import kotlin.math.roundToInt

@Repository
class RoutingRepositoryImpl @Autowired constructor(val jdbcTemplate: NamedParameterJdbcTemplate,
                                                   val jsonbConverter: IJsonbConverter)
    : IRoutingRepository {

    sealed interface ResultItem

    /**
     * Models full traversal of infrastructure link (from end-to-end) as a route link.
     */
    private data class RouteLinkResultItem(val routeSeqNum: Int,
                                           val linkId: InfrastructureLinkId,
                                           val linkGeometry: LineString<G2D>,
                                           val linkLength: Double,
                                           val isTraversalForwards: Boolean,
                                           val externalLinkRef: ExternalLinkReference,
                                           val linkName: MultilingualString
    ) : ResultItem

    /**
     * Models partial traversal of infrastructure link (not end-to-end) as a route link.
     */
    private data class TrimmedRouteLinkResultItem(val routeSeqNum: Int,
                                                  val linkId: InfrastructureLinkId,
                                                  val traversedGeometry: LineString<G2D>,
                                                  val traversedDistance: Double
    ) : ResultItem

    @Transactional(readOnly = true)
    override fun findRouteViaNetworkNodes(nodeIdSequence: NodeIdSequence,
                                          vehicleType: VehicleType,
                                          bufferAreaRestriction: BufferAreaRestriction?)
        : RouteDTO {

        return findRouteViaNetworkNodesInternal(nodeIdSequence, vehicleType, null, null, bufferAreaRestriction)
    }

    @Transactional(readOnly = true)
    override fun findRouteViaNetworkNodes(nodeIdSequence: NodeIdSequence,
                                          vehicleType: VehicleType,
                                          fractionalStartLocationOnFirstLink: Double,
                                          fractionalEndLocationOnLastLink: Double,
                                          bufferAreaRestriction: BufferAreaRestriction?)
        : RouteDTO {

        return findRouteViaNetworkNodesInternal(nodeIdSequence,
                                                vehicleType,
                                                fractionalStartLocationOnFirstLink,
                                                fractionalEndLocationOnLastLink,
                                                bufferAreaRestriction)
    }

    fun findRouteViaNetworkNodesInternal(nodeIdSequence: NodeIdSequence,
                                         vehicleType: VehicleType,
                                         fractionalStartLocationOnFirstLink: Double?,
                                         fractionalEndLocationOnLastLink: Double?,
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

            fractionalStartLocationOnFirstLink
                ?.let { pstmt.setDouble(paramIndex++, it) }
                ?: run { pstmt.setNull(paramIndex++, java.sql.Types.NUMERIC) }

            fractionalEndLocationOnLastLink
                ?.let { pstmt.setDouble(paramIndex++, it) }
                ?: run { pstmt.setNull(paramIndex++, java.sql.Types.NUMERIC) }
        }

        val queryString: String = getQueryForFindingRouteViaNodes(bufferAreaRestriction)

        return executeQueryAndTransformToResult(queryString, parameterSetter)
    }

    @Transactional(readOnly = true)
    override fun findRouteViaPoints(points: List<PgRoutingPoint>,
                                    vehicleType: VehicleType,
                                    bufferAreaRestriction: BufferAreaRestriction?)
        : RouteDTO {

        if (points.isEmpty()) {
            return RouteDTO.EMPTY
        }

        // These three lists must have equal amount of items. Each list contains certain property
        // for all custom points (to be visited on route). The properties are populated as array
        // parameters into SQL query.
        val linkIdsForCustomPoints: MutableList<Long> = ArrayList()
        val fractionalLocations: MutableList<Double> = ArrayList()
        val linkSides: MutableList<Char> = ArrayList() // left, right, both

        // contains both infrastructure nodes and custom points
        val visitedPointIds: MutableList<Long> = ArrayList()

        // co-efficients for rounding fractional locations along infrastructure link
        val roundedFractionDecimalPrecision = 3
        val roundingTolerance: Double = BigDecimal.ONE.movePointLeft(roundedFractionDecimalPrecision).toDouble()
        val roundingMultiplier: Double = 10.0.pow(roundedFractionDecimalPrecision.toDouble())

        // Is seems it is needed to avoid values [-2, -1] while assigning point IDs because that
        // range seems to have special semantics in pgRouting output.
        var nextCustomPointId = 3

        // state for previously added custom point, used for comparing while adding new custom points
        var prevCustomPointLinkId: Long? = null
        var prevCustomPointRoundedFraction: Double? = null

        fun addNodeAsRoutePoint(nodeId: InfrastructureNodeId) {
            visitedPointIds.add(nodeId.value)

            // Nullify these references since the previously added point is a node, not a location
            // in-between the endpoint nodes of a link.
            prevCustomPointLinkId = null
            prevCustomPointRoundedFraction = null
        }

        points.forEach { point ->
            when (point) {
                is NetworkNode -> addNodeAsRoutePoint(point.nodeId)

                is FractionalLocationAlongLink -> {

                    val fraction: Double = point.fractionalLocation

                    if (fraction.isWithinTolerance(0.0, roundingTolerance)
                        || fraction.isWithinTolerance(1.0, roundingTolerance)
                    ) {
                        // Snap to the node at link's endpoint if rounded value of fractional
                        // location is zero or one.
                        addNodeAsRoutePoint(point.closerNodeId)
                    } else {

                        val linkId: Long = point.linkId.value
                        val roundedFraction: Double = (fraction * roundingMultiplier).roundToInt() / roundingMultiplier

                        fun addCustomPoint() {
                            val side: Char = when (point.side) {
                                LinkSide.LEFT -> 'l'
                                LinkSide.RIGHT -> 'r'
                                LinkSide.BOTH -> 'b'
                            }

                            linkIdsForCustomPoints.add(linkId)
                            fractionalLocations.add(fraction)
                            linkSides.add(side)

                            // In pgRouting, custom point (relative location along infrastructure
                            // link) is referenced by negative ID value.
                            val customPointId: Int = nextCustomPointId++

                            visitedPointIds.add((-customPointId).toLong())

                            prevCustomPointLinkId = linkId
                            prevCustomPointRoundedFraction = roundedFraction
                        }

                        if (prevCustomPointLinkId == null || prevCustomPointRoundedFraction == null) {
                            addCustomPoint()
                        } else { // prevCustomPointLinkId != null && prevCustomPointRoundedFraction != null

                            // When point distribution is too dense multiple points may round to same
                            // location. Strip out points that round to same location as their
                            // predecessor. This is done in order to have SQL query not fail due to
                            // consecutive duplicate fractional locations on one link.

                            val isPointAConsecutiveDuplicateAfterRounding: Boolean = linkId == prevCustomPointLinkId &&
                                roundedFraction.isWithinTolerance(prevCustomPointRoundedFraction!!, roundingTolerance)

                            if (!isPointAConsecutiveDuplicateAfterRounding) {
                                addCustomPoint()
                            }
                        }
                    }
                }
            }
        }

        // There could be consecutive duplicates of infrastructure node IDs due to snapping to link
        // endpoints.
        val filteredPointsIds: List<Long> = filterOutConsecutiveDuplicates(visitedPointIds)

        val parameterSetter = PreparedStatementSetter { pstmt ->

            var paramIndex = 1

            // Setting array parameters can only be done through a java.sql.Connection object.

            pstmt.setArray(paramIndex++, pstmt.connection.createArrayOf("bigint", linkIdsForCustomPoints.toTypedArray()))
            pstmt.setArray(paramIndex++, pstmt.connection.createArrayOf("numeric", fractionalLocations.toTypedArray()))
            pstmt.setArray(paramIndex++, pstmt.connection.createArrayOf("char", linkSides.toTypedArray()))

            pstmt.setArray(paramIndex++, pstmt.connection.createArrayOf("bigint", filteredPointsIds.toTypedArray()))

            pstmt.setString(paramIndex++, vehicleType.value)

            // Set additional parameters if restricting infrastructure links with a buffer area.
            bufferAreaRestriction?.run {
                setParametersForBufferAreaRestriction(pstmt, this, paramIndex)
            }
        }

        val queryString: String = getQueryForFindingRouteViaPoints(roundedFractionDecimalPrecision,
                                                                   bufferAreaRestriction)

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

        // This flat list contains result items for both (1) fully traversed infrastructure links
        // (with whole link geometries) and (2) partially traversed links (with trimmed geometries).
        // They are separated by the "trimmed" column of the result set. Each partially traversed
        // link has a fully-traversed counterpart in the result set (with same sequence number) but
        // not the other way around.
        val queryResults: List<ResultItem> = jdbcTemplate.jdbcOperations
            .query(queryString, parameterSetter) { rs: ResultSet, _: Int ->
                val trimmed = rs.getBoolean("trimmed")

                val routeSeqNum = rs.getInt("seq")

                val infrastructureLinkId = InfrastructureLinkId(rs.getLong("infrastructure_link_id"))
                val cost = rs.getDouble("cost")

                val linkBytes: ByteArray = rs.getBytes("geom")

                val geom: Geometry<*> = fromEwkb(linkBytes)
                val lineString: LineString<G2D> = extractLineStringG2D(geom)

                when (trimmed) {
                    true -> TrimmedRouteLinkResultItem(routeSeqNum,
                                                       infrastructureLinkId,
                                                       lineString,
                                                       cost)

                    false -> {
                        val isTraversalForwards = rs.getBoolean("is_traversal_forwards")

                        val infrastructureSource = rs.getString("infrastructure_source_name")
                        val externalLinkId = rs.getString("external_link_id")

                        val linkNameJson = JSONB.jsonb(rs.getString("link_name"))
                        val linkName = jsonbConverter.fromJson(linkNameJson, MultilingualString::class.java)

                        RouteLinkResultItem(routeSeqNum,
                                            infrastructureLinkId,
                                            lineString,
                                            cost,
                                            isTraversalForwards,
                                            ExternalLinkReference(infrastructureSource, externalLinkId),
                                            linkName
                        )
                    }
                }
            }

        // Collect partially traversed links from the result set and index them by the sequence
        // number of route link.
        val routeSeqNumToTrimmedRouteLink: Map<Int, TrimmedRouteLinkResultItem> = queryResults
            .mapNotNull { if (it is TrimmedRouteLinkResultItem) it else null }
            .associateBy { it.routeSeqNum }

        val routeLinks: List<RouteLinkDTO> = queryResults
            .mapNotNull { path ->
                when (path) {
                    // Filtered out since trimmed versions (partial link traversals) were already
                    // collected to a Map.
                    is TrimmedRouteLinkResultItem -> null

                    is RouteLinkResultItem -> {
                        val seqNum: Int = path.routeSeqNum
                        val trimmedRouteLink: TrimmedRouteLinkResultItem? = routeSeqNumToTrimmedRouteLink[seqNum]

                        // If a trimmed version exists, then use its geometry as the traversed
                        // geometry. Otherwise, use whole link geometry as the traversed geometry
                        // (already reversed in SQL in case of backwards traversal).
                        val traversedGeometry: LineString<G2D> = trimmedRouteLink
                            ?.let { it.traversedGeometry }
                            ?: when (path.isTraversalForwards) {
                                true -> path.linkGeometry
                                false -> mkLineString(path.linkGeometry.positions.reverse(), WGS84)
                            }

                        // If a trimmed version exists, then use the length of its geometry as the
                        // traversed distance. Otherwise, use the length of whole link geometry as
                        // the traversed distance.
                        val traversedDistance: Double = trimmedRouteLink
                            ?.let { it.traversedDistance }
                            ?: path.linkLength

                        RouteLinkDTO(seqNum,
                                     InfrastructureLinkTraversal(path.linkId,
                                                                 path.externalLinkRef,
                                                                 path.linkGeometry,
                                                                 traversedGeometry,
                                                                 path.isTraversalForwards,
                                                                 path.linkLength,
                                                                 traversedDistance,
                                                                 path.linkName))
                    }
                }
            }
            .sortedBy(RouteLinkDTO::routeSeqNum)

        return RouteDTO(routeLinks)
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
                    CASE
                        WHEN max_seq = 1 THEN CASE -- only one link
                            WHEN is_traversal_forwards = true AND start_link_fractional < end_link_fractional 
                                THEN ST_LineSubstring(geom, start_link_fractional, end_link_fractional)
                            WHEN is_traversal_forwards = false AND start_link_fractional > end_link_fractional 
                                THEN ST_Reverse(ST_LineSubstring(geom, end_link_fractional, start_link_fractional))
                            ELSE NULL
                        END
                        WHEN seq = 1 THEN CASE -- start link
                            WHEN is_traversal_forwards = true AND start_link_fractional < 1.0
                                THEN ST_LineSubstring(geom, start_link_fractional, 1.0)
                            WHEN is_traversal_forwards = false AND start_link_fractional > 0.0
                                THEN ST_Reverse(ST_LineSubstring(geom, 0.0, start_link_fractional))
                            ELSE NULL
                        END
                        ELSE CASE -- end link
                            WHEN is_traversal_forwards = true AND end_link_fractional > 0.0
                                THEN ST_LineSubstring(geom, 0.0, end_link_fractional)
                            WHEN is_traversal_forwards = false AND end_link_fractional < 1.0
                                THEN ST_Reverse(ST_LineSubstring(geom, end_link_fractional, 1.0))
                            ELSE NULL
                        END
                    END AS geom
                FROM (
                    SELECT min(seq) AS min_seq, max(seq) AS max_seq FROM route_link
                ) min_max_seq
                INNER JOIN route_link ON seq IN (min_seq, max_seq)
                CROSS JOIN (
                    SELECT ?::numeric AS start_link_fractional, ?::numeric AS end_link_fractional
                ) substring_param
                WHERE start_link_fractional IS NOT NULL AND end_link_fractional IS NOT NULL
            )
            SELECT *
            FROM (
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
                    NULL::bool AS is_traversal_forwards,
                    ST_Length(ttl.geom) AS cost,
                    NULL::text AS infrastructure_source_name,
                    NULL::text AS external_link_id,
                    NULL::jsonb AS link_name,
                    ST_AsEWKB(ST_Transform(ttl.geom, 4326)) as geom
                FROM trimmed_terminus_link ttl
                WHERE ttl.geom IS NOT NULL
            ) combined
            ORDER BY seq, trimmed;
            """.trimIndent()

        /**
         * Returns an SQL query that finds the shortest path through infrastructure network via
         * route points given as parameters. A route point is either (A) infrastructure node or
         * (B) "custom point", that is, a point along infrastructure link that does not coincide
         * with link's endpoints. The resulting route is returned as a sequence of route links
         * each of which refers to an infrastructure link. For each route link, the geometry of the
         * whole infrastructure link is returned. For only partially traversed infrastructure links
         * also a trimmed version of the infrastructure link geometry is returned that models the
         * real traversed path on the route.
         *
         * Custom points are bound to the query via array parameters. There are three arrays for
         * custom points to be bound while preparing the query. Each custom point is effectively a
         * triple consisting of the following properties:
         * (1) ID of the infrastructure link along which the point is located,
         * (2) fractional location (0,1) on a link,
         * (3) side of the road/street that the point affects with regard to the digitised direction
         * of the link (left, right, both).
         *
         * A visited points parameter is also bound as an array into the query. It consists of IDs
         * of all route points.
         *
         * There is an embedded query enclosed in quotes that restricts the infrastructure links
         * that are considered viable when finding the shortest path. The embedded query has its own
         * set of parameters that are handled in [PgRoutingEdgeQueries] object.
         *
         * There is also a decimal precision parameter that is used to round fractional locations
         * along infrastructure links. The rounding is done as an optimisation to shorten the
         * internally built query that generates the visited points data as an SQL query.
         *
         * The query utilises pgRouting's pgr_withPointsVia function.
         *
         * The query contains sub-queries that process the output of pgRouting in several stages.
         * The processing involves recognising the direction of traversal on each link which
         * information pgRouting itself does not provide. Because some route points given as
         * parameters (custom points) appear as extra infrastructure nodes in the pgRouting output,
         * the infrastructure links along which those custom points are located appear more times in
         * the output than what is desired in the final output. The processing removes consecutive
         * duplicate appearances of links in one direction of traversal. In addition, closed-loop
         * links require special treatment which is done in the processing.
         *
         * Since the query is quite long and complex a commentary in form of a sample data
         * transformation is provided below for sub-queries transforming the output of pgRouting.
         * By glancing over sample data transformations one can get a better idea what is going on
         * while executing the query.
         *
         * Sample output after "pgr" sub-query. "edge" means the ID of an infrastructure link and
         * "node" denotes either:
         * - infrastructure node, if positive
         * - custom point (relative location along infrastructure link), if negative
         *
         *  seq | path_id |  edge  |  node  |        cost
         * -----+---------+--------+--------+--------------------
         *    1 |       1 | 238709 |     -3 | 27.689157697826765
         *    2 |       1 |     -1 |     -4 |                  0
         *    3 |       2 | 238709 |     -4 | 27.689157697826765
         *    4 |       2 |     -1 |     -5 |                  0
         *    5 |       3 | 238709 |     -5 | 27.689157697826765
         *    6 |       3 | 238709 |     -4 | 27.689157697826765
         *    7 |       3 | 238709 |     -3 | 27.689157697826772
         *    8 |       3 | 238714 | 115666 | 13.741292778420942
         *    9 |       3 | 238712 | 115667 | 21.790898535769582
         *   10 |       3 | 238707 | 115663 |  21.20953754741562
         *   11 |       3 |     -1 |     -6 |                  0
         *   12 |       4 | 238707 |     -6 |  21.20953754741562
         *   13 |       4 |     -2 |     -7 |                  0
         *
         * Sample output after "pgr_transform1" sub-query. Start and end points are added. Positive
         * values refer to actual infrastructure nodes and negative values refer to given custom
         * points.
         *
         *  seq | path_id |  edge  |        cost        | start_point | end_point
         * -----+---------+--------+--------------------+-------------+-----------
         *    1 |       1 | 238709 | 27.689157697826765 |          -3 |        -4
         *    3 |       2 | 238709 | 27.689157697826765 |          -4 |        -5
         *    5 |       3 | 238709 | 27.689157697826765 |          -5 |        -4
         *    6 |       3 | 238709 | 27.689157697826765 |          -4 |        -3
         *    7 |       3 | 238709 | 27.689157697826772 |          -3 |    115666
         *    8 |       3 | 238714 | 13.741292778420942 |      115666 |    115667
         *    9 |       3 | 238712 | 21.790898535769582 |      115667 |    115663
         *   10 |       3 | 238707 |  21.20953754741562 |      115663 |        -6
         *   12 |       4 | 238707 |  21.20953754741562 |          -6 |        -7
         *
         * Sample output after "pgr_transform2" sub-query. Start and end fractions are derived for
         * each link traversal.
         *
         *  seq | path_id |  edge  | start_fraction | end_fraction
         * -----+---------+--------+----------------+--------------
         *    1 |       1 | 238709 |           0.75 |          0.5
         *    3 |       2 | 238709 |            0.5 |         0.25
         *    5 |       3 | 238709 |           0.25 |          0.5
         *    6 |       3 | 238709 |            0.5 |         0.75
         *    7 |       3 | 238709 |           0.75 |          1.0
         *    8 |       3 | 238714 |            0.0 |          1.0
         *    9 |       3 | 238712 |            0.0 |          1.0
         *   10 |       3 | 238707 |            0.0 |         0.33
         *   12 |       4 | 238707 |           0.33 |         0.66
         *
         * Sample output after "pgr_transform3" sub-query. Direction of traversal is determined
         * based on start and end fractions.
         *
         *  seq | path_id |  edge  | is_traversal_forwards | start_fraction | end_fraction
         * -----+---------+--------+-----------------------+----------------+--------------
         *    1 |       1 | 238709 | f                     |           0.75 |          0.5
         *    3 |       2 | 238709 | f                     |            0.5 |         0.25
         *    5 |       3 | 238709 | t                     |           0.25 |          0.5
         *    6 |       3 | 238709 | t                     |            0.5 |         0.75
         *    7 |       3 | 238709 | t                     |           0.75 |          1.0
         *    8 |       3 | 238714 | t                     |            0.0 |          1.0
         *    9 |       3 | 238712 | t                     |            0.0 |          1.0
         *   10 |       3 | 238707 | t                     |            0.0 |         0.33
         *   12 |       4 | 238707 | t                     |           0.33 |         0.66
         *
         * Sample output after "pgr_transform4" sub-query. Consecutive duplicates of links in one
         * direction of traversal are removed.
         *
         *  seq | path_id |  edge  | is_traversal_forwards | start_fraction | end_fraction
         * -----+---------+--------+-----------------------+----------------+--------------
         *    1 |       1 | 238709 | f                     |           0.75 |          0.5
         *    5 |       3 | 238709 | t                     |           0.25 |          0.5
         *    8 |       3 | 238714 | t                     |            0.0 |          1.0
         *    9 |       3 | 238712 | t                     |            0.0 |          1.0
         *   10 |       3 | 238707 | t                     |            0.0 |         0.33
         *
         * Sample output after "pgr_transform5" sub-query. End fractions are corrected after removal
         * of duplicate links in the previous stage. In addition, sequence numbers are reallocated.
         *
         *  seq |  edge  | is_traversal_forwards | start_fraction | end_fraction
         * -----+--------+-----------------------+----------------+--------------
         *    1 | 238709 | f                     |           0.75 |         0.25
         *    2 | 238709 | t                     |           0.25 |          1.0
         *    3 | 238714 | t                     |            0.0 |          1.0
         *    4 | 238712 | t                     |            0.0 |          1.0
         *    5 | 238707 | t                     |            0.0 |         0.66
         */
        private fun getQueryForFindingRouteViaPoints(roundedFractionDecimalPrecision: Int,
                                                     bufferAreaRestriction: BufferAreaRestriction?) =
            """
            WITH point_params AS (
                SELECT
                    ?::bigint[] AS edge_ids,
                    ?::numeric[] AS fractions,
                    ?::char[] AS sides
            ),
            visited_points AS (
                SELECT ?::bigint[] AS ids
            ),
            custom_point AS (
                SELECT
                    edge_index + 2 AS pid,
                    edge_id,
                    fraction,
                    round(fraction, $roundedFractionDecimalPrecision) AS rounded_fraction,
                    side::char
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
                INNER JOIN (
                    SELECT *, row_number() OVER () AS side_index
                    FROM (
                        SELECT unnest(sides) AS side FROM point_params
                    ) u
                ) sides ON side_index = edge_index
            ),
            points_sql AS (
                SELECT string_agg(sql, ' UNION ') AS txt
                FROM (
                    SELECT
                        CASE
                            WHEN pid = 3 THEN
                                'SELECT ' || pid || ' AS pid, ' || edge_id || ' AS edge_id, ' || rounded_fraction || ' AS fraction, ''' || side || '''::char AS side'
                            ELSE
                                'SELECT ' || pid || ', ' || edge_id || ', ' || rounded_fraction || ', ''' || side || '''::char'
                        END AS sql
                    FROM custom_point
                ) parts
            ),
            pgr AS (
                SELECT seq, path_id, edge, node, cost
                FROM visited_points pts
                CROSS JOIN points_sql sql
                CROSS JOIN pgr_withPointsVia(
                    ${createLinkSelectionQueryForPgRouting(bufferAreaRestriction)},
                    sql.txt,
                    pts.ids,
                    directed => true,
                    strict => true,  -- for topologically sound results
                    U_turn_on_edge => true,
                    driving_side => 'r'::char,
                    details => true  -- for flawless operation of the following transformations
                ) pgr
            ),
            pgr_transform1 AS (
                SELECT seq, path_id, edge, cost, node AS start_point, end_point
                FROM (
                    SELECT *, lead(node) OVER (ORDER BY seq) AS end_point FROM pgr
                ) sub
                WHERE edge >= 0
            ),
            pgr_transform2 AS (
                SELECT seq, path_id, edge,
                    CASE
                        WHEN pgr.start_point < 0 THEN p1.fraction
                        WHEN l.start_node_id = l.end_node_id THEN ( -- closed loop
                            CASE
                                WHEN l.traffic_flow_direction_type = 3 THEN 1.0
                                WHEN l.traffic_flow_direction_type = 4 THEN 0.0
                                ELSE ( -- bi-directional closed loop, determine direction by matching to pgr cost
                                    SELECT CASE WHEN forward_cost_diff < backward_cost_diff THEN 0.0 ELSE 1.0 END
                                    FROM (
                                        SELECT
                                            abs(pgr.cost - l.cost * p2.rounded_fraction) AS forward_cost_diff,
                                            abs(pgr.cost - l.cost * (1.0 - p2.rounded_fraction)) AS backward_cost_diff
                                    ) diffs
                                )
                            END
                        )
                        WHEN pgr.start_point = l.start_node_id THEN 0.0
                        ELSE 1.0
                    END AS start_fraction,
                    CASE
                        WHEN pgr.end_point < 0 THEN p2.fraction
                        WHEN l.start_node_id = l.end_node_id THEN ( -- closed loop
                            CASE
                                WHEN l.traffic_flow_direction_type = 3 THEN 0.0
                                WHEN l.traffic_flow_direction_type = 4 THEN 1.0
                                ELSE ( -- bi-directional closed loop, determine direction by matching to pgr cost
                                    SELECT CASE WHEN forward_cost_diff < backward_cost_diff THEN 1.0 ELSE 0.0 END
                                    FROM (
                                        SELECT
                                            abs(pgr.cost - l.cost * (1.0 - p1.rounded_fraction)) AS forward_cost_diff,
                                            abs(pgr.cost - l.cost * p1.rounded_fraction) AS backward_cost_diff
                                    ) diffs
                                )
                            END
                        )
                        WHEN pgr.end_point = l.start_node_id THEN 0.0
                        ELSE 1.0
                    END AS end_fraction
                FROM pgr_transform1 pgr
                INNER JOIN routing.infrastructure_link l ON l.infrastructure_link_id = pgr.edge
                LEFT JOIN custom_point p1 ON (pgr.start_point < 0 AND p1.pid = -pgr.start_point)
                LEFT JOIN custom_point p2 ON (pgr.end_point < 0 AND p2.pid = -pgr.end_point)
            ),
            pgr_transform3 AS (
                SELECT seq, path_id, edge, (end_fraction > start_fraction) AS is_traversal_forwards, start_fraction, end_fraction
                FROM pgr_transform2
                WHERE start_fraction <> end_fraction -- sanity check
            ),
            pgr_transform4 AS (
                SELECT seq, path_id, edge, is_traversal_forwards, start_fraction, end_fraction
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
                    -- closed loop cases ->
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
                            SELECT end_fraction AS last_end_fraction FROM pgr_transform2 ORDER BY seq DESC LIMIT 1 
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
            trimmed_route_link AS (
                SELECT
                    pgr.seq,
                    link.infrastructure_link_id,
                    CASE
                        WHEN pgr.is_traversal_forwards THEN ST_LineSubstring(link.geom, pgr.start_fraction, pgr.end_fraction)
                        ELSE ST_Reverse(ST_LineSubstring(link.geom, pgr.end_fraction, pgr.start_fraction))
                    END AS geom
                FROM pgr_transform5 pgr
                INNER JOIN routing.infrastructure_link link ON link.infrastructure_link_id = pgr.edge
                WHERE pgr.start_fraction NOT IN (0.0, 1.0) OR pgr.end_fraction NOT IN (0.0, 1.0)
            )
            SELECT *
            FROM (
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
                    trl.seq,
                    trl.infrastructure_link_id,
                    NULL::bool AS is_traversal_forwards,
                    ST_Length(trl.geom) AS cost,
                    NULL::text AS infrastructure_source_name,
                    NULL::text AS external_link_id,
                    NULL::jsonb AS link_name,
                    ST_AsEWKB(ST_Transform(trl.geom, 4326)) as geom
                FROM trimmed_route_link trl
            ) combined
            ORDER BY seq, trimmed;
            """.trimIndent()
    }
}
