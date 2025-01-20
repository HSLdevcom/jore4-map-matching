package fi.hsl.jore4.mapmatching.repository.routing

object PgRoutingEdgeQueries {
    private const val UNNAMED_BIND_VAR_INSIDE_QUOTED_SQL = "''' || ? || '''"

    /**
     * Generates an SQL query that fetches infrastructure links associated with
     * a specific vehicle type. The generated query is enclosed in quotes and
     * intended to be passed as string parameter to a pgRouting SQL function.
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
     * intended to be passed as string parameter to a pgRouting SQL function.
     *
     * @param vehicleTypeVariableName the variable name for vehicle type
     *
     * @return an SQL query enclosed in quotes as [java.lang.String]
     */
    fun getVehicleTypeConstrainedLinksQuery(vehicleTypeVariableName: String): String {
        // Using SQL string concatenation in order to be able to inject a bind
        // variable placeholder into the query. This way we enable assigning the
        // actual vehicle type value through PreparedStatement variable binding.
        return "$$ ${getVehicleTypeConstrainedLinksQueryInternal("$$ || quote_literal(:$vehicleTypeVariableName)")}"
    }

    /**
     * Generates an SQL query that fetches infrastructure links associated with
     * a specific vehicle type and either (1) coinciding with the geometrical
     * "buffer area" or (2) matching with the given terminus link identifiers or
     * (3) whose endpoint nodes match with the given terminus node identifiers.
     * The generated query is enclosed in quotes and intended to be passed as
     * string parameter to a pgRouting SQL function.
     *
     * @param numberOfTerminusLinkIds the number of placeholders to generate for
     * terminus link identifiers
     * @param numberOfTerminusNodeIds the number of placeholders to generate for
     * terminus node identifiers
     *
     * @return an SQL query enclosed in quotes as [java.lang.String]
     */
    fun getVehicleTypeAndBufferAreaConstrainedLinksQuery(
        numberOfTerminusLinkIds: Int,
        numberOfTerminusNodeIds: Int
    ): String {
        require(numberOfTerminusLinkIds >= 0) { "numberOfTerminusLinkIds must be non-negative" }
        require(numberOfTerminusNodeIds >= 0) { "numberOfTerminusNodeIds must be non-negative" }

        // Using SQL string concatenation in order to be able to inject a bind
        // variable placeholders into the query. This way we enable assigning the
        // actual values through PreparedStatement variable binding.

        fun getPlaceholdersConcatenated(numberOfPlaceholders: Int): String {
            return CharArray(numberOfPlaceholders) { '?' }.joinToString(
                prefix = "''' || ",
                separator = " || ''',''' || ",
                postfix = " || '''"
            )
        }

        val additionalTerminusPredicates: MutableList<String> = ArrayList()

        if (numberOfTerminusLinkIds > 0) {
            val quotedLinkIdsParam = getPlaceholdersConcatenated(numberOfTerminusLinkIds)
            additionalTerminusPredicates.add("l.infrastructure_link_id IN ($quotedLinkIdsParam)")
        }
        if (numberOfTerminusNodeIds > 0) {
            val quotedNodeIdsParam = getPlaceholdersConcatenated(numberOfTerminusNodeIds)

            additionalTerminusPredicates.add("l.start_node_id IN ($quotedNodeIdsParam)")
            additionalTerminusPredicates.add("l.end_node_id IN ($quotedNodeIdsParam)")
        }

        // Buffer area restriction is always applied.
        additionalTerminusPredicates.add(
            getBufferAreaRestriction(
                UNNAMED_BIND_VAR_INSIDE_QUOTED_SQL,
                UNNAMED_BIND_VAR_INSIDE_QUOTED_SQL
            )
        )

        val queryStart: String = getVehicleTypeConstrainedLinksQueryInternal(UNNAMED_BIND_VAR_INSIDE_QUOTED_SQL)

        val queryEnd: String =
            when (additionalTerminusPredicates.size) {
                1 -> "\n  AND " + additionalTerminusPredicates.first()

                else ->
                    additionalTerminusPredicates.joinToString(
                        prefix = "\n  AND (\n    ",
                        separator = "\n    OR ",
                        postfix = "\n  )"
                    )
            }

        return "'$queryStart$queryEnd'"
    }

    /**
     * Generates an SQL query that fetches infrastructure links associated with
     * a specific vehicle type and either (1) coinciding with the geometrical
     * "buffer area" derived from parameters [lineStringEwkbVariableName] and
     * [bufferRadiusVariableName] or (2) matching with terminus links identified
     * by [terminusLinkIdsVariableName] or (3) whose endpoint nodes match with
     * terminus node identifiers identified by [terminusNodeIdsVariableName].
     * The generated query is enclosed in quotes and intended to be passed as
     * string parameter to a pgRouting SQL function.
     *
     * @param vehicleTypeVariableName the variable name for vehicle type
     * @param terminusLinkIdsVariableName the optional variable name for the
     * list of terminus link identifiers. The actual value to be bound must be
     * given as an array literal like e.g. "{1,2,3}".
     * @param terminusNodeIdsVariableName the optional variable name for the
     * list of terminus node identifiers. The actual value to be bound must be
     * given as an array literal like e.g. "{1,2,3}".
     * @param lineStringEwkbVariableName the variable name for the LineString
     * geometry that is expanded in all directions to form a polygon
     * @param bufferRadiusVariableName the variable name for the distance with
     * which the LineString geometry is expanded in all directions
     *
     * @return an SQL query enclosed in quotes as [java.lang.String]
     */
    fun getVehicleTypeAndBufferAreaConstrainedLinksQuery(
        vehicleTypeVariableName: String,
        terminusLinkIdsVariableName: String? = null,
        terminusNodeIdsVariableName: String? = null,
        lineStringEwkbVariableName: String,
        bufferRadiusVariableName: String
    ): String {
        // Using SQL string concatenation in order to be able to inject a bind
        // variable placeholders into the query. This way we enable assigning the
        // actual values through PreparedStatement variable binding.
        fun wrapBindVariable(varName: String) = "$$ || quote_literal(:$varName) || $$"

        val wrappedVehicleType: String = wrapBindVariable(vehicleTypeVariableName)
        val wrappedLineStringEwkb: String = wrapBindVariable(lineStringEwkbVariableName)
        val wrappedBufferRadius: String = wrapBindVariable(bufferRadiusVariableName)

        val additionalTerminusPredicates: MutableList<String> = ArrayList()

        if (terminusLinkIdsVariableName != null) {
            additionalTerminusPredicates.add(
                "l.infrastructure_link_id = ANY((${wrapBindVariable(terminusLinkIdsVariableName)})::bigint[])"
            )
        }
        if (terminusNodeIdsVariableName != null) {
            additionalTerminusPredicates.add(
                "(${wrapBindVariable(terminusNodeIdsVariableName)})::bigint[] && ARRAY[l.start_node_id, l.end_node_id]"
            )
        }

        // Buffer area restriction is always applied.
        additionalTerminusPredicates.add(getBufferAreaRestriction(wrappedLineStringEwkb, wrappedBufferRadius))

        val queryStart: String = getVehicleTypeConstrainedLinksQueryInternal(wrappedVehicleType)

        val queryEnd: String =
            when (additionalTerminusPredicates.size) {
                1 -> "\n  AND " + additionalTerminusPredicates.first()

                else ->
                    additionalTerminusPredicates.joinToString(
                        prefix = "\n  AND (\n    ",
                        separator = "\n    OR ",
                        postfix = "\n  )"
                    )
            }

        return "$$ $queryStart$queryEnd$$"
    }

    private fun getVehicleTypeConstrainedLinksQueryInternal(vehicleTypeParameter: String): String =
        """
        SELECT l.infrastructure_link_id AS id,
          l.start_node_id AS source,
          l.end_node_id AS target,
          l.cost,
          l.reverse_cost
        FROM routing.infrastructure_link l
        INNER JOIN routing.infrastructure_link_safely_traversed_by_vehicle_type s
          ON s.infrastructure_link_id = l.infrastructure_link_id
        WHERE s.vehicle_type = $vehicleTypeParameter
        """.trimIndent()

    private fun getBufferAreaRestriction(
        lineStringEwkbParameter: String,
        bufferRadiusParameter: String
    ): String =
        "ST_Contains(ST_Buffer(ST_Transform(ST_GeomFromEWKB($lineStringEwkbParameter), 3067), $bufferRadiusParameter), l.geom)"
}
