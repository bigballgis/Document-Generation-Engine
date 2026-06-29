package com.bank.docgen.apimgmt.api;

public record SaveBatchLimitsRequest(boolean batchEnabled, int syncMaxItems, int asyncMaxItems, boolean confirmed) {
}
