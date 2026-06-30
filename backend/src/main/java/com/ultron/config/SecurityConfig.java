package com.ultron.config;

import java.util.Arrays;
import java.util.List;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

/**
 * Phase 0 security: the API is reachable locally without login (solo-operator, local-first),
 * but the structure is in place so OAuth2 / mTLS can be layered in Phase 1.
 *
 * <ul>
 *   <li>CSRF disabled for the stateless JSON API.</li>
 *   <li>Strict CORS allow-list (no wildcard origins) sourced from {@code ultron.cors.allowed-origins}.</li>
 *   <li>Actuator health/info open for local checks; everything else permitted in Phase 0.</li>
 * </ul>
 */
@Configuration
public class SecurityConfig {

    private final UltronProperties properties;

    public SecurityConfig(UltronProperties properties) {
        this.properties = properties;
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
            .cors(Customizer.withDefaults())
            .csrf(csrf -> csrf.disable())
            .authorizeHttpRequests(auth -> auth
                .requestMatchers("/api/**", "/actuator/health", "/actuator/info").permitAll()
                .anyRequest().permitAll());
        return http.build();
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        List<String> origins = Arrays.stream(properties.getCors().getAllowedOrigins().split(","))
            .map(String::trim)
            .filter(s -> !s.isEmpty())
            .toList();

        CorsConfiguration config = new CorsConfiguration();
        config.setAllowedOrigins(origins);
        config.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "OPTIONS"));
        config.setAllowedHeaders(List.of("*"));
        config.setAllowCredentials(true);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", config);
        return source;
    }
}
