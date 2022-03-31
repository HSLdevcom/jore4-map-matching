package fi.hsl.jore4.mapmatching.repository.routing

object QueryHelper {

    private const val UNNAMED_BIND_VAR_INSIDE_QUOTED_SQL = "''' || ? || '''"

    /**
     * Generates an SQL query that fetches infrastructure links associated with
     * a specific vehicle type. The generated query is enclosed in quotes and
     * intended to be passed as string parameter to pgr_dijkstraVia SQL
     * function.
     *
     * @return an SQL query enclosed in quotes as [java.lang.String]
     */
    fun getVehicleTypeConstrainedLinksQuery(): String {
        // Using SQL string concatenation in order to be able to inject a bind
        // variable placeholder into the query. This way we enable assigning the
        // actual vehicle type value through PreparedStatement variable binding.
        return "'${getVehicleTypeConstrainedLinksQueryInternal(UNNAMED_BIND_VAR_INSIDE_QUOTED_SQL)}'"
    }

    /**
     * Generates an SQL query that fetches infrastructure links associated with
     * a specific vehicle type. The generated query is enclosed in quotes and
     * intended to be passed as string parameter to pgr_dijkstraVia SQL
     * function.
     *
     * @param vehicleTypeVariableName the variable name for vehicle type
     *
     * @return an SQL query enclosed in quotes as [java.lang.String]
     */
    fun getVehicleTypeConstrainedLinksQuery(vehicleTypeVariableName: String): String {
        // Using SQL string concatenation in order to be able to inject a bind
        // variable placeholder into the query. This way we enable assigning the
        // actual vehicle type value through PreparedStatement variable binding.
        return "'${getVehicleTypeConstrainedLinksQueryInternal("''' || :$vehicleTypeVariableName || '''")}'"
    }

    /**
     * Generates an SQL query that fetches infrastructure links associated with
     * a specific vehicle type and either (1) coinciding with the geometrical
     * "buffer area" or (2) matching with the given terminus link identifiers.
     * The generated query is enclosed in quotes and intended to be passed as
     * string parameter to pgr_dijkstraVia SQL function.
     *
     * @param numberOfTerminusLinkIds the number of placeholders to generate for
     * terminus link identifiers
     *
     * @return an SQL query enclosed in quotes as [java.lang.String]
     */
    fun getVehicleTypeAndBufferAreaConstrainedLinksQuery(numberOfTerminusLinkIds: Int): String {
        require(numberOfTerminusLinkIds >= 0) { "numberOfTerminusLinkIds must be non-negative" }

        // Using SQL string concatenation in order to be able to inject a bind
        // variable placeholders into the query. This way we enable assigning the
        // actual values through PreparedStatement variable binding.

        return when (numberOfTerminusLinkIds) {
            0 -> "'${
                getVehicleTypeAndBufferAreaConstrainedLinksQueryInternal(UNNAMED_BIND_VAR_INSIDE_QUOTED_SQL,
                                                                         UNNAMED_BIND_VAR_INSIDE_QUOTED_SQL,
                                                                         UNNAMED_BIND_VAR_INSIDE_QUOTED_SQL)
            }'"
            else -> {
                val quotedLinkIdsParam = CharArray(numberOfTerminusLinkIds) { '?' }.joinToString(prefix = "''' || ",
                                                                                                 separator = " || ''',''' || ",
                                                                                                 postfix = " || '''")

                "'${
                    getVehicleTypeAndBufferAreaConstrainedLinksQueryInternal(UNNAMED_BIND_VAR_INSIDE_QUOTED_SQL,
                                                                             quotedLinkIdsParam,
                                                                             UNNAMED_BIND_VAR_INSIDE_QUOTED_SQL,
                                                                             UNNAMED_BIND_VAR_INSIDE_QUOTED_SQL)
                }'"
            }
        }
    }

    /**
     * Generates an SQL query that fetches infrastructure links associated with
     * a specific vehicle type and either (1) coinciding with the geometrical
     * "buffer area" derived from parameters [lineStringEwkbVariableName] and
     * [bufferRadiusVariableName] or (2) matching with terminus links identified
     * by [terminusLinkIdsVariableName]. The generated query is enclosed in
     * quotes and intended to be passed as string parameter to pgr_dijkstraVia
     * SQL function.
     *
     * @param vehicleTypeVariableName the variable name for vehicle type
     * @param terminusLinkIdsVariableName the variable name for the list of
     * terminus link identifiers
     * @param lineStringEwkbVariableName the variable name for the LineString
     * geometry that is expanded in all directions to form a polygon
     * @param bufferRadiusVariableName the variable name for the distance with
     * which the LineString geometry is expanded in all directions
     *
     * @return an SQL query enclosed in quotes as [java.lang.String]
     */
    fun getVehicleTypeAndBufferAreaConstrainedLinksQuery(vehicleTypeVariableName: String,
                                                         terminusLinkIdsVariableName: String,
                                                         lineStringEwkbVariableName: String,
                                                         bufferRadiusVariableName: String): String {

        // Using SQL string concatenation in order to be able to inject a bind
        // variable placeholders into the query. This way we enable assigning the
        // actual values through PreparedStatement variable binding.
        return "'${
            getVehicleTypeAndBufferAreaConstrainedLinksQueryInternal("''' || :$vehicleTypeVariableName || '''",
                                                                     "''' || :$terminusLinkIdsVariableName || '''",
                                                                     "''' || :$lineStringEwkbVariableName || '''",
                                                                     "''' || :$bufferRadiusVariableName || '''")
        }'"
    }

    private fun getVehicleTypeConstrainedLinksQueryInternal(vehicleTypeParameter: String): String = """
        SELECT l.infrastructure_link_id AS id,
          l.start_node_id AS source,
          l.end_node_id AS target,
          l.cost,
          l.reverse_cost
        FROM routing.infrastructure_link l
        INNER JOIN routing.infrastructure_link_safely_traversed_by_vehicle_type s
          ON s.infrastructure_link_id = l.infrastructure_link_id
        WHERE s.vehicle_type = $vehicleTypeParameter""".trimIndent()

    private fun getVehicleTypeAndBufferAreaConstrainedLinksQueryInternal(vehicleTypeParameter: String,
                                                                         lineStringEwkbParameter: String,
                                                                         bufferRadiusParameter: String): String =
        getVehicleTypeConstrainedLinksQueryInternal(vehicleTypeParameter) + """

        |  AND ${getBufferAreaRestriction(lineStringEwkbParameter, bufferRadiusParameter)}""".trimMargin()

    private fun getVehicleTypeAndBufferAreaConstrainedLinksQueryInternal(vehicleTypeParameter: String,
                                                                         terminusLinkIdsParameter: String,
                                                                         lineStringEwkbParameter: String,
                                                                         bufferRadiusParameter: String): String =
        getVehicleTypeConstrainedLinksQueryInternal(vehicleTypeParameter) + """

        |  AND (
        |    l.infrastructure_link_id IN ($terminusLinkIdsParameter)
        |    OR ${getBufferAreaRestriction(lineStringEwkbParameter, bufferRadiusParameter)}
        |  )""".trimMargin()

    private fun getBufferAreaRestriction(lineStringEwkbParameter: String, bufferRadiusParameter: String): String =
        "ST_Contains(ST_Buffer(ST_Transform(ST_GeomFromEWKB($lineStringEwkbParameter), 3067), $bufferRadiusParameter), l.geom)"
}
