package com.bank.docgen.audit.service;

public final class ManagementAuditEventTypes {

    public static final String API_POLICY_UPDATED = "API_POLICY_UPDATED";
    public static final String API_CREDENTIAL_CREATED = "API_CREDENTIAL_CREATED";
    public static final String API_CREDENTIAL_ROTATED = "API_CREDENTIAL_ROTATED";
    public static final String API_CREDENTIAL_REVOKED = "API_CREDENTIAL_REVOKED";
    public static final String USER_CREATED = "USER_CREATED";
    public static final String USER_UPDATED = "USER_UPDATED";
    public static final String USER_DISABLED = "USER_DISABLED";
    public static final String USER_ENABLED = "USER_ENABLED";
    public static final String USER_PASSWORD_RESET = "USER_PASSWORD_RESET";
    public static final String USER_DELETED = "USER_DELETED";
    public static final String GROUP_CREATED = "GROUP_CREATED";
    public static final String GROUP_UPDATED = "GROUP_UPDATED";
    public static final String GROUP_DISABLED = "GROUP_DISABLED";
    public static final String GROUP_ENABLED = "GROUP_ENABLED";
    public static final String IDENTITY_ESCALATION_DENIED = "IDENTITY_ESCALATION_DENIED";
    public static final String RISK_PROMPT_CONFIG_UPDATED = "RISK_PROMPT_CONFIG_UPDATED";
    public static final String COLLABORATION_TIMEOUT_CONFIG_UPDATED = "COLLABORATION_TIMEOUT_CONFIG_UPDATED";
    public static final String COLLABORATION_TIMEOUT_ESCALATION = "COLLABORATION_TIMEOUT_ESCALATION";
    public static final String COLLABORATION_WORK_ITEM_CREATED = "COLLABORATION_WORK_ITEM_CREATED";
    public static final String COLLABORATION_WORK_ITEM_RESOLVED = "COLLABORATION_WORK_ITEM_RESOLVED";
    public static final String COLLABORATION_ESCALATION_ACTOR_USERNAME = "00000000";
    public static final String COLLABORATION_ESCALATION_ACTOR_SUMMARY = "Collaboration escalation scheduler";
    public static final String CONTENT_MODULE_CREATED = "CONTENT_MODULE_CREATED";
    public static final String CONTENT_MODULE_VERSION_CREATED = "CONTENT_MODULE_VERSION_CREATED";
    public static final String CONTENT_MODULE_VERSION_UPDATED = "CONTENT_MODULE_VERSION_UPDATED";
    public static final String CONTENT_MODULE_SHARED_GROUP_CODES_UPDATED = "CONTENT_MODULE_SHARED_GROUP_CODES_UPDATED";
    public static final String CONTENT_MODULE_REVIEW_TRANSITION = "CONTENT_MODULE_REVIEW_TRANSITION";
    public static final String CONTENT_MODULE_LIFECYCLE_OPERATION = "CONTENT_MODULE_LIFECYCLE_OPERATION";
    public static final String TEMPLATE_EXPORTED = "TEMPLATE_EXPORTED";
    public static final String TEMPLATE_IMPORTED = "TEMPLATE_IMPORTED";
    public static final String TEMPLATE_IMPORT_DRY_RUN = "TEMPLATE_IMPORT_DRY_RUN";
    public static final String TEMPLATE_TEST_DATA_PII_EXPLICIT_CONFIRM = "TEMPLATE_TEST_DATA_PII_EXPLICIT_CONFIRM";
    /** CE-G06: controlled regenerate terminal event (success or failure). */
    public static final String INVOCATION_REGENERATED = "INVOCATION_REGENERATED";
    /** CE-E02: asset library catalog upload. */
    public static final String ASSET_LIBRARY_UPLOAD = "ASSET_LIBRARY_UPLOAD";
    /** CE-E02: asset library catalog disable. */
    public static final String ASSET_LIBRARY_DISABLE = "ASSET_LIBRARY_DISABLE";
    /** CE-E02: DISABLED key re-upload reactivation. */
    public static final String ASSET_LIBRARY_REUPLOAD = "ASSET_LIBRARY_REUPLOAD";
    /** CE-E03: full-library export (including partial success). */
    public static final String LIBRARY_EXPORT = "LIBRARY_EXPORT";
    /** CE-G04: legal hold created. */
    public static final String LEGAL_HOLD_CREATED = "LEGAL_HOLD_CREATED";
    /** CE-G04: legal hold released. */
    public static final String LEGAL_HOLD_RELEASED = "LEGAL_HOLD_RELEASED";

    private ManagementAuditEventTypes() {
    }
}
