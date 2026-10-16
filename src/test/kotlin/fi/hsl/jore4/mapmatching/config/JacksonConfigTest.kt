package fi.hsl.jore4.mapmatching.config

import org.geolatte.geom.G2D
import org.geolatte.geom.LineString
import org.geolatte.geom.builder.DSL.c
import org.geolatte.geom.builder.DSL.g
import org.geolatte.geom.builder.DSL.linestring
import org.geolatte.geom.crs.CoordinateReferenceSystems.WGS84
import org.geolatte.geom.crs.CrsRegistry
import org.hamcrest.MatcherAssert.assertThat
import org.hamcrest.Matchers.containsString
import org.hamcrest.Matchers.equalTo
import org.junit.jupiter.api.Test
import tools.jackson.databind.json.JsonMapper

class JacksonConfigTest {
    private val mapper: JsonMapper =
        JsonMapper
            .builder()
            .also { JacksonConfig().jsonMapperCustomizer().customize(it) }
            .build()

    private data class Payload(
        val isForwards: Boolean,
        val name: String?,
        val geometry: LineString<G2D>
    )

    @Test
    fun serializesKotlinPropertiesWithIsPrefixAndOmitsNulls() {
        val json = mapper.writeValueAsString(Payload(true, null, linestring(WGS84, g(24.9, 60.1), g(25.0, 60.2))))

        assertThat(
            json,
            equalTo(
                "{\"isForwards\":true,\"geometry\":{\"type\":\"LineString\",\"coordinates\":[[24.9,60.1],[25.0,60.2]]}}"
            )
        )
    }

    @Test
    fun deserializesGeoJsonLineString() {
        val json =
            "{\"isForwards\":false,\"geometry\":{\"type\":\"LineString\",\"coordinates\":[[24.9,60.1],[25.0,60.2]]}}"

        val payload = mapper.readValue(json, Payload::class.java)

        assertThat(payload, equalTo(Payload(false, null, linestring(WGS84, g(24.9, 60.1), g(25.0, 60.2)))))
    }

    @Test
    fun serializesGeolatteLineStringAsGeoJson() {
        val epsg3067 = CrsRegistry.getProjectedCoordinateReferenceSystemForEPSG(3067)
        val geometry = linestring(epsg3067, c(385795.1, 6672185.2), c(386284.0, 6673127.0))

        val payload: Map<String, Any> = mapOf("geometry" to geometry)
        val json = mapper.writeValueAsString(payload)

        assertThat(json, containsString("\"geometry\":{\"type\":\"LineString\""))
        assertThat(json, containsString("\"coordinates\":[[385795.1,6672185.2],[386284.0,6673127.0]]"))
    }
}
