package fi.hsl.jore4.mapmatching.config

import org.geolatte.geom.builder.DSL.c
import org.geolatte.geom.builder.DSL.linestring
import org.geolatte.geom.crs.CrsRegistry
import org.hamcrest.MatcherAssert.assertThat
import org.hamcrest.Matchers.containsString
import org.junit.jupiter.api.Test
import tools.jackson.databind.json.JsonMapper

class JacksonGeoJsonConfigTest {
    @Test
    fun serializesGeolatteLineStringAsGeoJson() {
        val customizer = JacksonGeoJsonConfig().geolatteLineStringJsonCustomizer()
        val builder = JsonMapper.builder()
        customizer.customize(builder)
        val mapper = builder.build()

        val epsg3067 = CrsRegistry.getProjectedCoordinateReferenceSystemForEPSG(3067)
        val geometry = linestring(epsg3067, c(385795.1, 6672185.2), c(386284.0, 6673127.0))

        val payload: Map<String, Any> = mapOf("geometry" to geometry)
        val json = mapper.writeValueAsString(payload)

        assertThat(json, containsString("\"geometry\":{\"type\":\"LineString\""))
        assertThat(json, containsString("\"coordinates\":[[385795.1,6672185.2],[386284.0,6673127.0]]"))
    }
}
