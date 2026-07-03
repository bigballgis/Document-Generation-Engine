package com.bank.docgen.apimgmt.domain;

import java.util.Set;

public final class ApiPolicyRetentionPresets {

    public static final Set<Integer> INVOCATION_RECORD_RETENTION_DAYS = Set.of(7, 30, 90, 180, 365, 1095, 2555);
    public static final Set<Integer> DOCUMENT_RETENTION_DAYS = Set.of(7, 30, 90, 180, 365);

    private ApiPolicyRetentionPresets() {
    }
}
