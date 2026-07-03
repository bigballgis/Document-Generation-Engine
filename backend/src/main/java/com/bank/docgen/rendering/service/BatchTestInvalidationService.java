package com.bank.docgen.rendering.service;

import com.bank.docgen.rendering.persistence.BatchTestRunRepository;
import java.util.UUID;
import org.springframework.stereotype.Service;

/**
 * Invalidates the latest valid batch test run when template content changes.
 * Called within the same transaction as the content-modification operation.
 */
@Service
public class BatchTestInvalidationService {

    private final BatchTestRunRepository batchTestRunRepository;

    public BatchTestInvalidationService(BatchTestRunRepository batchTestRunRepository) {
        this.batchTestRunRepository = batchTestRunRepository;
    }

    /**
     * Marks the most recent non-invalidated, non-hidden batch test run for the given template as invalid.
     * No-op if no valid run exists.
     */
    public void invalidateLatestRun(UUID templateId) {
        batchTestRunRepository.findLatestValidByTemplateId(templateId).ifPresent(run -> {
            run.invalidate();
            batchTestRunRepository.save(run);
        });
    }
}
