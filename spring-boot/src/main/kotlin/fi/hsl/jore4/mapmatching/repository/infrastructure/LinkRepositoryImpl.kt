package fi.hsl.jore4.mapmatching.repository.infrastructure

import fi.hsl.jore4.mapmatching.model.InfrastructureLinkId
import fi.hsl.jore4.mapmatching.model.InfrastructureNodeId
import fi.hsl.jore4.mapmatching.model.NodeProximity
import fi.hsl.jore4.mapmatching.model.VehicleType
import fi.hsl.jore4.mapmatching.model.tables.InfrastructureLink
import fi.hsl.jore4.mapmatching.model.tables.records.InfrastructureLinkRecord
import fi.hsl.jore4.mapmatching.util.GeolatteUtils.toEwkb
import fi.hsl.jore4.mapmatching.util.MathUtils.clampToZero
import fi.hsl.jore4.mapmatching.util.MathUtils.isZeroOrNegative
import org.geolatte.geom.G2D
import org.geolatte.geom.Geometries.mkMultiPoint
import org.geolatte.geom.Point
import org.jooq.DSLContext
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate
import org.springframework.stereotype.Repository
import org.springframework.transaction.annotation.Transactional
import java.sql.ResultSet

@Repository
class LinkRepositoryImpl @Autowired constructor(val jdbcTemplate: NamedParameterJdbcTemplate,
                                                val dslContext: DSLContext)
    : ILinkRepository {

    @Transactional(readOnly = true)
    override fun findByIds(ids: Collection<InfrastructureLinkId>): List<InfrastructureLinkRecord> {
        if (ids.isEmpty()) {
            return emptyList()
        }

        val idValues: List<Long> = ids.map(InfrastructureLinkId::value)

        return dslContext
            .select()
            .from(LINK)
            .where(LINK.INFRASTRUCTURE_LINK_ID.`in`(idValues))
            .fetch()
            .into(InfrastructureLinkRecord::class.java)
    }

    private data class ClosestLinkResult(val pointSeqNum: Int,
                                         val infrastructureLinkId: InfrastructureLinkId,
                                         val closestDistance: Double,
                                         val startNodeId: InfrastructureNodeId,
                                         val endNodeId: InfrastructureNodeId,
                                         val distanceToStartNode: Double,
                                         val distanceToEndNode: Double)

    @Transactional(readOnly = true)
    override fun findClosestLinks(points: List<Point<G2D>>,
                                  vehicleType: VehicleType,
                                  distanceInMeters: Double): Map<Int, SnapPointToLinkDTO> {

        if (points.isEmpty()) {
            return emptyMap()
        }

        // List of points is transformed to binary MultiPoint format (for compact representation).
        val ewkb: ByteArray = toEwkb(mkMultiPoint(points))

        val params = MapSqlParameterSource()
            .addValue("ewkb", ewkb)
            .addValue("vehicleType", vehicleType.value)
            .addValue("distance", distanceInMeters)

        val resultItems: List<ClosestLinkResult> =
            jdbcTemplate.query(FIND_CLOSEST_LINKS_SQL, params) { rs: ResultSet, _: Int ->
                val pointSeqNum = rs.getInt("seq")
                val infrastructureLinkId = rs.getLong("infrastructure_link_id")
                val closestDistance = rs.getDouble("closest_distance")
                val startNodeId = rs.getLong("start_node_id")
                val endNodeId = rs.getLong("end_node_id")

                val distanceToStartNode = rs.getDouble("start_node_distance")
                val distanceToEndNode = rs.getDouble("end_node_distance")

                val zeroClampedDistanceToStartNode: Double
                val zeroClampedDistanceToEndNode: Double

                if (startNodeId == endNodeId
                    && (isZeroOrNegative(distanceToStartNode) || isZeroOrNegative(distanceToEndNode))
                ) {
                    zeroClampedDistanceToStartNode = 0.0
                    zeroClampedDistanceToEndNode = 0.0
                } else {
                    zeroClampedDistanceToStartNode = clampToZero(distanceToStartNode)
                    zeroClampedDistanceToEndNode = clampToZero(distanceToEndNode)
                }

                ClosestLinkResult(pointSeqNum,
                                  InfrastructureLinkId(infrastructureLinkId),
                                  closestDistance,
                                  InfrastructureNodeId(startNodeId),
                                  InfrastructureNodeId(endNodeId),
                                  zeroClampedDistanceToStartNode,
                                  zeroClampedDistanceToEndNode)
            }

        return resultItems.associateBy(ClosestLinkResult::pointSeqNum, valueTransform = {
            val pointIndex = it.pointSeqNum - 1
            val point = points[pointIndex]

            SnapPointToLinkDTO(point,
                               distanceInMeters,
                               SnappedLinkState(it.infrastructureLinkId,
                                                it.closestDistance,
                                                NodeProximity(it.startNodeId, it.distanceToStartNode),
                                                NodeProximity(it.endNodeId, it.distanceToEndNode)))
        })
    }

    companion object {
        private val LINK = InfrastructureLink.INFRASTRUCTURE_LINK

        private val FIND_CLOSEST_LINKS_SQL = """
            SELECT
                path[1] AS seq,
                closest_link.infrastructure_link_id,
                closest_link.distance AS closest_distance,
                closest_link.start_node_id,
                closest_link.end_node_id,
                point.geom <-> start_node.the_geom AS start_node_distance,
                point.geom <-> end_node.the_geom AS end_node_distance
            FROM (
                SELECT (g.gdump).path AS path, (g.gdump).geom AS geom
                FROM (
                    SELECT ST_Dump(ST_Transform(ST_GeomFromEWKB(:ewkb), 3067)) AS gdump
                ) AS g
            ) AS point, LATERAL (
               SELECT
                    link.infrastructure_link_id,
                    link.start_node_id,
                    link.end_node_id,
                    point.geom <-> link.geom AS distance
                FROM routing.infrastructure_link link
                INNER JOIN routing.infrastructure_link_safely_traversed_by_vehicle_type safe
                    ON safe.infrastructure_link_id = link.infrastructure_link_id
                WHERE ST_DWithin(point.geom, link.geom, :distance)
                    AND safe.vehicle_type = :vehicleType
                ORDER BY distance
                LIMIT 1
            ) AS closest_link
            INNER JOIN routing.infrastructure_link_vertices_pgr start_node ON start_node.id = closest_link.start_node_id
            INNER JOIN routing.infrastructure_link_vertices_pgr end_node ON end_node.id = closest_link.end_node_id
            ORDER BY seq ASC;
            """.trimIndent()
    }
}
