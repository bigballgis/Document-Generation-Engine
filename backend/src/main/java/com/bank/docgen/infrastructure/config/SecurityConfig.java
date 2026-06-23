package com.bank.docgen.infrastructure.config;

import com.bank.docgen.authorization.management.web.JwtAuthenticationFilter;
import com.bank.docgen.runtime.security.ApiCredentialAuthenticationFilter;
import com.bank.docgen.runtime.security.RuntimeRateLimitFilter;
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

    @Bean
    @Order(2)
    SecurityFilterChain managementSecurityFilterChain(
            HttpSecurity http,
            JwtAuthenticationFilter jwtAuthenticationFilter,
            ManagementSecurityHandlers managementSecurityHandlers
    ) throws Exception {
        http.csrf(AbstractHttpConfigurer::disable)
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .exceptionHandling(ex -> ex
                        .authenticationEntryPoint(managementSecurityHandlers)
                        .accessDeniedHandler(managementSecurityHandlers))
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(
                                "/healthz",
                                "/readyz",
                                "/v3/api-docs/**",
                                "/swagger-ui/**",
                                "/swagger-ui.html",
                                "/api/management/v1/auth/login")
                        .permitAll()
                        .anyRequest()
                        .authenticated())
                .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);
        return http.build();
    }
}
