package fi.hsl.jore4.mapmatching.config

import fi.hsl.jore4.mapmatching.api.MapMatchingController
import fi.hsl.jore4.mapmatching.api.RouteController
import org.springframework.context.annotation.Configuration
import org.springframework.http.HttpMethod
import org.springframework.security.config.annotation.web.builders.HttpSecurity
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity
import org.springframework.security.config.annotation.web.configuration.WebSecurityConfigurerAdapter
import org.springframework.security.config.http.SessionCreationPolicy

@Configuration
@EnableWebSecurity
class WebSecurityConfig : WebSecurityConfigurerAdapter() {

    override fun configure(httpSec: HttpSecurity) {
        httpSec
            .sessionManagement()
            .sessionCreationPolicy(SessionCreationPolicy.NEVER)

            .and()
            .csrf().disable()

            .authorizeRequests()

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
}
