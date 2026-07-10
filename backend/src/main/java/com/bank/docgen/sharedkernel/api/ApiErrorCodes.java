package com.bank.docgen.sharedkernel.api;

public final class ApiErrorCodes {

    public static final String REQUEST_BODY_INVALID = "REQUEST_BODY_INVALID";
    public static final String INTERNAL_ERROR = "INTERNAL_ERROR";
    public static final String INVALID_CREDENTIALS = "INVALID_CREDENTIALS";
    public static final String SESSION_EXPIRED = "SESSION_EXPIRED";
    public static final String SESSION_ABSOLUTE_LIMIT_REACHED = "SESSION_ABSOLUTE_LIMIT_REACHED";
    public static final String SESSION_REVOKED = "SESSION_REVOKED";
    public static final String SESSION_VALIDATION_UNAVAILABLE = "SESSION_VALIDATION_UNAVAILABLE";
    public static final String ACCESS_DENIED = "ACCESS_DENIED";
    public static final String MASTER_NOT_FOUND = "MASTER_NOT_FOUND";
    public static final String MASTER_VALIDATION_FAILED = "MASTER_VALIDATION_FAILED";
    public static final String MASTER_INVALID_STATE = "MASTER_INVALID_STATE";
    public static final String MASTER_ANCHOR_INTEGRITY_FAILED = "MASTER_ANCHOR_INTEGRITY_FAILED";
    public static final String MASTER_INVALID_FILE = "MASTER_INVALID_FILE";
    public static final String TEMPLATE_NOT_FOUND = "TEMPLATE_NOT_FOUND";
    public static final String TEST_DATA_SET_NOT_FOUND = "TEST_DATA_SET_NOT_FOUND";
    public static final String TEMPLATE_VALIDATION_FAILED = "TEMPLATE_VALIDATION_FAILED";
    public static final String TEMPLATE_DEV_LINE_IN_FLIGHT = "TEMPLATE_DEV_LINE_IN_FLIGHT";
    public static final String TEMPLATE_VERSION_IMMUTABLE = "TEMPLATE_VERSION_IMMUTABLE";
    public static final String TEMPLATE_DEFAULT_ROUTE_TARGET = "TEMPLATE_DEFAULT_ROUTE_TARGET";
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
    public static final String IDEMPOTENCY_DIGEST_FAILED = "IDEMPOTENCY_DIGEST_FAILED";
    public static final String ASYNC_TASK_NOT_FOUND = "ASYNC_TASK_NOT_FOUND";
    public static final String ASYNC_TASK_CANCELLATION_NOT_ALLOWED = "ASYNC_TASK_CANCELLATION_NOT_ALLOWED";
    public static final String ASYNC_TASK_EXPIRED = "ASYNC_TASK_EXPIRED";
    public static final String BATCH_PROCESSING_FAILED = "BATCH_PROCESSING_FAILED";
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
    public static final String CONTENT_MODULE_NOT_FOUND = "CONTENT_MODULE_NOT_FOUND";
    public static final String CONTENT_MODULE_VALIDATION_FAILED = "CONTENT_MODULE_VALIDATION_FAILED";
    public static final String MODULE_REVIEW_ROLE_DENIED = "MODULE_REVIEW_ROLE_DENIED";
    public static final String MODULE_REVIEW_STATE_TRANSITION_DENIED = "MODULE_REVIEW_STATE_TRANSITION_DENIED";
    public static final String MODULE_CHANGE_DESCRIPTION_REQUIRED = "MODULE_CHANGE_DESCRIPTION_REQUIRED";
    public static final String MODULE_REJECTION_REASON_REQUIRED = "MODULE_REJECTION_REASON_REQUIRED";
    public static final String MODULE_REVIEW_REQUEST_INVALID = "MODULE_REVIEW_REQUEST_INVALID";
    public static final String CONTENT_MODULE_ROLE_DENIED = "CONTENT_MODULE_ROLE_DENIED";
    public static final String CONTENT_MODULE_IMPACT_CONFIRMATION_REQUIRED = "CONTENT_MODULE_IMPACT_CONFIRMATION_REQUIRED";
    public static final String CONTENT_MODULE_STATE_TRANSITION_DENIED = "CONTENT_MODULE_STATE_TRANSITION_DENIED";
    public static final String CONTENT_MODULE_REQUEST_INVALID = "CONTENT_MODULE_REQUEST_INVALID";
    public static final String CONTENT_MODULE_STRUCTURE_MISSING = "CONTENT_MODULE_STRUCTURE_MISSING";
    public static final String IMAGE_ASSET_NOT_FOUND = "IMAGE_ASSET_NOT_FOUND";
    public static final String SEAL_ASSET_NOT_FOUND = "SEAL_ASSET_NOT_FOUND";
    public static final String PREVIEW_CONCURRENCY_LIMIT_EXCEEDED = "PREVIEW_CONCURRENCY_LIMIT_EXCEEDED";
    public static final String PREVIEW_ARTIFACT_EXPIRED = "PREVIEW_ARTIFACT_EXPIRED";
    public static final String BATCH_TEST_RUN_NOT_FOUND = "BATCH_TEST_RUN_NOT_FOUND";
    public static final String INVOCATION_NOT_FOUND = "INVOCATION_NOT_FOUND";
    public static final String INVOCATION_RECORD_EXPIRED = "INVOCATION_RECORD_EXPIRED";
    public static final String INVOCATION_VIEW_INVALID = "INVOCATION_VIEW_INVALID";
    public static final String PDF_CONVERSION_CAPACITY_EXCEEDED = "PDF_CONVERSION_CAPACITY_EXCEEDED";
    /** LR-A6 / ADR-0043: assembled DOCX failed OOXML well-formedness validation. */
    public static final String OOXML_VALIDATION_FAILED = "OOXML_VALIDATION_FAILED";

    private ApiErrorCodes() {
    }
}
