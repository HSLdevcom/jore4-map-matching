package fi.hsl.jore4.mapmatching.repository.infrastructure

import fi.hsl.jore4.mapmatching.model.LatLng
import fi.hsl.jore4.mapmatching.model.LocalisedName
import fi.hsl.jore4.mapmatching.util.GeolatteUtils.extractPointG2D
import fi.hsl.jore4.mapmatching.util.GeolatteUtils.fromEwkb
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate
import org.springframework.stereotype.Repository
import org.springframework.transaction.annotation.Transactional
import java.sql.ResultSet

@Repository
class StopRepositoryImpl @Autowired constructor(val jdbcTemplate: NamedParameterJdbcTemplate) : StopRepository {

    @Transactional(readOnly = true)
    override fun findAllStops(linkIds: Set<String>): List<StopInfoDTO> {
        if (linkIds.isEmpty()) {
            return emptyList()
        }

        val params = MapSqlParameterSource()
            .addValue("linkIds", linkIds)

        return jdbcTemplate.query(FIND_STOPS_BY_LINK_IDS_SQL, params) { rs: ResultSet, _: Int ->
            val nationalId = rs.getInt("valtak_id")
            val linkId = rs.getString("link_id")
            val nameFi = rs.getString("nimi_su")
            val nameSv = rs.getString("nimi_ru")
            val distanceFromLinkStart = rs.getDouble("sijainti_m")

            val direction = when (rs.getInt("vaik_suunt")) {
                2 -> DirectionType.ALONG_DIGITISED_DIRECTION
                3 -> DirectionType.AGAINST_DIGITISED_DIRECTION
                else -> DirectionType.UNKNOWN
            }

            val stopGeomBytes: ByteArray = rs.getBytes("geom")
            val stopCoord = LatLng.fromPointG2D(extractPointG2D(fromEwkb(stopGeomBytes)))

            StopInfoDTO(nationalId,
                        linkId,
                        stopCoord,
                        distanceFromLinkStart,
                        direction,
                        LocalisedName(nameFi, nameSv))
        }
    }

    companion object {
        private const val FIND_STOPS_BY_LINK_IDS_SQL =
            "SELECT \n" +
                "    valtak_id, \n" +
                "    link_id, \n" +
                "    nimi_su, \n" +
                "    nimi_ru, \n" +
                "    sijainti_m, \n" +
                "    vaik_suunt, \n" +
                "    ST_AsEWKB(ST_Transform(geom, 4326)) AS geom \n" +
                "FROM routing.dr_pysakki \n" +
                "WHERE link_id IN (:linkIds);"
    }
}
