package com.bank.docgen.runtime.service;

import com.bank.docgen.runtime.api.BatchResultView;

public class SyncBatchFailureException extends RuntimeException {

    private final BatchResultView batchResult;

    public SyncBatchFailureException(BatchResultView batchResult) {
        super("api.error.runtime.batchProcessingFailed");
        this.batchResult = batchResult;
    }

    public BatchResultView batchResult() {
        return batchResult;
    }

    public String messageKey() {
        return "api.error.runtime.batchProcessingFailed";
    }
}
