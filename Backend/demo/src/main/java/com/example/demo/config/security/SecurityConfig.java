package com.example.demo.config.security;

import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;

@RequiredArgsConstructor
@Configuration
public class SecurityConfig {

    private final JwtConfig jwtConfig;

    @Bean
    public BCryptPasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    /**
     * NOTE on the authorization model (see AGENTS.md for the full write-up):
     * the actual route protection for "/api/**" is done by the custom
     * JwtInterceptor/RoleInterceptor (see config.web.WebConfig), not by this
     * filter chain. "/api/**" is intentionally permitAll here so Spring
     * Security doesn't duplicate/conflict with that interceptor-based
     * enforcement. anyRequest().authenticated() below is real, not dead
     * code, for any future endpoint outside the listed prefixes and outside
     * the interceptors' purview.
     */
    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                .cors(Customizer.withDefaults())
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(
                                "/swagger-ui/**",
                                "/swagger-ui/index.html",
                                "/v3/api-docs/**",
                                "/v3/api-docs.yaml",
                                "/configuration/**",
                                "/swagger-resources/**",
                                "/webjars/**",
                                "/api/**",
                                "/ws/**",
                                "/topic/**",
                                "/app/**"
                        ).permitAll()
                        .anyRequest().authenticated()
                )
                .oauth2ResourceServer(oauth2 -> oauth2
                        .jwt(jwt -> jwt.decoder(jwtConfig.jwtDecoder()))
                )
                .csrf(AbstractHttpConfigurer::disable);

        return http.build();
    }
}
