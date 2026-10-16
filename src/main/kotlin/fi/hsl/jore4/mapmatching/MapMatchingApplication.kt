package fi.hsl.jore4.mapmatching

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication
import org.springframework.boot.security.autoconfigure.UserDetailsServiceAutoConfiguration
import org.springframework.context.annotation.Bean
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
}
