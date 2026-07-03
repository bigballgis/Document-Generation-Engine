package com.bank.docgen.apimgmt.persistence;

import static org.assertj.core.api.Assertions.assertThat;

import com.bank.docgen.apimgmt.domain.ApiPolicyPlatformDefaults;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class ApiPolicyEntitySkeletonTest {

    @Test
    void createSkeleton_appliesPlatformDefaultsWithEmptyDefaultRoute() {
        UUID templateId = UUID.randomUUID();

        ApiPolicyEntity skeleton = ApiPolicyEntity.createSkeleton(templateId, "10000002");

        assertThat(skeleton.getTemplateId()).isEqualTo(templateId);
        assertThat(skeleton.getPolicyVersion()).isEqualTo(1);
        assertThat(skeleton.getDefaultRouteReleaseVersion()).isNull();
        assertThat(skeleton.getAllowedAdGroupsJson()).isEqualTo(ApiPolicyPlatformDefaults.ALLOWED_AD_GROUPS_JSON);
        assertThat(skeleton.getOutputFormatsJson()).isEqualTo(ApiPolicyPlatformDefaults.OUTPUT_FORMATS_JSON);
        assertThat(skeleton.getOutputModesJson()).isEqualTo(ApiPolicyPlatformDefaults.OUTPUT_MODES_JSON);
        assertThat(skeleton.isBatchEnabled()).isEqualTo(ApiPolicyPlatformDefaults.BATCH_ENABLED);
        assertThat(skeleton.getBatchSyncMaxItems()).isEqualTo(ApiPolicyPlatformDefaults.BATCH_SYNC_MAX_ITEMS);
        assertThat(skeleton.getBatchAsyncMaxItems()).isEqualTo(ApiPolicyPlatformDefaults.BATCH_ASYNC_MAX_ITEMS);
        assertThat(skeleton.isDocxEncryptionEnabled()).isFalse();
        assertThat(skeleton.isPdfEncryptionEnabled()).isFalse();
        assertThat(skeleton.isSaveGeneratedDocuments()).isTrue();
        assertThat(skeleton.getInvocationRecordRetentionDays()).isEqualTo(90);
        assertThat(skeleton.getDocumentRetentionDays()).isEqualTo(30);
        assertThat(skeleton.getCreatedBy()).isEqualTo("10000002");
    }

    @Test
    void materializeDefaultRouteOnFirstPublish_setsRouteWithoutVersionBump() {
        ApiPolicyEntity skeleton = ApiPolicyEntity.createSkeleton(UUID.randomUUID(), "10000002");

        skeleton.materializeDefaultRouteOnFirstPublish("1.0.0", "10000003");

        assertThat(skeleton.getDefaultRouteReleaseVersion()).isEqualTo("1.0.0");
        assertThat(skeleton.getPolicyVersion()).isEqualTo(1);
        assertThat(skeleton.getUpdatedBy()).isEqualTo("10000003");
    }

    @Test
    void replaceConfiguration_preservesDefaultRouteWhenNull() {
        ApiPolicyEntity policy = ApiPolicyEntity.createSkeleton(UUID.randomUUID(), "10000002");
        policy.materializeDefaultRouteOnFirstPublish("1.0.0", "10000002");

        policy.replaceConfiguration(
                "[\"GRP\"]",
                null,
                ApiPolicyPlatformDefaults.OUTPUT_FORMATS_JSON,
                ApiPolicyPlatformDefaults.OUTPUT_MODES_JSON,
                true,
                50,
                false,
                false,
                "10000003"
        );

        assertThat(policy.getDefaultRouteReleaseVersion()).isEqualTo("1.0.0");
        assertThat(policy.getAllowedAdGroupsJson()).isEqualTo("[\"GRP\"]");
    }
}
