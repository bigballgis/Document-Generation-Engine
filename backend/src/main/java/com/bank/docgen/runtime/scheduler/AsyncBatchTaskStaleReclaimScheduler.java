package com.bank.docgen.runtime.scheduler;

import com.bank.docgen.runtime.service.AsyncBatchTaskStaleReclaimService;
import net.javacrumbs.shedlock.spring.annotation.SchedulerLock;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class AsyncBatchTaskStaleReclaimScheduler {

    private static final Logger LOG = LoggerFactory.getLogger(AsyncBatchTaskStaleReclaimScheduler.class);

    private final AsyncBatchTaskStaleReclaimService staleReclaimService;

    public AsyncBatchTaskStaleReclaimScheduler(AsyncBatchTaskStaleReclaimService staleReclaimService) {
        this.staleReclaimService = staleReclaimService;
    }

    @Scheduled(fixedDelayString = "${docgen.async.stale-reclaim-interval-ms:300000}")
    @SchedulerLock(
            name = "async-batch-stale-task-reclaim",
            lockAtMostFor = "PT10M",
            lockAtLeastFor = "PT20S"
    )
    public void reclaimStaleProcessingTasks() {
        int reclaimed = staleReclaimService.reclaimStaleTasks();
        if (reclaimed > 0) {
            LOG.info("[AsyncBatchStaleReclaimScheduler] Reclaimed {} stale task(s)", reclaimed);
        }
    }
}
