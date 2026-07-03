package com.bank.docgen.rendering.listener;

import com.bank.docgen.rendering.service.BatchTestInvalidationService;
import com.bank.docgen.template.event.TemplateContentChangedEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

/**
 * Listens for template content changes and invalidates the latest valid batch test run
 * within the same transaction.
 */
@Component
public class BatchTestInvalidationListener {

    private final BatchTestInvalidationService batchTestInvalidationService;

    public BatchTestInvalidationListener(BatchTestInvalidationService batchTestInvalidationService) {
        this.batchTestInvalidationService = batchTestInvalidationService;
    }

    @EventListener
    public void onTemplateContentChanged(TemplateContentChangedEvent event) {
        batchTestInvalidationService.invalidateLatestRun(event.getTemplateId());
    }
}
