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
}
