package fi.hsl.jore4.mapmatching

import com.fasterxml.jackson.annotation.JsonInclude
import com.fasterxml.jackson.databind.ObjectMapper
import org.geolatte.geom.json.GeolatteGeomModule
import org.geolatte.geom.json.Setting
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.autoconfigure.security.servlet.UserDetailsServiceAutoConfiguration
import org.springframework.boot.runApplication
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Primary
import org.springframework.transaction.annotation.EnableTransactionManagement
import org.springframework.validation.beanvalidation.LocalValidatorFactoryBean
import org.springframework.validation.beanvalidation.MethodValidationPostProcessor

fun main(args: Array<String>) {
    runApplication<MapMatchingApplication>(*args)
}

/**
 * Spring boot application definition.
 *
 * Disable UserDetailsServiceAutoConfiguration, since we don't have user accounts.
 */
@SpringBootApplication(
    exclude = [
        UserDetailsServiceAutoConfiguration::class
    ]
)
@EnableTransactionManagement
class MapMatchingApplication {

    @Bean
    fun methodValidationPostProcessor(): MethodValidationPostProcessor {
        val mv = MethodValidationPostProcessor()
        mv.setValidator(validator())
        return mv
    }

    @Bean
    fun validator() = LocalValidatorFactoryBean()

    @Bean
    @Primary
    fun objectMapper(): ObjectMapper {
        val geolatteModule = GeolatteGeomModule()
        geolatteModule.set(Setting.SUPPRESS_CRS_SERIALIZATION, true)

        return ObjectMapper()
            .setSerializationInclusion(JsonInclude.Include.NON_NULL)
            .registerModule(geolatteModule)
    }
}
