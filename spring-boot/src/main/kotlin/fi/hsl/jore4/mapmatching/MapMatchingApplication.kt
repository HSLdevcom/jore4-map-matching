package fi.hsl.jore4.mapmatching

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration
import org.springframework.boot.runApplication
import org.springframework.transaction.annotation.EnableTransactionManagement

fun main(args: Array<String>) {
    runApplication<MapMatchingApplication>(*args)
}

/**
 * Spring boot application definition.
 *
 * Disable Spring Security initialization, since we don't use it at this stage.
 */
@SpringBootApplication(
    exclude = [
        SecurityAutoConfiguration::class
    ]
)
@EnableTransactionManagement
class MapMatchingApplication
