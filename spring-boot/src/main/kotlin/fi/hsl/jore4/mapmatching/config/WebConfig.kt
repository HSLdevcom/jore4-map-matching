package fi.hsl.jore4.mapmatching.config

import fi.hsl.jore4.mapmatching.api.RouteController
import org.springframework.context.annotation.Configuration
import org.springframework.web.servlet.config.annotation.CorsRegistry
import org.springframework.web.servlet.config.annotation.PathMatchConfigurer
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer
import org.springframework.web.util.UrlPathHelper

@Configuration
class WebConfig : WebMvcConfigurer {

    override fun addCorsMappings(registry: CorsRegistry) {
        registry.addMapping(RouteController.URL_PREFIX + "/**")
            .allowedOrigins("*")
            .allowedMethods("GET")
    }

    override fun configurePathMatch(pathMatchConfig: PathMatchConfigurer) {
        // Prevent URL path variables from being split by semicolons in order to
        // support semicolon separated coordinates as request parameter.
        val urlPathHelper = UrlPathHelper()
        urlPathHelper.setRemoveSemicolonContent(false)
        pathMatchConfig.setUrlPathHelper(urlPathHelper)
    }
}
