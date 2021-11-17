package fi.hsl.jore4.mapmatching.config

import fi.hsl.jore4.mapmatching.api.RouteController
import fi.hsl.jore4.mapmatching.config.profile.Development
import org.springframework.context.annotation.Configuration
import org.springframework.web.servlet.config.annotation.CorsRegistry
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer

@Configuration
class WebConfig : WebMvcConfigurer {

    @Development
    @Configuration
    class DevelopmentWebConfig : WebMvcConfigurer {
        override fun addCorsMappings(registry: CorsRegistry) {
            // To fix browser errors while trying to invoke routing API from
            // a web page served from a local disk.
            registry.addMapping(RouteController.URL_PREFIX + "/**")
                .allowedOrigins("*")
                .allowedMethods("GET")
        }
    }
}
