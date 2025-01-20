package fi.hsl.jore4.mapmatching.config

import fi.hsl.jore4.mapmatching.api.RouteController
import fi.hsl.jore4.mapmatching.config.profile.Development
import org.springframework.context.annotation.Configuration
import org.springframework.web.servlet.config.annotation.CorsRegistry
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer
import org.springframework.web.servlet.resource.EncodedResourceResolver
import org.springframework.web.servlet.resource.PathResourceResolver

@Configuration
class WebConfig : WebMvcConfigurer {
    @Development
    @Configuration
    class DevelopmentWebConfig : WebMvcConfigurer {
        override fun addCorsMappings(registry: CorsRegistry) {
            // To fix browser errors while trying to invoke route API from
            // a web page served from a local disk.
            registry.addMapping(RouteController.URL_PREFIX + "/**")
                .allowedOrigins("*")
                .allowedMethods("GET")
        }
    }

    override fun addResourceHandlers(registry: ResourceHandlerRegistry) {
        registry
            .addResourceHandler("/*")
            .addResourceLocations("classpath:/static/")
            .setCachePeriod(3600)
            .resourceChain(true)
            .addResolver(EncodedResourceResolver())
            .addResolver(PathResourceResolver())
    }
}
