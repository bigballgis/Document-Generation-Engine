package com.bank.docgen.infrastructure.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.context.event.EventListener;

/**
 * Logs multi-instance readiness constraints at prod startup (SOR-S07).
 */
@Configuration
@Profile("prod")
public class ProductionMultiInstanceGuard {

    private static final Logger LOG = LoggerFactory.getLogger(ProductionMultiInstanceGuard.class);

    @EventListener(ApplicationReadyEvent.class)
    void logMultiInstanceConstraints() {
        LOG.warn(
                "Multi-instance: enable RUNTIME_RATE_LIMIT_DISTRIBUTED before >1 replica; "
                        + "SSE progress requires sticky sessions until Redis pub/sub registry lands (ADR-0044)."
        );
    }
}
