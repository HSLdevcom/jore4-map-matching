package fi.hsl.jore4.mapmatching.repository.infrastructure

import fi.hsl.jore4.mapmatching.model.InfrastructureLinkId
import fi.hsl.jore4.mapmatching.model.InfrastructureNodeId
import fi.hsl.jore4.mapmatching.model.LinkSide
import fi.hsl.jore4.mapmatching.model.TrafficFlowDirectionType
import fi.hsl.jore4.mapmatching.model.VehicleType
import fi.hsl.jore4.mapmatching.util.GeolatteUtils.toEwkb
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate
import org.springframework.stereotype.Repository
import org.springframework.transaction.annotation.Transactional
import java.sql.ResultSet
import java.util.concurrent.ConcurrentHashMap

@Repository
class StopRepositoryImpl @Autowired constructor(val jdbcTemplate: NamedParameterJdbcTemplate) : IStopRepository {

    private val cachedQueriesForFindingStopsByNationalId = ConcurrentHashMap<Int, String>()

    @Transactional(readOnly = true)
    override fun findStopsAndSnapToInfrastructureLinks(stopMatchParams: Collection<PublicTransportStopMatchParameters>,
                                                       maxDistanceBetweenExpectedAndActualStopLocation: Double)
        : List<SnapStopToLinkResult> {

        if (stopMatchParams.isEmpty()) {
            return emptyList()
        }

        val queryString: String = getQueryForFindingStopsByNationalId(stopMatchParams.size)

        val jdbcParams = MapSqlParameterSource()
            .addValue("vehicleType", VehicleType.GENERIC_BUS.value)
            .addValue("maxDistance", maxDistanceBetweenExpectedAndActualStopLocation)

        stopMatchParams.withIndex().forEach { (index: Int, params: PublicTransportStopMatchParameters) ->
            val seq = index + 1

            jdbcParams
                .addValue("nationalId$seq", params.nationalId)
                .addValue("srcLocation$seq", toEwkb(params.sourceLocation))
        }

        return jdbcTemplate.query(queryString, jdbcParams) { rs: ResultSet, _: Int ->
            val stopNationalId = rs.getInt("public_transport_stop_national_id")

            val stopSideOnLink: LinkSide =
                when (rs.getBoolean("is_on_direction_of_link_forward_traversal")) {
                    true -> LinkSide.RIGHT
                    false -> {
                        if (rs.wasNull()) // only rarely in the real world
                            LinkSide.BOTH
                        else
                            LinkSide.LEFT
                    }
                }

            val infrastructureLinkId = rs.getLong("infrastructure_link_id")
            val startNodeId = rs.getLong("start_node_id")
            val endNodeId = rs.getLong("end_node_id")

            val trafficFlowDirectionType = rs.getInt("traffic_flow_direction_type")
            val linkLength = rs.getDouble("infrastructure_link_len2d")

            val closestPointFractionalMeasure = rs.getDouble("fractional_measure")

            SnapStopToLinkResult(stopNationalId,
                                 stopSideOnLink,
                                 SnappedPointOnLink(InfrastructureLinkId(infrastructureLinkId),
                                                    0.0, // closest distance from stop to link is ignored
                                                    closestPointFractionalMeasure,
                                                    TrafficFlowDirectionType.from(trafficFlowDirectionType),
                                                    linkLength,
                                                    InfrastructureNodeId(startNodeId),
                                                    InfrastructureNodeId(endNodeId)))
        }
    }

    private fun getQueryForFindingStopsByNationalId(numberOfStops: Int): String =
        cachedQueriesForFindingStopsByNationalId.computeIfAbsent(numberOfStops,
                                                                 StopRepositoryImpl::createQueryForFindingStopsByNationalId)

    companion object {

        private fun createQueryForFindingStopsByNationalId(numberOfStops: Int): String {
            require(numberOfStops in 1..500) { "numberOfStops must be in range 1..500" }

            fun createUnionSubquery(numberOfStops: Int): String = (1..numberOfStops).joinToString(
                transform = { i: Int ->
                    "SELECT $i AS seq, :nationalId$i AS stop_national_id, :srcLocation$i AS src_location"
                },
                // Indent in separator should match the main query string below in order to have
                // properly formatted SQL result.
                separator = "\n                    UNION "
            )

            return """
                SELECT
                    stop.public_transport_stop_national_id,
                    stop.is_on_direction_of_link_forward_traversal,
                    link.infrastructure_link_id,
                    link.start_node_id,
                    link.end_node_id,
                    link.traffic_flow_direction_type,
                    ST_LineLocatePoint(link.geom, stop.geom) AS fractional_measure,
                    ST_Length(link.geom) AS infrastructure_link_len2d
                FROM (
                    ${createUnionSubquery(numberOfStops)}
                ) stop_params
                INNER JOIN routing.public_transport_stop stop ON
                    stop.public_transport_stop_national_id = stop_params.stop_national_id
                INNER JOIN routing.infrastructure_link link ON
                    link.infrastructure_link_id = stop.located_on_infrastructure_link_id
                INNER JOIN routing.infrastructure_link_safely_traversed_by_vehicle_type safe
                    ON safe.infrastructure_link_id = link.infrastructure_link_id
                WHERE ST_DWithin(stop.geom, ST_Transform(ST_GeomFromEWKB(stop_params.src_location), 3067), :maxDistance)
                    AND safe.vehicle_type = :vehicleType
                ORDER BY stop_params.seq ASC;""".trimIndent()
        }
    }
}
