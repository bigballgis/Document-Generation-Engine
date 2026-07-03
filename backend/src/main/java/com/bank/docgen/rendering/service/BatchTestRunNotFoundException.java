package com.bank.docgen.rendering.service;

public class BatchTestRunNotFoundException extends RuntimeException {

    public BatchTestRunNotFoundException() {
        super("Batch test run not found");
    }
}
