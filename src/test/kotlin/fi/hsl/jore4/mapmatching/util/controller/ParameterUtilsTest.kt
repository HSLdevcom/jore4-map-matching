package fi.hsl.jore4.mapmatching.util.controller

import fi.hsl.jore4.mapmatching.model.LatLng
import fi.hsl.jore4.mapmatching.model.VehicleMode
import fi.hsl.jore4.mapmatching.model.VehicleType
import fi.hsl.jore4.mapmatching.util.controller.ParameterUtils.findVehicleType
import fi.hsl.jore4.mapmatching.util.controller.ParameterUtils.parseCoordinates
import org.hamcrest.MatcherAssert.assertThat
import org.hamcrest.Matchers.equalTo
import org.hamcrest.Matchers.`is`
import org.hamcrest.Matchers.nullValue
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtensionContext
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.ArgumentsProvider
import org.junit.jupiter.params.provider.ArgumentsSource
import org.junit.jupiter.params.provider.EnumSource
import org.junit.jupiter.params.provider.ValueSource
import java.util.stream.Stream
import kotlin.test.assertFailsWith

class ParameterUtilsTest {
    @Nested
    @DisplayName("Parse coordinates")
    inner class ParseCoordinates {
        @Test
        @DisplayName("When given valid coordinates with decimal part")
        fun shouldParseValidCoordinatesWithDecimalPart() {
            assertThat(
                parseCoordinates("24.123,60.456"),
                equalTo(listOf(LatLng(60.456, 24.123)))
            )

            assertThat(
                parseCoordinates("24.123,60.456~24.987,60.654"),
                equalTo(listOf(LatLng(60.456, 24.123), LatLng(60.654, 24.987)))
            )

            assertThat(
                parseCoordinates("24.123,60.456~24.789,60.123~24.456,60.789"),
                equalTo(listOf(LatLng(60.456, 24.123), LatLng(60.123, 24.789), LatLng(60.789, 24.456)))
            )
        }

        @Test
        @DisplayName("When given valid coordinates without decimal part")
        fun shouldParseValidCoordinatesWithoutDecimalPart() {
            assertThat(parseCoordinates("24.1,60"), equalTo(listOf(LatLng(60.0, 24.1))))
            assertThat(parseCoordinates("24,60.2"), equalTo(listOf(LatLng(60.2, 24.0))))
            assertThat(parseCoordinates("24,60"), equalTo(listOf(LatLng(60.0, 24.0))))
        }

        @Test
        @DisplayName("When given invalid coordinates")
        fun shouldThrowExceptionOnInvalidCoordinates() {
            listOf(
                ",",
                "x",
                "24.123",
                "x,",
                "24.123,",
                "x,y",
                ",60.456",
                "x,60.456",
                "24.123,y",
                // empty values
                "",
                "   ",
                "~",
                " ~ ",
                " ~ ~ ",
                // no whitespace allowed
                " 24.123,60.456",
                "24.123 ,60.456",
                "24.123, 60.456",
                "24.123,60.456 ",
                // no Z axis allowed
                "24.123,60.456,789"
            ).forEach { invalidCoord ->
                val exception =
                    assertFailsWith<IllegalArgumentException> {
                        parseCoordinates(invalidCoord)
                    }
                assertThat(exception.message, equalTo("Invalid coordinate sequence: \"$invalidCoord\""))
            }
        }
    }

    @Nested
    @DisplayName("Find vehicle type")
    inner class FindVehicleType {
        @Nested
        @DisplayName("When transportation mode is given as string")
        inner class WhenTransportationModeIsGivenAsString {
            @ParameterizedTest
            @EnumSource(VehicleType::class)
            fun shouldReturnVehicleTypeEnumWhenGivenMatchingVehicleModeString(vehicleType: VehicleType) {
                val vehicleModeString: String = vehicleType.vehicleMode.value
                val vehicleTypeString: String = vehicleType.value

                val result: VehicleType? = findVehicleType(vehicleModeString, vehicleTypeString)

                assertThat(result, equalTo(vehicleType))
            }

            @ParameterizedTest
            @ArgumentsSource(VehicleModeTypeToDefaultVehicleTypeArgumentsProvider::class)
            fun shouldReturnDefaultVehicleTypeForVehicleModeWhenVehicleTypeParameterIsNull(
                vehicleMode: VehicleMode,
                expectedVehicleType: VehicleType
            ) {
                val vehicleModeString: String = vehicleMode.value
                val result: VehicleType? = findVehicleType(vehicleModeString, null)

                assertThat(result, equalTo(expectedVehicleType))
            }

            @ParameterizedTest
            @EnumSource(VehicleType::class)
            fun shouldReturnNullIfVehicleModeDoesNotMatchVehicleTypeString(vehicleType: VehicleType) {
                val vehicleTypeString: String = vehicleType.value
                val incorrectVehicleModes: List<VehicleMode> =
                    VehicleMode.values().filter { it != vehicleType.vehicleMode }

                incorrectVehicleModes.forEach { vehicleMode ->
                    val vehicleModeString: String = vehicleMode.value
                    val result: VehicleType? = findVehicleType(vehicleModeString, vehicleTypeString)

                    assertThat(result, `is`(nullValue()))
                }
            }

            @ParameterizedTest
            @ValueSource(strings = ["", " ", "abc", " def "])
            fun shouldReturnNullIfGivenInvalidVehicleTypes(vehicleTypeString: String) {
                VehicleMode.values().forEach { vehicleMode ->
                    val vehicleModeString: String = vehicleMode.value
                    val result: VehicleType? = findVehicleType(vehicleModeString, vehicleTypeString)

                    assertThat(result, `is`(nullValue()))
                }
            }
        }

        @Nested
        @DisplayName("When transportation mode is given as enum")
        inner class WhenTransportationModeIsGivenAsEnum {
            @ParameterizedTest
            @EnumSource(VehicleType::class)
            fun shouldReturnVehicleTypeEnumWhenGivenMatchingVehicleMode(vehicleType: VehicleType) {
                val vehicleTypeString: String = vehicleType.value
                val result: VehicleType? = findVehicleType(vehicleType.vehicleMode, vehicleTypeString)

                assertThat(result, equalTo(vehicleType))
            }

            @ParameterizedTest
            @ArgumentsSource(VehicleModeTypeToDefaultVehicleTypeArgumentsProvider::class)
            fun shouldReturnDefaultVehicleTypeForVehicleModeWhenVehicleTypeParameterIsNull(
                vehicleMode: VehicleMode,
                expectedVehicleType: VehicleType
            ) {
                val result: VehicleType? = findVehicleType(vehicleMode, null)

                assertThat(result, equalTo(expectedVehicleType))
            }

            @ParameterizedTest
            @EnumSource(VehicleType::class)
            fun shouldReturnNullIfVehicleModeDoesNotMatchVehicleTypeString(vehicleType: VehicleType) {
                val vehicleTypeString: String = vehicleType.value
                val incorrectVehicleModes: List<VehicleMode> =
                    VehicleMode.values().filter { it != vehicleType.vehicleMode }

                incorrectVehicleModes.forEach { vehicleMode ->
                    val result: VehicleType? = findVehicleType(vehicleMode, vehicleTypeString)

                    assertThat(result, `is`(nullValue()))
                }
            }

            @ParameterizedTest
            @ValueSource(strings = ["", " ", "abc", " def "])
            fun shouldReturnNullIfGivenInvalidVehicleTypes(vehicleTypeString: String) {
                VehicleMode.values().forEach { vehicleMode ->
                    val result: VehicleType? = findVehicleType(vehicleMode, vehicleTypeString)

                    assertThat(result, `is`(nullValue()))
                }
            }
        }
    }

    class VehicleModeTypeToDefaultVehicleTypeArgumentsProvider : ArgumentsProvider {
        override fun provideArguments(extensionContext: ExtensionContext): Stream<out Arguments> =
            Stream.of(
                Arguments.of(VehicleMode.BUS, VehicleType.GENERIC_BUS),
                Arguments.of(VehicleMode.TRAM, VehicleType.GENERIC_TRAM),
                Arguments.of(VehicleMode.TRAIN, VehicleType.GENERIC_TRAIN),
                Arguments.of(VehicleMode.METRO, VehicleType.GENERIC_METRO),
                Arguments.of(VehicleMode.FERRY, VehicleType.GENERIC_FERRY)
            )
    }
}
