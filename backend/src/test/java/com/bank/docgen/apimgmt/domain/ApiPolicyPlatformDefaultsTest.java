package com.bank.docgen.apimgmt.domain;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class ApiPolicyPlatformDefaultsTest {

    @Test
    void platformDefaults_matchBddConvention() {
        assertThat(ApiPolicyPlatformDefaults.OUTPUT_FORMATS).containsExactly("DOCX", "PDF");
        assertThat(ApiPolicyPlatformDefaults.OUTPUT_MODES).containsExactly(
                "SYNC_STREAM", "SYNC_DOWNLOAD_URL", "ASYNC_TASK");
        assertThat(ApiPolicyPlatformDefaults.BATCH_ENABLED).isTrue();
        assertThat(ApiPolicyPlatformDefaults.BATCH_SYNC_MAX_ITEMS).isEqualTo(100);
        assertThat(ApiPolicyPlatformDefaults.BATCH_ASYNC_MAX_ITEMS).isEqualTo(10000);
        assertThat(ApiPolicyPlatformDefaults.DOCX_ENCRYPTION_ENABLED).isFalse();
        assertThat(ApiPolicyPlatformDefaults.PDF_ENCRYPTION_ENABLED).isFalse();
        assertThat(ApiPolicyPlatformDefaults.SAVE_GENERATED_DOCUMENTS).isTrue();
        assertThat(ApiPolicyPlatformDefaults.INVOCATION_RECORD_RETENTION_DAYS).isEqualTo(90);
        assertThat(ApiPolicyPlatformDefaults.DOCUMENT_RETENTION_DAYS).isEqualTo(30);
        assertThat(ApiPolicyPlatformDefaults.ALLOWED_AD_GROUPS_JSON).isEqualTo("[]");
    }
}
