package com.bank.docgen.sharedkernel.api;

public final class ApiErrorCodes {

    public static final String REQUEST_BODY_INVALID = "REQUEST_BODY_INVALID";
    public static final String INTERNAL_ERROR = "INTERNAL_ERROR";
    public static final String INVALID_CREDENTIALS = "INVALID_CREDENTIALS";
    public static final String SESSION_EXPIRED = "SESSION_EXPIRED";
    public static final String ACCESS_DENIED = "ACCESS_DENIED";
    public static final String MASTER_NOT_FOUND = "MASTER_NOT_FOUND";
    public static final String MASTER_VALIDATION_FAILED = "MASTER_VALIDATION_FAILED";
    public static final String MASTER_INVALID_STATE = "MASTER_INVALID_STATE";
    public static final String MASTER_ANCHOR_INTEGRITY_FAILED = "MASTER_ANCHOR_INTEGRITY_FAILED";
    public static final String MASTER_INVALID_FILE = "MASTER_INVALID_FILE";
    public static final String TEMPLATE_NOT_FOUND = "TEMPLATE_NOT_FOUND";
    public static final String TEST_DATA_SET_NOT_FOUND = "TEST_DATA_SET_NOT_FOUND";
    public static final String TEMPLATE_VALIDATION_FAILED = "TEMPLATE_VALIDATION_FAILED";
    public static final String PREVIEW_NOT_FOUND = "PREVIEW_NOT_FOUND";
    public static final String RENDERING_FAILED = "RENDERING_FAILED";
    public static final String API_POLICY_NOT_FOUND = "API_POLICY_NOT_FOUND";
    public static final String INVALID_TIME_WINDOW = "INVALID_TIME_WINDOW";
    public static final String AUDIT_SCOPE_REQUIRED = "AUDIT_SCOPE_REQUIRED";
    public static final String DOCUMENT_NOT_FOUND = "DOCUMENT_NOT_FOUND";
    public static final String DOWNLOAD_URL_EXPIRED = "DOWNLOAD_URL_EXPIRED";
    public static final String BATCH_LIMIT_EXCEEDED = "BATCH_LIMIT_EXCEEDED";
    public static final String ITEM_ID_DUPLICATED = "ITEM_ID_DUPLICATED";
    public static final String OUTPUT_MODE_NOT_ALLOWED = "OUTPUT_MODE_NOT_ALLOWED";
    public static final String IDEMPOTENCY_KEY_CONFLICT = "IDEMPOTENCY_KEY_CONFLICT";
    public static final String ASYNC_TASK_NOT_FOUND = "ASYNC_TASK_NOT_FOUND";
    public static final String ASYNC_TASK_CANCELLATION_NOT_ALLOWED = "ASYNC_TASK_CANCELLATION_NOT_ALLOWED";
    public static final String ENCRYPTION_PARAMETER_INVALID = "ENCRYPTION_PARAMETER_INVALID";
    public static final String ENCRYPTION_NOT_ALLOWED = "ENCRYPTION_NOT_ALLOWED";
    public static final String ENCRYPTION_FAILED = "ENCRYPTION_FAILED";
    public static final String RATE_LIMIT_EXCEEDED = "RATE_LIMIT_EXCEEDED";
    public static final String USER_NOT_FOUND = "USER_NOT_FOUND";
    public static final String GROUP_NOT_FOUND = "GROUP_NOT_FOUND";
    public static final String USERNAME_ALREADY_EXISTS = "USERNAME_ALREADY_EXISTS";
    public static final String GROUP_CODE_ALREADY_EXISTS = "GROUP_CODE_ALREADY_EXISTS";
    public static final String GROUP_SCOPE_OUT_OF_RANGE = "GROUP_SCOPE_OUT_OF_RANGE";
    public static final String ROLE_ASSIGNMENT_NOT_ALLOWED = "ROLE_ASSIGNMENT_NOT_ALLOWED";
    public static final String USER_DELETE_NOT_ALLOWED = "USER_DELETE_NOT_ALLOWED";
    public static final String GROUP_MANAGEMENT_NOT_ALLOWED = "GROUP_MANAGEMENT_NOT_ALLOWED";

    private ApiErrorCodes() {
    }
}
