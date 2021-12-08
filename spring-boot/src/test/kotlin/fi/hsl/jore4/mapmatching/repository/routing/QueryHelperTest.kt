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

            assertThat(query).isEqualTo(
                "'SELECT l.infrastructure_link_id AS id, " +
                    "l.start_node_id AS source, " +
                    "l.end_node_id AS target, " +
                    "l.cost, " +
                    "l.reverse_cost " +
                    "FROM routing.infrastructure_link l " +
                    "INNER JOIN routing.infrastructure_link_safely_traversed_by_vehicle_type s " +
                    "ON s.infrastructure_link_id = l.infrastructure_link_id " +
                    "WHERE s.vehicle_type = ''' || ? || ''''")
        }

        @Test
        @DisplayName("When vehicle type parameter is given explicitly")
        fun whenVehicleTypeParameterGivenExplicitly() {
            val query: String = QueryHelper.getVehicleTypeConstrainedLinksQuery(":vehicleType")

            assertThat(query).isEqualTo(
                "'SELECT l.infrastructure_link_id AS id, " +
                    "l.start_node_id AS source, " +
                    "l.end_node_id AS target, " +
                    "l.cost, " +
                    "l.reverse_cost " +
                    "FROM routing.infrastructure_link l " +
                    "INNER JOIN routing.infrastructure_link_safely_traversed_by_vehicle_type s " +
                    "ON s.infrastructure_link_id = l.infrastructure_link_id " +
                    "WHERE s.vehicle_type = ''' || :vehicleType || ''''")
        }
    }
}