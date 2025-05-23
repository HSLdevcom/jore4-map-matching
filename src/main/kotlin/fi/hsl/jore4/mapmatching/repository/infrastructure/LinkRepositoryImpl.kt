package fi.hsl.jore4.mapmatching.repository.infrastructure

import fi.hsl.jore4.mapmatching.model.InfrastructureLinkId
import fi.hsl.jore4.mapmatching.model.InfrastructureNodeId
import fi.hsl.jore4.mapmatching.model.TrafficFlowDirectionType
import fi.hsl.jore4.mapmatching.model.VehicleType
import fi.hsl.jore4.mapmatching.util.GeolatteUtils.toEwkb
import org.geolatte.geom.G2D
import org.geolatte.geom.Geometries.mkMultiPoint
import org.geolatte.geom.Point
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate
import org.springframework.stereotype.Repository
import org.springframework.transaction.annotation.Transactional
import java.sql.ResultSet

@Repository
class LinkRepositoryImpl(
    val jdbcTemplate: NamedParameterJdbcTemplate
) : ILinkRepository {
    private data class LinkResultItem(
        val pointSeqNum: Int,
        val infrastructureLinkId: InfrastructureLinkId,
        val trafficFlowDirectionType: TrafficFlowDirectionType,
        val closestDistance: Double,
        val closestPointFractionalMeasure: Double,
        val linkLength: Double,
        val startNodeId: InfrastructureNodeId,
        val endNodeId: InfrastructureNodeId
    )

    @Transactional(readOnly = true)
    override fun findClosestLinks(
        points: List<Point<G2D>>,
        vehicleType: VehicleType,
        distanceInMeters: Double
    ): Map<Int, SnapPointToLinkResult> {
        if (points.isEmpty()) {
            return emptyMap()
        }

        val resultItems: List<LinkResultItem> = findClosestLinksInternal(points, vehicleType, distanceInMeters, 1)

        return resultItems.associateBy(LinkResultItem::pointSeqNum, valueTransform = {
            val pointIndex = it.pointSeqNum - 1
            val point = points[pointIndex]

            SnapPointToLinkResult(
                point,
                distanceInMeters,
                SnappedPointOnLink(
                    it.infrastructureLinkId,
                    it.closestDistance,
                    it.closestPointFractionalMeasure,
                    it.trafficFlowDirectionType,
                    it.linkLength,
                    it.startNodeId,
                    it.endNodeId
                )
            )
        })
    }

    @Transactional(readOnly = true)
    override fun findNClosestLinks(
        points: List<Point<G2D>>,
        vehicleType: VehicleType,
        distanceInMeters: Double,
        limit: Int
    ): Map<Int, SnapPointToLinksResult> {
        if (points.isEmpty()) {
            return emptyMap()
        }

        val resultItems: List<LinkResultItem> =
            findClosestLinksInternal(points, vehicleType, distanceInMeters, limit)

        return resultItems
            .groupBy(LinkResultItem::pointSeqNum, valueTransform = {
                SnappedPointOnLink(
                    it.infrastructureLinkId,
                    it.closestDistance,
                    it.closestPointFractionalMeasure,
                    it.trafficFlowDirectionType,
                    it.linkLength,
                    it.startNodeId,
                    it.endNodeId
                )
            })
            .mapValues { entry ->
                val pointSeqNum = entry.key
                val point = points[pointSeqNum - 1]

                val closestLinks: List<SnappedPointOnLink> =
                    entry.value.sortedBy(
                        SnappedPointOnLink::closestDistance
                    )

                SnapPointToLinksResult(point, distanceInMeters, limit, closestLinks)
            }
    }

    private fun findClosestLinksInternal(
        points: List<Point<G2D>>,
        vehicleType: VehicleType,
        distanceInMeters: Double,
        limit: Int
    ): List<LinkResultItem> {
        // List of points is transformed to binary MultiPoint format (for compact representation).
        val ewkb: ByteArray = toEwkb(mkMultiPoint(points))

        val params =
            MapSqlParameterSource()
                .addValue("ewkb", ewkb)
                .addValue("vehicleType", vehicleType.value)
                .addValue("distance", distanceInMeters)
                .addValue("limit", limit)

        return jdbcTemplate.query(FIND_CLOSEST_LINKS_SQL, params) { rs: ResultSet, _: Int ->
            val pointSeqNum = rs.getInt("seq")

            val infrastructureLinkId = rs.getLong("infrastructure_link_id")
            val startNodeId = rs.getLong("start_node_id")
            val endNodeId = rs.getLong("end_node_id")

            val trafficFlowDirectionType = rs.getInt("traffic_flow_direction_type")
            val linkLength = rs.getDouble("infrastructure_link_len2d")

            val closestDistance = rs.getDouble("closest_distance")
            val closestPointFractionalMeasure = rs.getDouble("fractional_measure")

            LinkResultItem(
                pointSeqNum,
                InfrastructureLinkId(infrastructureLinkId),
                TrafficFlowDirectionType.from(trafficFlowDirectionType),
                closestDistance,
                closestPointFractionalMeasure,
                linkLength,
                InfrastructureNodeId(startNodeId),
                InfrastructureNodeId(endNodeId)
            )
        }
    }

    companion object {
        private val FIND_CLOSEST_LINKS_SQL =
            """
            SELECT
                path[1] AS seq,
                link.infrastructure_link_id,
                link.start_node_id,
                link.end_node_id,
                link.traffic_flow_direction_type,
                closest_link.distance AS closest_distance,
                closest_link_aux.fractional_measure,
                closest_link_aux.infrastructure_link_len2d
            FROM (
                SELECT (g.gdump).path AS path, (g.gdump).geom AS geom
                FROM (
                    SELECT ST_Dump(ST_Transform(ST_GeomFromEWKB(:ewkb), 3067)) AS gdump
                ) g
            ) point
            CROSS JOIN LATERAL (
                SELECT
                    link.infrastructure_link_id,
                    point.geom <-> link.geom AS distance
                FROM routing.infrastructure_link link
                INNER JOIN routing.infrastructure_link_safely_traversed_by_vehicle_type safe
                    ON safe.infrastructure_link_id = link.infrastructure_link_id
                WHERE ST_DWithin(point.geom, link.geom, :distance)
                    AND safe.vehicle_type = :vehicleType
                ORDER BY distance
                LIMIT :limit
            ) closest_link
            INNER JOIN routing.infrastructure_link link
                ON link.infrastructure_link_id = closest_link.infrastructure_link_id
            CROSS JOIN LATERAL (
                SELECT
                    ST_LineLocatePoint(link.geom, point.geom) AS fractional_measure,
                    ST_Length(link.geom) AS infrastructure_link_len2d
            ) closest_link_aux
            ORDER BY seq, closest_distance;
            """.trimIndent()
    }
}
