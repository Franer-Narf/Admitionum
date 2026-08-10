package nc.admitionum.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.factory.PasswordEncoderFactories;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.provisioning.InMemoryUserDetailsManager;
import org.springframework.security.web.SecurityFilterChain;

@Configuration(proxyBeanMethods = false)
@EnableWebSecurity
public class SecurityConfig {

    @Bean
    public SecurityFilterChain applicationSecurityFilterChain(
            HttpSecurity http) throws Exception {

        http
            .authorizeHttpRequests(authorize -> authorize
                .requestMatchers(
                    "/",
                    "/index.html",
                    "/css/**",
                    "/js/**",
                    "/api/public/**",
                    "/error"
                )
                .permitAll()

                .requestMatchers(
                    "/admin/**",
                    "/api/admin/**"
                )
                .hasRole("ADMIN")

                .anyRequest()
                .denyAll()
            )

            .formLogin(form -> form
                .permitAll()
            )

            .logout(logout -> logout
                .logoutSuccessUrl("/")
                .permitAll()
            )

            .csrf(csrf -> csrf
                .ignoringRequestMatchers(
                    "/api/public/**"
                )
            );

        return http.build();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return PasswordEncoderFactories
            .createDelegatingPasswordEncoder();
    }

    @Bean
    public UserDetailsService userDetailsService(
            @Value("${app.admin.username}")
            String adminUsername,

            @Value("${app.admin.password}")
            String adminPassword,

            PasswordEncoder passwordEncoder) {

        UserDetails admin =
            User.builder()
                .username(adminUsername)
                .password(
                    passwordEncoder.encode(
                        adminPassword
                    )
                )
                .roles("ADMIN")
                .build();

        return new InMemoryUserDetailsManager(admin);
    }
}