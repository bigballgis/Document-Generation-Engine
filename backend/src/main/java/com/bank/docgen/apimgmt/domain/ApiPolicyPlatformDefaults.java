package com.bank.docgen.apimgmt.domain;

import java.util.List;

/**
 * Single source of truth for platform-wide API policy defaults used when materializing
 * skeleton policies and seeding new rows.
 */
public final class ApiPolicyPlatformDefaults {

    public static final List<String> OUTPUT_FORMATS = List.of("DOCX", "PDF");
    public static final List<String> OUTPUT_MODES = List.of("SYNC_STREAM", "SYNC_DOWNLOAD_URL", "ASYNC_TASK");
    public static final boolean BATCH_ENABLED = true;
    public static final int BATCH_SYNC_MAX_ITEMS = 100;
    public static final int BATCH_ASYNC_MAX_ITEMS = 10000;
    public static final boolean DOCX_ENCRYPTION_ENABLED = false;
    public static final boolean PDF_ENCRYPTION_ENABLED = false;
    public static final boolean SAVE_GENERATED_DOCUMENTS = true;
    public static final int INVOCATION_RECORD_RETENTION_DAYS = 90;
    public static final int DOCUMENT_RETENTION_DAYS = 30;
    public static final String ALLOWED_AD_GROUPS_JSON = "[]";
    public static final String OUTPUT_FORMATS_JSON = "[\"DOCX\",\"PDF\"]";
    public static final String OUTPUT_MODES_JSON = "[\"SYNC_STREAM\",\"SYNC_DOWNLOAD_URL\",\"ASYNC_TASK\"]";

    private ApiPolicyPlatformDefaults() {
    }
}
