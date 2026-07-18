package com.bank.docgen;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.security.autoconfigure.UserDetailsServiceAutoConfiguration;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

/**
 * Default form-login {@link UserDetailsServiceAutoConfiguration} is excluded: management uses JWT,
 * runtime uses API credentials, and actuator scrape (when enabled) uses a chain-local Basic store
 * (PRR-D01b) — never the Boot-generated default password.
 */
@SpringBootApplication(exclude = UserDetailsServiceAutoConfiguration.class)
@ConfigurationPropertiesScan
public class DocGenApplication {

    public static void main(String[] args) {
        SpringApplication.run(DocGenApplication.class, args);
    }
}
