package fi.hsl.jore4.mapmatching.repository.routing

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test

class QueryHelperTest {

    @Nested
    @DisplayName("Get vehicle type constrained infrastructure links query (in SQL)")
    inner class GetVehicleTypeConstrainedLinksQuery {

        @Test
        @DisplayName("With default parameter")
        fun withDefaultParameter() {
            val query: String = QueryHelper.getVehicleTypeConstrainedLinksQuery()

            assertThat(query).isEqualTo("""
                'SELECT l.infrastructure_link_id AS id,
                  l.start_node_id AS source,
                  l.end_node_id AS target,
                  l.cost,
                  l.reverse_cost
                FROM routing.infrastructure_link l
                INNER JOIN routing.infrastructure_link_safely_traversed_by_vehicle_type s
                  ON s.infrastructure_link_id = l.infrastructure_link_id
                WHERE s.vehicle_type = ''' || ? || ''''""".trimIndent())
        }

        @Test
        @DisplayName("When vehicle type parameter is given explicitly")
        fun whenVehicleTypeParameterGivenExplicitly() {
            val query: String = QueryHelper.getVehicleTypeConstrainedLinksQuery("vehicleType")

            assertThat(query).isEqualTo("""
                'SELECT l.infrastructure_link_id AS id,
                  l.start_node_id AS source,
                  l.end_node_id AS target,
                  l.cost,
                  l.reverse_cost
                FROM routing.infrastructure_link l
                INNER JOIN routing.infrastructure_link_safely_traversed_by_vehicle_type s
                  ON s.infrastructure_link_id = l.infrastructure_link_id
                WHERE s.vehicle_type = ''' || :vehicleType || ''''""".trimIndent())
        }
    }

    @Nested
    @DisplayName("Get vehicle type and buffer area constrained infrastructure links query (in SQL)")
    inner class GetVehicleTypeAndBufferAreaConstrainedLinksQuery {

        @Nested
        @DisplayName("When not providing named parameters")
        inner class WithNonNamedParameters {

            @Test
            @DisplayName("Without terminus link or node IDs")
            fun withoutTerminusLinkOrNodeIds() {
                val query: String = QueryHelper.getVehicleTypeAndBufferAreaConstrainedLinksQuery(0, 0)

                assertThat(query).isEqualTo("""
                    'SELECT l.infrastructure_link_id AS id,
                      l.start_node_id AS source,
                      l.end_node_id AS target,
                      l.cost,
                      l.reverse_cost
                    FROM routing.infrastructure_link l
                    INNER JOIN routing.infrastructure_link_safely_traversed_by_vehicle_type s
                      ON s.infrastructure_link_id = l.infrastructure_link_id
                    WHERE s.vehicle_type = ''' || ? || '''
                      AND ST_Contains(ST_Buffer(ST_Transform(ST_GeomFromEWKB(''' || ? || '''), 3067), ''' || ? || '''), l.geom)'""".trimIndent())
            }

            @Nested
            @DisplayName("When only terminus link IDs present")
            inner class WithTerminusLinksOnly {

                @Test
                @DisplayName("With one terminus link ID")
                fun withOneTerminusLinkId() {
                    val query: String = QueryHelper.getVehicleTypeAndBufferAreaConstrainedLinksQuery(1, 0)

                    assertThat(query).isEqualTo("""
                        'SELECT l.infrastructure_link_id AS id,
                          l.start_node_id AS source,
                          l.end_node_id AS target,
                          l.cost,
                          l.reverse_cost
                        FROM routing.infrastructure_link l
                        INNER JOIN routing.infrastructure_link_safely_traversed_by_vehicle_type s
                          ON s.infrastructure_link_id = l.infrastructure_link_id
                        WHERE s.vehicle_type = ''' || ? || '''
                          AND (
                            l.infrastructure_link_id IN (''' || ? || ''')
                            OR ST_Contains(ST_Buffer(ST_Transform(ST_GeomFromEWKB(''' || ? || '''), 3067), ''' || ? || '''), l.geom)
                          )'""".trimIndent())
                }

                @Test
                @DisplayName("With two terminus link IDs")
                fun withTwoTerminusLinkIds() {
                    val query: String = QueryHelper.getVehicleTypeAndBufferAreaConstrainedLinksQuery(2, 0)

                    assertThat(query).isEqualTo("""
                        'SELECT l.infrastructure_link_id AS id,
                          l.start_node_id AS source,
                          l.end_node_id AS target,
                          l.cost,
                          l.reverse_cost
                        FROM routing.infrastructure_link l
                        INNER JOIN routing.infrastructure_link_safely_traversed_by_vehicle_type s
                          ON s.infrastructure_link_id = l.infrastructure_link_id
                        WHERE s.vehicle_type = ''' || ? || '''
                          AND (
                            l.infrastructure_link_id IN (''' || ? || ''',''' || ? || ''')
                            OR ST_Contains(ST_Buffer(ST_Transform(ST_GeomFromEWKB(''' || ? || '''), 3067), ''' || ? || '''), l.geom)
                          )'""".trimIndent())
                }
            }

            @Nested
            @DisplayName("When only terminus node IDs present")
            inner class WithTerminusNodesOnly {

                @Test
                @DisplayName("With one terminus node ID")
                fun withOneTerminusNodeId() {
                    val query: String = QueryHelper.getVehicleTypeAndBufferAreaConstrainedLinksQuery(0, 1)

                    assertThat(query).isEqualTo("""
                        'SELECT l.infrastructure_link_id AS id,
                          l.start_node_id AS source,
                          l.end_node_id AS target,
                          l.cost,
                          l.reverse_cost
                        FROM routing.infrastructure_link l
                        INNER JOIN routing.infrastructure_link_safely_traversed_by_vehicle_type s
                          ON s.infrastructure_link_id = l.infrastructure_link_id
                        WHERE s.vehicle_type = ''' || ? || '''
                          AND (
                            l.start_node_id IN (''' || ? || ''')
                            OR l.end_node_id IN (''' || ? || ''')
                            OR ST_Contains(ST_Buffer(ST_Transform(ST_GeomFromEWKB(''' || ? || '''), 3067), ''' || ? || '''), l.geom)
                          )'""".trimIndent())
                }

                @Test
                @DisplayName("With two terminus node IDs")
                fun withTwoTerminusNodeIds() {
                    val query: String = QueryHelper.getVehicleTypeAndBufferAreaConstrainedLinksQuery(0, 2)

                    assertThat(query).isEqualTo("""
                        'SELECT l.infrastructure_link_id AS id,
                          l.start_node_id AS source,
                          l.end_node_id AS target,
                          l.cost,
                          l.reverse_cost
                        FROM routing.infrastructure_link l
                        INNER JOIN routing.infrastructure_link_safely_traversed_by_vehicle_type s
                          ON s.infrastructure_link_id = l.infrastructure_link_id
                        WHERE s.vehicle_type = ''' || ? || '''
                          AND (
                            l.start_node_id IN (''' || ? || ''',''' || ? || ''')
                            OR l.end_node_id IN (''' || ? || ''',''' || ? || ''')
                            OR ST_Contains(ST_Buffer(ST_Transform(ST_GeomFromEWKB(''' || ? || '''), 3067), ''' || ? || '''), l.geom)
                          )'""".trimIndent())
                }
            }

            @Nested
            @DisplayName("When both terminus links and nodes are present")
            inner class WithTerminusLinksAndNodes {

                @Test
                @DisplayName("With one terminus link ID and one terminus node ID")
                fun withOneTerminusLinkIdAndOneTerminusNodeId() {
                    val query: String = QueryHelper.getVehicleTypeAndBufferAreaConstrainedLinksQuery(1, 1)

                    assertThat(query).isEqualTo("""
                        'SELECT l.infrastructure_link_id AS id,
                          l.start_node_id AS source,
                          l.end_node_id AS target,
                          l.cost,
                          l.reverse_cost
                        FROM routing.infrastructure_link l
                        INNER JOIN routing.infrastructure_link_safely_traversed_by_vehicle_type s
                          ON s.infrastructure_link_id = l.infrastructure_link_id
                        WHERE s.vehicle_type = ''' || ? || '''
                          AND (
                            l.infrastructure_link_id IN (''' || ? || ''')
                            OR l.start_node_id IN (''' || ? || ''')
                            OR l.end_node_id IN (''' || ? || ''')
                            OR ST_Contains(ST_Buffer(ST_Transform(ST_GeomFromEWKB(''' || ? || '''), 3067), ''' || ? || '''), l.geom)
                          )'""".trimIndent())
                }

                @Test
                @DisplayName("With two terminus link IDs and three terminus node IDs")
                fun withTwoTerminusLinkIdsAndThreeTerminusNodeIds() {
                    val query: String = QueryHelper.getVehicleTypeAndBufferAreaConstrainedLinksQuery(2, 3)

                    assertThat(query).isEqualTo("""
                        'SELECT l.infrastructure_link_id AS id,
                          l.start_node_id AS source,
                          l.end_node_id AS target,
                          l.cost,
                          l.reverse_cost
                        FROM routing.infrastructure_link l
                        INNER JOIN routing.infrastructure_link_safely_traversed_by_vehicle_type s
                          ON s.infrastructure_link_id = l.infrastructure_link_id
                        WHERE s.vehicle_type = ''' || ? || '''
                          AND (
                            l.infrastructure_link_id IN (''' || ? || ''',''' || ? || ''')
                            OR l.start_node_id IN (''' || ? || ''',''' || ? || ''',''' || ? || ''')
                            OR l.end_node_id IN (''' || ? || ''',''' || ? || ''',''' || ? || ''')
                            OR ST_Contains(ST_Buffer(ST_Transform(ST_GeomFromEWKB(''' || ? || '''), 3067), ''' || ? || '''), l.geom)
                          )'""".trimIndent())
                }
            }
        }
    }

    @Nested
    @DisplayName("When providing named parameters")
    inner class WithNamedParameters {

        @Test
        @DisplayName("Without terminus link or node IDs")
        fun withoutTerminusLinkOrNodeIds() {
            val query: String = QueryHelper.getVehicleTypeAndBufferAreaConstrainedLinksQuery(
                vehicleTypeVariableName = "vehicleType",
                lineStringEwkbVariableName = "lineStringEwkb",
                bufferRadiusVariableName = "bufferRadius")

            assertThat(query).isEqualTo("""
                'SELECT l.infrastructure_link_id AS id,
                  l.start_node_id AS source,
                  l.end_node_id AS target,
                  l.cost,
                  l.reverse_cost
                FROM routing.infrastructure_link l
                INNER JOIN routing.infrastructure_link_safely_traversed_by_vehicle_type s
                  ON s.infrastructure_link_id = l.infrastructure_link_id
                WHERE s.vehicle_type = ''' || :vehicleType || '''
                  AND ST_Contains(ST_Buffer(ST_Transform(ST_GeomFromEWKB(''' || :lineStringEwkb || '''), 3067), ''' || :bufferRadius || '''), l.geom)'""".trimIndent())
        }

        @Test
        @DisplayName("With terminus link IDs")
        fun withTerminusLinkIds() {
            val query: String = QueryHelper.getVehicleTypeAndBufferAreaConstrainedLinksQuery(
                vehicleTypeVariableName = "vehicleType",
                terminusLinkIdsVariableName = "terminusLinkIds",
                lineStringEwkbVariableName = "lineStringEwkb",
                bufferRadiusVariableName = "bufferRadius")

            assertThat(query).isEqualTo("""
                'SELECT l.infrastructure_link_id AS id,
                  l.start_node_id AS source,
                  l.end_node_id AS target,
                  l.cost,
                  l.reverse_cost
                FROM routing.infrastructure_link l
                INNER JOIN routing.infrastructure_link_safely_traversed_by_vehicle_type s
                  ON s.infrastructure_link_id = l.infrastructure_link_id
                WHERE s.vehicle_type = ''' || :vehicleType || '''
                  AND (
                    l.infrastructure_link_id IN (''' || :terminusLinkIds || ''')
                    OR ST_Contains(ST_Buffer(ST_Transform(ST_GeomFromEWKB(''' || :lineStringEwkb || '''), 3067), ''' || :bufferRadius || '''), l.geom)
                  )'""".trimIndent())
        }

        @Test
        @DisplayName("With terminus node IDs")
        fun withTerminusNodeIds() {
            val query: String = QueryHelper.getVehicleTypeAndBufferAreaConstrainedLinksQuery(
                vehicleTypeVariableName = "vehicleType",
                terminusNodeIdsVariableName = "terminusNodeIds",
                lineStringEwkbVariableName = "lineStringEwkb",
                bufferRadiusVariableName = "bufferRadius")

            assertThat(query).isEqualTo("""
                'SELECT l.infrastructure_link_id AS id,
                  l.start_node_id AS source,
                  l.end_node_id AS target,
                  l.cost,
                  l.reverse_cost
                FROM routing.infrastructure_link l
                INNER JOIN routing.infrastructure_link_safely_traversed_by_vehicle_type s
                  ON s.infrastructure_link_id = l.infrastructure_link_id
                WHERE s.vehicle_type = ''' || :vehicleType || '''
                  AND (
                    l.start_node_id IN (''' || :terminusNodeIds || ''')
                    OR l.end_node_id IN (''' || :terminusNodeIds || ''')
                    OR ST_Contains(ST_Buffer(ST_Transform(ST_GeomFromEWKB(''' || :lineStringEwkb || '''), 3067), ''' || :bufferRadius || '''), l.geom)
                  )'""".trimIndent())
        }

        @Test
        @DisplayName("With terminus link and node IDs")
        fun withTerminusLinkAndNodeIds() {
            val query: String = QueryHelper.getVehicleTypeAndBufferAreaConstrainedLinksQuery(
                vehicleTypeVariableName = "vehicleType",
                terminusLinkIdsVariableName = "terminusLinkIds",
                terminusNodeIdsVariableName = "terminusNodeIds",
                lineStringEwkbVariableName = "lineStringEwkb",
                bufferRadiusVariableName = "bufferRadius")

            assertThat(query).isEqualTo("""
                'SELECT l.infrastructure_link_id AS id,
                  l.start_node_id AS source,
                  l.end_node_id AS target,
                  l.cost,
                  l.reverse_cost
                FROM routing.infrastructure_link l
                INNER JOIN routing.infrastructure_link_safely_traversed_by_vehicle_type s
                  ON s.infrastructure_link_id = l.infrastructure_link_id
                WHERE s.vehicle_type = ''' || :vehicleType || '''
                  AND (
                    l.infrastructure_link_id IN (''' || :terminusLinkIds || ''')
                    OR l.start_node_id IN (''' || :terminusNodeIds || ''')
                    OR l.end_node_id IN (''' || :terminusNodeIds || ''')
                    OR ST_Contains(ST_Buffer(ST_Transform(ST_GeomFromEWKB(''' || :lineStringEwkb || '''), 3067), ''' || :bufferRadius || '''), l.geom)
                  )'""".trimIndent())
        }
    }
}
