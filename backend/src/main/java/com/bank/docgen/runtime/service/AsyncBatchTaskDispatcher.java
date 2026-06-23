package com.bank.docgen.runtime.service;

import java.util.UUID;

public interface AsyncBatchTaskDispatcher {

    void dispatch(UUID taskId);
}
