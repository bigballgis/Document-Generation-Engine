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
    public static final String MASTER_REVISION_IN_USE_BY_PUBLISHED_RELEASE = "MASTER_REVISION_IN_USE_BY_PUBLISHED_RELEASE";
    public static final String MASTER_REVISION_DELETE_FAILED = "MASTER_REVISION_DELETE_FAILED";
    public static final String TEMPLATE_NOT_FOUND = "TEMPLATE_NOT_FOUND";
    public static final String TEST_DATA_SET_NOT_FOUND = "TEST_DATA_SET_NOT_FOUND";
    public static final String TEMPLATE_VALIDATION_FAILED = "TEMPLATE_VALIDATION_FAILED";
    public static final String TEMPLATE_DEV_LINE_IN_FLIGHT = "TEMPLATE_DEV_LINE_IN_FLIGHT";
    public static final String TEMPLATE_VERSION_IMMUTABLE = "TEMPLATE_VERSION_IMMUTABLE";
    public static final String TEMPLATE_DEFAULT_ROUTE_TARGET = "TEMPLATE_DEFAULT_ROUTE_TARGET";
    /** CE-U21: anchor binding update expectedUpdatedAt mismatch. */
    public static final String BINDING_VERSION_CONFLICT = "BINDING_VERSION_CONFLICT";
    public static final String PREVIEW_NOT_FOUND = "PREVIEW_NOT_FOUND";
    public static final String RENDERING_FAILED = "RENDERING_FAILED";
    public static final String API_POLICY_NOT_FOUND = "API_POLICY_NOT_FOUND";
    public static final String INVALID_TIME_WINDOW = "INVALID_TIME_WINDOW";
    public static final String AUDIT_SCOPE_REQUIRED = "AUDIT_SCOPE_REQUIRED";
    public static final String DOCUMENT_NOT_FOUND = "DOCUMENT_NOT_FOUND";
    public static final String DOWNLOAD_URL_EXPIRED = "DOWNLOAD_URL_EXPIRED";
    public static final String BATCH_LIMIT_EXCEEDED = "BATCH_LIMIT_EXCEEDED";
    public static final String ITEM_ID_DUPLICATED = "ITEM_ID_DUPLICATED";
    /** CE-C05: originalBatchId not visible as same-credential BATCH_ROOT (opaque 404). */
    public static final String ORIGINAL_BATCH_NOT_FOUND = "ORIGINAL_BATCH_NOT_FOUND";
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
    /** CE-O01 / ADR-0058: PDF/A-2b archival profile cannot combine with encryption. */
    public static final String PDF_ARCHIVAL_ENCRYPTION_MUTEX = "PDF_ARCHIVAL_ENCRYPTION_MUTEX";
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
    /** PRR-D01a / DEF-LRP-D6-001: circuit open / bulkhead full / unknown resilience failure. */
    public static final String GENERATION_SERVICE_UNAVAILABLE = "GENERATION_SERVICE_UNAVAILABLE";
    /** PRR-D01a: resilience / mapped generation timeout. */
    public static final String GENERATION_TIMEOUT = "GENERATION_TIMEOUT";
    /** LR-A6 / ADR-0043: assembled DOCX failed OOXML well-formedness validation. */
    public static final String OOXML_VALIDATION_FAILED = "OOXML_VALIDATION_FAILED";
    /** LR-C7: collaboration notification mark-read fail-closed (invisible / missing). */
    public static final String WORK_ITEM_NOT_FOUND = "WORK_ITEM_NOT_FOUND";
    /** CE-G01: decision actor equals the most recent submitter and no exception intervention applies. */
    public static final String SELF_APPROVAL_FORBIDDEN = "SELF_APPROVAL_FORBIDDEN";
    /** CE-G01: exception intervention requested by a role that is not GROUP_ADMIN / GLOBAL_ADMIN. */
    public static final String EXCEPTION_INTERVENTION_NOT_ALLOWED = "EXCEPTION_INTERVENTION_NOT_ALLOWED";
    /** CE-G01: exception intervention missing a non-blank exception reason. */
    public static final String EXCEPTION_REASON_REQUIRED = "EXCEPTION_REASON_REQUIRED";
    /** CE-G01: exception intervention missing secondary confirmation. */
    public static final String EXCEPTION_SECONDARY_CONFIRM_REQUIRED = "EXCEPTION_SECONDARY_CONFIRM_REQUIRED";
    /** CE-K03: whitelist compute DSL evaluation failed (fail-closed). */
    public static final String VARIABLE_COMPUTE_FAILED = "VARIABLE_COMPUTE_FAILED";
    /**
     * IBL-A1: aggregated VariableSchema validation failure (required / type / enum / unknown field).
     * HTTP 422; details in {@code fieldErrors[]}.
     */
    public static final String VARIABLE_VALIDATION_FAILED = "VARIABLE_VALIDATION_FAILED";

    /** CE-C04 / ADR-0009: credential past persisted expires_at. */
    public static final String API_CREDENTIAL_EXPIRED = "API_CREDENTIAL_EXPIRED";
    /** CE-C04 / ADR-0009: credential revoked. */
    public static final String API_CREDENTIAL_REVOKED = "API_CREDENTIAL_REVOKED";

    /** CE-G06: invocation lacks release-bundle fingerprint. */
    public static final String RELEASE_BUNDLE_SNAPSHOT_UNAVAILABLE = "RELEASE_BUNDLE_SNAPSHOT_UNAVAILABLE";
    /** CE-G06: pinned master object hash drift. */
    public static final String RELEASE_BUNDLE_HASH_MISMATCH = "RELEASE_BUNDLE_HASH_MISMATCH";
    /** CE-G06 / CE-K01: pinned master revision unavailable. */
    public static final String PINNED_MASTER_UNAVAILABLE = "PINNED_MASTER_UNAVAILABLE";
    /** CE-E01: import commit blocked by unmet dependency pre-check. */
    public static final String IMPORT_DEPENDENCIES_UNSATISFIED = "IMPORT_DEPENDENCIES_UNSATISFIED";
    /** CE-G06: BATCH_ROOT (and similar) cannot be regenerated. */
    public static final String INVOCATION_KIND_NOT_REGENERABLE = "INVOCATION_KIND_NOT_REGENERABLE";
    /** CE-G06: SPECIMEN watermark application failed. */
    public static final String SPECIMEN_WATERMARK_FAILED = "SPECIMEN_WATERMARK_FAILED";
    /** CE-G06: regenerate outputFormat outside DOCX/PDF allow-list. */
    public static final String OUTPUT_FORMAT_NOT_ALLOWED = "OUTPUT_FORMAT_NOT_ALLOWED";

    /** CE-E02: asset key grammar invalid. */
    public static final String ASSET_LIBRARY_ASSET_KEY_INVALID = "ASSET_LIBRARY_ASSET_KEY_INVALID";
    /** CE-E02: ACTIVE asset key conflict. */
    public static final String ASSET_LIBRARY_ASSET_KEY_CONFLICT = "ASSET_LIBRARY_ASSET_KEY_CONFLICT";
    /** CE-E02: unsupported upload content type. */
    public static final String ASSET_LIBRARY_CONTENT_TYPE_UNSUPPORTED = "ASSET_LIBRARY_CONTENT_TYPE_UNSUPPORTED";
    /** CE-E02: declared content type vs magic mismatch (or empty payload). */
    public static final String ASSET_LIBRARY_CONTENT_TYPE_MISMATCH = "ASSET_LIBRARY_CONTENT_TYPE_MISMATCH";
    /** CE-E02: application-layer payload exceeds 5 MiB. */
    public static final String ASSET_LIBRARY_PAYLOAD_TOO_LARGE = "ASSET_LIBRARY_PAYLOAD_TOO_LARGE";
    /** CE-E02: catalog key not found for authorized admin. */
    public static final String ASSET_LIBRARY_ASSET_NOT_FOUND = "ASSET_LIBRARY_ASSET_NOT_FOUND";

    /** CE-E03: full-library export produced no INCLUDED templates. */
    public static final String LIBRARY_EXPORT_EMPTY = "LIBRARY_EXPORT_EMPTY";
    /** CE-E03: templateIds length or eligible candidates exceed 500. */
    public static final String LIBRARY_EXPORT_LIMIT_EXCEEDED = "LIBRARY_EXPORT_LIMIT_EXCEEDED";
    /** CE-E03: full-library ZIP assembly or catalog serialization failed. */
    public static final String LIBRARY_EXPORT_FAILED = "LIBRARY_EXPORT_FAILED";

    /** CE-G04: legal hold not found. */
    public static final String LEGAL_HOLD_NOT_FOUND = "LEGAL_HOLD_NOT_FOUND";
    /** CE-G04: release requested on already RELEASED hold. */
    public static final String LEGAL_HOLD_ALREADY_RELEASED = "LEGAL_HOLD_ALREADY_RELEASED";

    /** IBL-E1: same group + localeVariantFamilyId + locale already exists. */
    public static final String LOCALE_VARIANT_CONFLICT = "LOCALE_VARIANT_CONFLICT";
    /** IBL-E1: runtime context.locale incompatible with pinned template locale. */
    public static final String TEMPLATE_LOCALE_MISMATCH = "TEMPLATE_LOCALE_MISMATCH";

    /** IBL-E2 / ADR-0063: illegal Composition Inclusion Rule set on management PUT. */
    public static final String COMPOSITION_INCLUSION_RULE_INVALID = "COMPOSITION_INCLUSION_RULE_INVALID";
    /** IBL-E2 / ADR-0063: required inclusion rule unsatisfied at generate. */
    public static final String COMPOSITION_INCLUSION_UNSATISFIED = "COMPOSITION_INCLUSION_UNSATISFIED";
    /** IBL-E2 / ADR-0063: INCLUDE'd CM jurisdiction differs from context.jurisdiction. */
    public static final String CONTENT_MODULE_JURISDICTION_MISMATCH = "CONTENT_MODULE_JURISDICTION_MISMATCH";

    /** IBL-E3 / ADR-0064: approvalMatrixMode write outside DRAFT / APPROVAL+PENDING_SUBMIT. */
    public static final String APPROVAL_MATRIX_MODE_LOCKED = "APPROVAL_MATRIX_MODE_LOCKED";
    /** IBL-E3 / ADR-0064: decision actor lacks the role for the current approval stage. */
    public static final String APPROVAL_STAGE_ROLE_FORBIDDEN = "APPROVAL_STAGE_ROLE_FORBIDDEN";
    /** IBL-E3 / ADR-0064: requested approvalStage does not match current sub-state. */
    public static final String APPROVAL_STAGE_MISMATCH = "APPROVAL_STAGE_MISMATCH";

    /** IBL-E4 / ADR-0065: legal entity code unknown in group catalog. */
    public static final String LEGAL_ENTITY_UNKNOWN = "LEGAL_ENTITY_UNKNOWN";
    /** IBL-E4 / ADR-0065: legal entity exists but INACTIVE. */
    public static final String LEGAL_ENTITY_INACTIVE = "LEGAL_ENTITY_INACTIVE";
    /** IBL-E4 / ADR-0065: bound / resolved document brand missing or INACTIVE. */
    public static final String DOCUMENT_BRAND_INACTIVE = "DOCUMENT_BRAND_INACTIVE";
    /** IBL-E4 / ADR-0065: resolved brand not in template allow-list. */
    public static final String DOCUMENT_BRAND_NOT_ALLOWED = "DOCUMENT_BRAND_NOT_ALLOWED";
    /** IBL-E4 / ADR-0065: document brand code unknown in group catalog. */
    public static final String DOCUMENT_BRAND_UNKNOWN = "DOCUMENT_BRAND_UNKNOWN";
    /** IBL-E4 / ADR-0065: document brand / legal entity code format invalid. */
    public static final String DOCUMENT_BRAND_CODE_INVALID = "DOCUMENT_BRAND_CODE_INVALID";

    private ApiErrorCodes() {
    }
}
