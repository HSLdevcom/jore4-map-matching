package fi.hsl.jore4.mapmatching.config

import org.geolatte.geom.LineString
import org.springframework.boot.jackson.autoconfigure.JsonMapperBuilderCustomizer
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import tools.jackson.core.JsonGenerator
import tools.jackson.databind.SerializationContext
import tools.jackson.databind.SerializationFeature
import tools.jackson.databind.ValueSerializer
import tools.jackson.databind.module.SimpleModule

@Configuration
class JacksonGeoJsonConfig {
    @Bean
    fun geolatteLineStringJsonCustomizer(): JsonMapperBuilderCustomizer =
        JsonMapperBuilderCustomizer { builder ->
            val module = SimpleModule("GeoLatteLineStringModule")
            @Suppress("UNCHECKED_CAST")
            module.addSerializer(
                LineString::class.java as Class<LineString<*>>,
                GeoLatteLineStringSerializer()
            )

            builder.addModule(module)
            builder.disable(SerializationFeature.FAIL_ON_SELF_REFERENCES)
            builder.enable(SerializationFeature.WRITE_SELF_REFERENCES_AS_NULL)
        }

    private class GeoLatteLineStringSerializer : ValueSerializer<LineString<*>>() {
        override fun serialize(
            value: LineString<*>,
            generator: JsonGenerator,
            context: SerializationContext
        ) {
            val positions = value.positions
            val coordinateDimension = positions.coordinateDimension

            generator.writeStartObject(value)
            generator.writeStringProperty("type", "LineString")
            generator.writeName("coordinates")
            generator.writeStartArray()

            val coordinateBuffer = DoubleArray(coordinateDimension)
            for (index in 0 until positions.size()) {
                positions.getCoordinates(index, coordinateBuffer)
                generator.writeStartArray()
                for (dimension in 0 until coordinateDimension) {
                    generator.writeNumber(coordinateBuffer[dimension])
                }
                generator.writeEndArray()
            }

            generator.writeEndArray()
            generator.writeEndObject()
        }
    }
}
