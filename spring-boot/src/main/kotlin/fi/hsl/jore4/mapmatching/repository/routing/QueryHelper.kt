package fi.hsl.jore4.mapmatching.repository.routing

object QueryHelper {

    /**
     * Generates an SQL query that fetches infrastructure links associated with
     * a specific vehicle type. The generated query is enclosed in quotes and
     * intended to be passed as string parameter to pgr_dijkstraVia SQL
     * function.
     *
     * @param vehicleTypePlaceholder the vehicle type or name of the bind
     * variable for vehicle type. Defaults to "?".
     *
     * @return an SQL query enclosed in quotes as [java.lang.String]
     */
    fun getVehicleTypeConstrainedLinksQuery(vehicleTypePlaceholder: String = "?"): String {

        // Using SQL string concatenation in order to be able to inject a bind
        // variable placeholder into the query. This way we enable assigning the
        // actual vehicle type value through PreparedStatement variable binding.
        val quotedPlaceHolder = "' || $vehicleTypePlaceholder || '"

        return "'${getVehicleTypeConstrainedLinksQueryInternal("''$quotedPlaceHolder''")}'"
    }

    /**
     * Generates an SQL query that fetches infrastructure links associated with
     * a specific vehicle type and either (1) coinciding with the geometrical
     * "buffer area" derived from parameters [lineStringEwkbPlaceHolder] and
     * [bufferRadiusPlaceHolder] or (2) matching with terminus links identified
     * by [startLinkIdPlaceHolder] and [endLinkIdPlaceHolder]. The generated
     * query is enclosed in quotes and intended to be passed as string parameter
     * to pgr_dijkstraVia SQL function.
     *
     * @param vehicleTypePlaceholder the vehicle type or name of the bind
     * variable for vehicle type. Defaults to "?".
     * @param startLinkIdPlaceHolder the identifier of the start link or name of
     * the bind variable. Defaults to "?".
     * @param endLinkIdPlaceHolder the identifier of the end link or name of the
     * bind variable. Defaults to "?".
     * @param lineStringEwkbPlaceHolder the LineString geometry that is expanded
     * in all directions to form a polygon or name of the bind variable for it.
     * Defaults to "?".
     * @param bufferRadiusPlaceHolder the distance with which the LineString
     * geometry is expanded in all directions or name of the bind variable for
     * it. Defaults to "?".
     *
     * @return an SQL query enclosed in quotes as [java.lang.String]
     */
    fun getVehicleTypeAndBufferAreaConstrainedLinksQuery(vehicleTypePlaceholder: String = "?",
                                                         startLinkIdPlaceHolder: String = "?",
                                                         endLinkIdPlaceHolder: String = "?",
                                                         lineStringEwkbPlaceHolder: String = "?",
                                                         bufferRadiusPlaceHolder: String = "?"): String {

        // Using SQL string concatenation in order to be able to inject a bind
        // variable placeholders into the query. This way we enable assigning the
        // actual values through PreparedStatement variable binding.

        val quotedVehicleType = "' || $vehicleTypePlaceholder || '"
        val quotedStartLinkId = "' || $startLinkIdPlaceHolder || '"
        val quotedEndLinkId = "' || $endLinkIdPlaceHolder || '"
        val quotedLineStringEwkb = "' || $lineStringEwkbPlaceHolder || '"
        val quotedBufferRadius = "' || $bufferRadiusPlaceHolder || '"

        return "'${
            getVehicleTypeAndBufferAreaConstrainedLinksQueryInternal("''$quotedVehicleType''",
                                                                     "''$quotedStartLinkId''",
                                                                     "''$quotedEndLinkId''",
                                                                     "''$quotedLineStringEwkb''",
                                                                     "''$quotedBufferRadius''")
        }'"
    }

    private fun getVehicleTypeConstrainedLinksQueryInternal(vehicleTypeParameter: String): String =
        "SELECT l.infrastructure_link_id AS id, " +
            "l.start_node_id AS source, " +
            "l.end_node_id AS target, " +
            "l.cost, " +
            "l.reverse_cost " +
            "FROM routing.infrastructure_link l " +
            "INNER JOIN routing.infrastructure_link_safely_traversed_by_vehicle_type s " +
            "ON s.infrastructure_link_id = l.infrastructure_link_id " +
            "WHERE s.vehicle_type = $vehicleTypeParameter"

    private fun getVehicleTypeAndBufferAreaConstrainedLinksQueryInternal(vehicleTypeParameter: String,
                                                                         startLinkIdParameter: String,
                                                                         endLinkIdParameter: String,
                                                                         lineStringEwkbVariable: String,
                                                                         bufferRadiusVariable: String): String =
        getVehicleTypeConstrainedLinksQueryInternal(vehicleTypeParameter) +
            "AND (" +
            "  l.infrastructure_link_id IN ($startLinkIdParameter, $endLinkIdParameter)" +
            "  OR ST_Contains(ST_Buffer(ST_Transform(ST_GeomFromEWKB($lineStringEwkbVariable), 3067), $bufferRadiusVariable), l.geom)" +
            ")"
}
