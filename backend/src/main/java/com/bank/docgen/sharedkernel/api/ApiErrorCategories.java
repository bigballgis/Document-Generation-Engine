package com.bank.docgen.sharedkernel.api;

public final class ApiErrorCategories {

    public static final String VALIDATION = "VALIDATION";
    public static final String AUTHENTICATION = "AUTHENTICATION";
    public static final String AUTHORIZATION = "AUTHORIZATION";
    public static final String MASTER = "MASTER";
    public static final String GENERATION = "GENERATION";
    public static final String TEMPLATE = "TEMPLATE";
    public static final String RENDERING = "RENDERING";
    public static final String APIMGMT = "APIMGMT";
    public static final String RUNTIME = "RUNTIME";
    public static final String IDEMPOTENCY = "IDEMPOTENCY";
    public static final String AUDIT = "AUDIT";
    public static final String ENCRYPTION = "ENCRYPTION";
    public static final String NOT_FOUND = "NOT_FOUND";
    public static final String CONFLICT = "CONFLICT";

    private ApiErrorCategories() {
    }
}
