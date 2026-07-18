package com.bank.docgen.infrastructure.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * PRR-D01b: HTTP Basic scrape credentials for actuator metrics/prometheus on
 * claimed-prod / acceptance-hardening paths. No in-image default password.
 */
@ConfigurationProperties(prefix = "docgen.actuator")
public record ActuatorScrapeProperties(
        boolean scrapeAuthEnabled,
        String scrapeUsername,
        String scrapePassword
) {
}
