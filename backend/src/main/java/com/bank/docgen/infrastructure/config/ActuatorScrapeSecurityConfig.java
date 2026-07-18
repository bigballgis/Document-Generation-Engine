package com.bank.docgen.infrastructure.config;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.ProviderManager;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.factory.PasswordEncoderFactories;
import org.springframework.security.provisioning.InMemoryUserDetailsManager;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.util.StringUtils;

/**
 * PRR-D01b: dedicated filter chain for actuator scrape paths (HTTP Basic).
 * Fail-closed when credentials are unset — anonymous and wrong Basic both get 401.
 * Uses a chain-local {@link AuthenticationManager} so scrape users never become the
 * global UserDetailsService (must not affect JWT management login).
 */
@Configuration
@ConditionalOnProperty(name = "docgen.actuator.scrape-auth-enabled", havingValue = "true")
public class ActuatorScrapeSecurityConfig {

    @Bean
    @Order(2)
    SecurityFilterChain actuatorScrapeSecurityFilterChain(
            HttpSecurity http,
            ActuatorScrapeProperties actuatorScrapeProperties
    ) throws Exception {
        AuthenticationManager scrapeAuthManager = scrapeAuthenticationManager(actuatorScrapeProperties);
        http.securityMatcher(
                        "/actuator/prometheus",
                        "/actuator/metrics",
                        "/actuator/metrics/**"
                )
                .csrf(AbstractHttpConfigurer::disable)
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authenticationManager(scrapeAuthManager)
                .httpBasic(basic -> basic.realmName("docgen-actuator-scrape"))
                .authorizeHttpRequests(auth -> auth.anyRequest().authenticated());
        return http.build();
    }

    private static AuthenticationManager scrapeAuthenticationManager(ActuatorScrapeProperties properties) {
        UserDetailsService scrapeUsers = scrapeUserDetailsService(properties);
        DaoAuthenticationProvider provider = new DaoAuthenticationProvider(scrapeUsers);
        provider.setPasswordEncoder(PasswordEncoderFactories.createDelegatingPasswordEncoder());
        return new ProviderManager(provider);
    }

    private static UserDetailsService scrapeUserDetailsService(ActuatorScrapeProperties properties) {
        String username = properties.scrapeUsername();
        String password = properties.scrapePassword();
        if (!StringUtils.hasText(username) || !StringUtils.hasText(password)) {
            // No default credentials in image/repo — empty store rejects all Basic attempts.
            return new InMemoryUserDetailsManager();
        }
        UserDetails user = User.withUsername(username.trim())
                .password("{noop}" + password)
                .roles("ACTUATOR_SCRAPE")
                .build();
        return new InMemoryUserDetailsManager(user);
    }
}
