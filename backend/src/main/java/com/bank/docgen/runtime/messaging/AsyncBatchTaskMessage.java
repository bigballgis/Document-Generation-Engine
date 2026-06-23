package com.bank.docgen.runtime.messaging;

import com.fasterxml.jackson.annotation.JsonProperty;

public record AsyncBatchTaskMessage(
        @JsonProperty("taskId") String taskId
) {
}
