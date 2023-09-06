package fi.hsl.jore4.mapmatching.config

import fi.hsl.jore4.mapmatching.api.MapMatchingController
import fi.hsl.jore4.mapmatching.api.RouteController
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.http.HttpMethod
import org.springframework.security.config.Customizer.withDefaults
import org.springframework.security.config.annotation.web.builders.HttpSecurity
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity
import org.springframework.security.config.http.SessionCreationPolicy
import org.springframework.security.web.SecurityFilterChain

@Configuration
@EnableWebSecurity
class WebSecurityConfig {

    @Bean
    @Throws(Exception::class)
    fun configure(httpSecurity: HttpSecurity): SecurityFilterChain {
        return httpSecurity
            .sessionManagement { it.sessionCreationPolicy(SessionCreationPolicy.NEVER) }

            // CSRF is not needed.
            .csrf().disable()

            /** A CORS mapping is defined in [WebConfig] within "development" Spring profile. */
            .cors(withDefaults())

            .authorizeHttpRequests {
                it
                    .antMatchers(HttpMethod.GET,
                                 RouteController.URL_PREFIX + "/**",
                                 "/actuator/health",
                                 "/*" // matches static landing page for examining results from route API
                    ).permitAll()

                    .antMatchers(HttpMethod.POST,
                                 MapMatchingController.URL_PREFIX + "/**",
                                 RouteController.URL_PREFIX + "/**"
                    ).permitAll()

                    .anyRequest().denyAll()
            }
            .build()
    }
}
