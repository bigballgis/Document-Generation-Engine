package com.bank.docgen.collaboration.scheduler;

import com.bank.docgen.collaboration.service.CollaborationEscalationService;
import net.javacrumbs.shedlock.spring.annotation.SchedulerLock;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(
        prefix = "docgen.collaboration.escalation",
        name = "enabled",
        havingValue = "true",
        matchIfMissing = true
)
public class CollaborationEscalationScheduler {

    private static final Logger LOG = LoggerFactory.getLogger(CollaborationEscalationScheduler.class);

    private final CollaborationEscalationService escalationService;

    public CollaborationEscalationScheduler(CollaborationEscalationService escalationService) {
        this.escalationService = escalationService;
    }

    // LR-B2: lockAtMostFor PT10M >> observed runtime (seconds); lockAtLeastFor PT20S << 5min interval.
    @Scheduled(fixedDelayString = "${docgen.collaboration.escalation.fixed-delay-ms:300000}")
    @SchedulerLock(
            name = "collaboration-escalation-check",
            lockAtMostFor = "PT10M",
            lockAtLeastFor = "PT20S"
    )
    public void runEscalationCheck() {
        int created = escalationService.processDueEscalations();
        if (created > 0) {
            LOG.info("Collaboration timeout escalation created {} notification item(s)", created);
        }
    }
}
