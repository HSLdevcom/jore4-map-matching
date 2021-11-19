package fi.hsl.jore4.mapmatching.web.util

import fi.hsl.jore4.mapmatching.model.LatLng
import fi.hsl.jore4.mapmatching.web.util.ParameterUtils.parseCoordinates
import org.hamcrest.MatcherAssert.assertThat
import org.hamcrest.Matchers.equalTo
import org.junit.jupiter.api.Test
import kotlin.test.assertFailsWith

class ParameterUtilsTest {

    @Test
    fun testParseCoordinates() {
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
    fun testParseCoordinates_withoutDecimalPart() {
        assertThat(parseCoordinates("24.1,60"), equalTo(listOf(LatLng(60.0, 24.1))))
        assertThat(parseCoordinates("24,60.2"), equalTo(listOf(LatLng(60.2, 24.0))))
        assertThat(parseCoordinates("24,60"), equalTo(listOf(LatLng(60.0, 24.0))))
    }

    @Test
    fun testParseCoordinates_withInvalidCoordinates() {
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
            val exception = assertFailsWith<IllegalArgumentException> {
                parseCoordinates(invalidCoord)
            }
            assertThat(exception.message, equalTo("Invalid coordinate sequence: \"$invalidCoord\""))
        }
    }
}
