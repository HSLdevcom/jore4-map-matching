package fi.hsl.jore4.mapmatching.repository.routing

object QueryHelper {

    internal fun getVehicleTypeConstrainedQueryForPgrDijkstra(vehicleTypeBindVariableName: String,
                                                              withSurroundingQuotes: Boolean = true): String {

        val quotedPlaceHolder = "' || $vehicleTypeBindVariableName || '"

        return if (withSurroundingQuotes)
            "'${getVehicleTypeConstrainedQueryForPgrDijkstra("''$quotedPlaceHolder''")}'"
        else
            getVehicleTypeConstrainedQueryForPgrDijkstra(quotedPlaceHolder)
    }

    private fun getVehicleTypeConstrainedQueryForPgrDijkstra(vehicleTypeParameter: String): String =
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
