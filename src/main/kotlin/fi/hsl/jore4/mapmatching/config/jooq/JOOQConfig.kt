package fi.hsl.jore4.mapmatching.config.jooq

import org.jooq.conf.Settings
import org.jooq.conf.SettingsTools
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.PropertySource

/**
 * Most of the configuration is handled in the autoconfiguration
 *
 * @see org.springframework.boot.autoconfigure.jooq.JooqAutoConfiguration
 */
@Configuration
@PropertySource("classpath:db/jooq.properties")
class JOOQConfig {
    @Bean
    fun settings(): Settings =
        SettingsTools
            .defaultSettings()
            .withReturnAllOnUpdatableRecord(true)
}
