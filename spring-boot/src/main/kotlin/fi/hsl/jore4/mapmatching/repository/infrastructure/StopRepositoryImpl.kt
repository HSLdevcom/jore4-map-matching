package fi.hsl.jore4.mapmatching.repository.infrastructure

import fi.hsl.jore4.mapmatching.model.LatLng
import fi.hsl.jore4.mapmatching.util.GeolatteUtils.extractPointG2D
import fi.hsl.jore4.mapmatching.util.GeolatteUtils.fromEwkb
import fi.hsl.jore4.mapmatching.util.MultilingualString
import fi.hsl.jore4.mapmatching.util.component.IJsonbConverter
import org.jooq.JSONB
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate
import org.springframework.stereotype.Repository
import org.springframework.transaction.annotation.Transactional
import java.sql.ResultSet

@Repository
class StopRepositoryImpl @Autowired constructor(val jdbcTemplate: NamedParameterJdbcTemplate,
                                                val jsonbConverter: IJsonbConverter) : IStopRepository {

    @Transactional(readOnly = true)
    override fun findStopsAlongLinks(infrastructureLinkIds: Set<Long>): List<StopInfoDTO> {
        if (infrastructureLinkIds.isEmpty()) {
            return emptyList()
        }

        val params = MapSqlParameterSource()
            .addValue("infrastructureLinkIds", infrastructureLinkIds)

        return jdbcTemplate.query(FIND_STOPS_BY_INFRASTRUCTURE_LINK_IDS_SQL, params) { rs: ResultSet, _: Int ->

            val stopId = rs.getLong("public_transport_stop_id")
            val stopNationalId = rs.getInt("public_transport_stop_national_id")

            val infrastructureLinkId = rs.getLong("located_on_infrastructure_link_id")
            val distanceOfStopFromStartOfLink = rs.getDouble("distance_from_link_start_in_meters")

            val direction = when (rs.getBoolean("is_on_direction_of_link_forward_traversal")) {
                true -> DirectionType.ALONG_DIGITISED_DIRECTION
                false -> DirectionType.AGAINST_DIGITISED_DIRECTION
                null -> DirectionType.UNKNOWN
            }

            val nameJson = JSONB.jsonb(rs.getString("name"))
            val name = jsonbConverter.fromJson(nameJson, MultilingualString::class.java)

            val stopGeomBytes: ByteArray = rs.getBytes("geom")
            val stopCoord = LatLng.fromPointG2D(extractPointG2D(fromEwkb(stopGeomBytes)))

            StopInfoDTO(stopId,
                        stopNationalId,
                        stopCoord,
                        infrastructureLinkId,
                        distanceOfStopFromStartOfLink,
                        direction,
                        name)
        }
    }

    companion object {
        private const val FIND_STOPS_BY_INFRASTRUCTURE_LINK_IDS_SQL =
            "SELECT \n" +
                "    public_transport_stop_id, \n" +
                "    public_transport_stop_national_id, \n" +
                "    located_on_infrastructure_link_id, \n" +
                "    is_on_direction_of_link_forward_traversal, \n" +
                "    distance_from_link_start_in_meters, \n" +
                "    name, \n" +
                "    ST_AsEWKB(ST_Transform(geom, 4326)) AS geom \n" +
                "FROM routing.public_transport_stop \n" +
                "WHERE located_on_infrastructure_link_id IN (:infrastructureLinkIds);"
    }
}
