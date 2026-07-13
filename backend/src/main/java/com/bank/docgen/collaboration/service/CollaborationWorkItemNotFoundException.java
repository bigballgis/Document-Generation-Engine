package com.bank.docgen.collaboration.service;

public class CollaborationWorkItemNotFoundException extends RuntimeException {

    public CollaborationWorkItemNotFoundException() {
        super("Collaboration work item not found");
    }
}
