package com.bank.docgen.apimgmt.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.bank.docgen.apimgmt.domain.ApiPolicyPlatformDefaults;
import com.bank.docgen.apimgmt.persistence.ApiPolicyEntity;
import com.bank.docgen.apimgmt.persistence.ApiPolicyRepository;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ApiPolicyMaterializationServiceTest {

    @Mock
    private ApiPolicyRepository apiPolicyRepository;

    private ApiPolicyMaterializationService service;
    private UUID templateId;

    @BeforeEach
    void setUp() {
        service = new ApiPolicyMaterializationService(apiPolicyRepository);
        templateId = UUID.randomUUID();
    }

    @Test
    void ensureApiPolicySkeleton_createsSkeletonWhenMissing() {
        when(apiPolicyRepository.findByTemplateId(templateId)).thenReturn(Optional.empty());
        when(apiPolicyRepository.save(any(ApiPolicyEntity.class))).thenAnswer(invocation -> invocation.getArgument(0));

        ApiPolicyEntity result = service.ensureApiPolicySkeleton(templateId, "10000002");

        ArgumentCaptor<ApiPolicyEntity> captor = ArgumentCaptor.forClass(ApiPolicyEntity.class);
        verify(apiPolicyRepository).save(captor.capture());
        assertThat(captor.getValue().getTemplateId()).isEqualTo(templateId);
        assertThat(captor.getValue().getDefaultRouteReleaseVersion()).isNull();
        assertThat(captor.getValue().getAllowedAdGroupsJson()).isEqualTo(ApiPolicyPlatformDefaults.ALLOWED_AD_GROUPS_JSON);
        assertThat(result.getTemplateId()).isEqualTo(templateId);
    }

    @Test
    void ensureApiPolicySkeleton_isIdempotentWhenPolicyExists() {
        ApiPolicyEntity existing = ApiPolicyEntity.createSkeleton(templateId, "10000002");
        when(apiPolicyRepository.findByTemplateId(templateId)).thenReturn(Optional.of(existing));

        ApiPolicyEntity result = service.ensureApiPolicySkeleton(templateId, "10000003");

        verify(apiPolicyRepository, never()).save(any());
        assertThat(result).isSameAs(existing);
    }

    @Test
    void ensureApiPolicyOnPublish_setsDefaultRouteOnFirstPublishOnly() {
        when(apiPolicyRepository.findByTemplateId(templateId)).thenReturn(Optional.empty());
        when(apiPolicyRepository.save(any(ApiPolicyEntity.class))).thenAnswer(invocation -> invocation.getArgument(0));

        ApiPolicyEntity firstPublish = service.ensureApiPolicyOnPublish(templateId, "1.0.0", "10000002");
        assertThat(firstPublish.getDefaultRouteReleaseVersion()).isEqualTo("1.0.0");

        when(apiPolicyRepository.findByTemplateId(templateId)).thenReturn(Optional.of(firstPublish));

        ApiPolicyEntity secondPublish = service.ensureApiPolicyOnPublish(templateId, "2.0.0", "10000003");
        assertThat(secondPublish.getDefaultRouteReleaseVersion()).isEqualTo("1.0.0");
    }
}
