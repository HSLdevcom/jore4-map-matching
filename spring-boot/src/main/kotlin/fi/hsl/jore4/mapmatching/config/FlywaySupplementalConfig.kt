package fi.hsl.jore4.mapmatching.config

import org.flywaydb.core.api.configuration.FluentConfiguration
import org.springframework.boot.autoconfigure.flyway.FlywayConfigurationCustomizer
import org.springframework.context.annotation.Configuration

/**
 * The preferred way of defining Flyway configuration is to add Spring Boot parameters into
 * application.properties file. However, not all Flyway settings are available via Spring Boot
 * "namespace" (parameters starting with prefix "spring.flyway."). Therefore, this class exists
 * to make it possible to set up additional Flyway configuration parameters that cannot set via
 * application.properties.
 */
@Configuration
class FlywaySupplementalConfig : FlywayConfigurationCustomizer {

    override fun customize(configuration: FluentConfiguration?) {
        // Customising `search_path` is required at least by migrations that add PostGIS geometry columns.
        configuration?.run {
            initSql("SET search_path = extensions, routing;")
        }
    }
}
