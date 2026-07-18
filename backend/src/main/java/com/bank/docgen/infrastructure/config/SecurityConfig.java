package com.bank.docgen.infrastructure.config;

import com.bank.docgen.authorization.management.web.JwtAuthenticationFilter;
import com.bank.docgen.runtime.security.ApiCredentialAuthenticationFilter;
import com.bank.docgen.runtime.security.RuntimeRateLimitFilter;
import java.util.ArrayList;
import java.util.List;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
public class SecurityConfig {

    @Bean
    @Order(1)
    SecurityFilterChain runtimeSecurityFilterChain(
            HttpSecurity http,
            RuntimeRateLimitFilter runtimeRateLimitFilter,
            ApiCredentialAuthenticationFilter apiCredentialAuthenticationFilter,
            ManagementSecurityHandlers managementSecurityHandlers
    ) throws Exception {
        http.securityMatcher(request -> {
            String path = request.getRequestURI();
            return path.matches("/api/[^/]+/v1/.*") && !path.startsWith("/api/management/");
        })
                .csrf(AbstractHttpConfigurer::disable)
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .exceptionHandling(ex -> ex
                        .authenticationEntryPoint(managementSecurityHandlers)
                        .accessDeniedHandler(managementSecurityHandlers))
                .authorizeHttpRequests(auth -> auth.anyRequest().authenticated())
                .addFilterBefore(runtimeRateLimitFilter, UsernamePasswordAuthenticationFilter.class)
                .addFilterBefore(apiCredentialAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);
        return http.build();
    }

    /**
     * Order 3 — after runtime (1) and optional actuator scrape Basic chain (2).
     * When scrape auth is enabled, metrics/prometheus are not anonymous (see
     * {@link ActuatorScrapeSecurityConfig}).
     */
    @Bean
    @Order(3)
    SecurityFilterChain managementSecurityFilterChain(
            HttpSecurity http,
            JwtAuthenticationFilter jwtAuthenticationFilter,
            ManagementSecurityHandlers managementSecurityHandlers,
            ActuatorScrapeProperties actuatorScrapeProperties
    ) throws Exception {
        List<String> permitAll = new ArrayList<>(List.of(
                "/healthz",
                "/readyz",
                "/actuator/health",
                "/actuator/health/**",
                "/v3/api-docs/**",
                "/swagger-ui/**",
                "/swagger-ui.html",
                "/api/management/v1/auth/login"
        ));
        if (!actuatorScrapeProperties.scrapeAuthEnabled()) {
            // Dev / non-hardening: keep anonymous scrape for local debugging (D01B-C5).
            permitAll.add("/actuator/prometheus");
            permitAll.add("/actuator/metrics");
            permitAll.add("/actuator/metrics/**");
        }
        http.csrf(AbstractHttpConfigurer::disable)
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .exceptionHandling(ex -> ex
                        .authenticationEntryPoint(managementSecurityHandlers)
                        .accessDeniedHandler(managementSecurityHandlers))
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(permitAll.toArray(String[]::new))
                        .permitAll()
                        .anyRequest()
                        .authenticated())
                .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);
        return http.build();
    }
}
