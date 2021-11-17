package fi.hsl.jore4.mapmatching.config

import fi.hsl.jore4.mapmatching.api.RouteController
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.http.HttpMethod
import org.springframework.security.config.annotation.web.builders.HttpSecurity
import org.springframework.security.config.annotation.web.builders.WebSecurity
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity
import org.springframework.security.config.annotation.web.configuration.WebSecurityConfigurerAdapter
import org.springframework.security.config.http.SessionCreationPolicy
import org.springframework.security.web.firewall.HttpFirewall
import org.springframework.security.web.firewall.StrictHttpFirewall

@Configuration
@EnableWebSecurity
class WebSecurityConfig : WebSecurityConfigurerAdapter() {

    override fun configure(httpSec: HttpSecurity) {
        httpSec
            .sessionManagement()
            .sessionCreationPolicy(SessionCreationPolicy.NEVER)

            .and()

            .authorizeRequests()
            .antMatchers(HttpMethod.GET,
                         RouteController.URL_PREFIX + "/**",
                         "/actuator/health"
            ).permitAll()
            .anyRequest().denyAll()
    }
}
