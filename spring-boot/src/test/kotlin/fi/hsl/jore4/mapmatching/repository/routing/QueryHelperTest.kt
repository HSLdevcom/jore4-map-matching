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
            @DisplayName("With no terminus link IDs")
            fun withNoTerminusLinkIds() {
                val query: String = QueryHelper.getVehicleTypeAndBufferAreaConstrainedLinksQuery(0)

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

            @Test
            @DisplayName("With one terminus link ID")
            fun withOneTerminusLinkId() {
                val query: String = QueryHelper.getVehicleTypeAndBufferAreaConstrainedLinksQuery(1)

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
                val query: String = QueryHelper.getVehicleTypeAndBufferAreaConstrainedLinksQuery(2)

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

        @Test
        @DisplayName("With named parameters")
        fun withNamedParameters() {
            val query: String = QueryHelper.getVehicleTypeAndBufferAreaConstrainedLinksQuery("vehicleType",
                                                                                             "terminusLinkIds",
                                                                                             "lineStringEwkb",
                                                                                             "bufferRadius")
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
    }
}
