package fi.hsl.jore4.mapmatching.config

import com.fasterxml.jackson.annotation.JsonInclude
import org.geolatte.geom.json.Setting
import org.geolatte.geom.json.jackson3.GeolatteGeomModule
import org.springframework.boot.jackson.autoconfigure.JsonMapperBuilderCustomizer
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import tools.jackson.databind.DeserializationFeature
import tools.jackson.module.kotlin.KotlinFeature
import tools.jackson.module.kotlin.KotlinModule

@Configuration
class JacksonConfig {
    @Bean
    fun jsonMapperCustomizer(): JsonMapperBuilderCustomizer =
        JsonMapperBuilderCustomizer { builder ->
            val geolatteModule = GeolatteGeomModule()
            geolatteModule.set(Setting.SUPPRESS_CRS_SERIALIZATION, true)

            builder
                .changeDefaultPropertyInclusion { it.withValueInclusion(JsonInclude.Include.NON_NULL) }
                .enable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)
                .addModule(
                    KotlinModule
                        .Builder()
                        .withReflectionCacheSize(512)
                        .configure(KotlinFeature.NullToEmptyCollection, false)
                        .configure(KotlinFeature.NullToEmptyMap, false)
                        .configure(KotlinFeature.NullIsSameAsDefault, false)
                        .configure(KotlinFeature.SingletonSupport, false)
                        .configure(KotlinFeature.StrictNullChecks, true)
                        .build()
                ).addModule(geolatteModule)
        }
}
