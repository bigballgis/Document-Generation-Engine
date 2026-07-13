package com.bank.docgen.template.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

/**
 * CE-K01: after Flyway adds the pinning columns (V57), retroactively pin already-published
 * releases that predate publish-time pinning. Idempotent — safe on every boot
 * (BDD-CE-K01-016..019).
 */
@Component
@Order(20)
public class ReleaseBundleBackfillRunner implements ApplicationRunner {

    private static final Logger LOG = LoggerFactory.getLogger(ReleaseBundleBackfillRunner.class);

    private final ReleaseBundleBackfillService releaseBundleBackfillService;

    public ReleaseBundleBackfillRunner(ReleaseBundleBackfillService releaseBundleBackfillService) {
        this.releaseBundleBackfillService = releaseBundleBackfillService;
    }

    @Override
    public void run(ApplicationArguments args) {
        ReleaseBundleBackfillResult result = releaseBundleBackfillService.runBackfill();
        LOG.info(
                "CE-K01 ReleaseBundleBackfillRunner finished: pinned={}, anomalies={}",
                result.pinnedCount(),
                result.anomalyCount()
        );
    }
}
