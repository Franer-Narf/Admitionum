package nc.admitionum.config;

import org.springframework.boot.security.autoconfigure.web.servlet.PathRequest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.web.SecurityFilterChain;

@Configuration(proxyBeanMethods = false)
@Profile("local")
public class LocalSecurityConfig {

    @Bean
    @Order(Ordered.HIGHEST_PRECEDENCE)
    public SecurityFilterChain
            h2ConsoleSecurityFilterChain(
                    HttpSecurity http)
                    throws Exception {

        http
            .securityMatcher(
                PathRequest.toH2Console()
            )

            .authorizeHttpRequests(
                authorize -> authorize
                    .anyRequest()
                    .permitAll()
            )

            .csrf(csrf -> csrf
                .disable()
            )

            .headers(headers -> headers
                .frameOptions(
                    frameOptions ->
                        frameOptions.sameOrigin()
                )
            );

        return http.build();
    }
}