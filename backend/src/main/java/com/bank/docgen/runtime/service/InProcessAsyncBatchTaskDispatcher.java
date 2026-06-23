package com.bank.docgen.runtime.service;

import java.util.UUID;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(name = "docgen.async.transport", havingValue = "in-process", matchIfMissing = true)
public class InProcessAsyncBatchTaskDispatcher implements AsyncBatchTaskDispatcher {

    private final AsyncBatchTaskRunner asyncBatchTaskRunner;

    public InProcessAsyncBatchTaskDispatcher(AsyncBatchTaskRunner asyncBatchTaskRunner) {
        this.asyncBatchTaskRunner = asyncBatchTaskRunner;
    }

    @Override
    public void dispatch(UUID taskId) {
        asyncBatchTaskRunner.run(taskId);
    }
}
