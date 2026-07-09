package com.bank.docgen.runtime.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.bank.docgen.apimgmt.persistence.ApiPolicyEntity;
import com.bank.docgen.apimgmt.persistence.ApiPolicyRepository;
import com.bank.docgen.runtime.api.GenerateRequestBody;
import com.bank.docgen.runtime.api.OutputOptionsView;
import com.bank.docgen.runtime.persistence.GenerationIdempotencyEntity;
import com.bank.docgen.runtime.security.RuntimeSessionClaims;
import com.bank.docgen.template.domain.TemplateLifecycleStatus;
import com.bank.docgen.template.persistence.TemplateEntity;
import com.bank.docgen.template.persistence.TemplateVersionEntity;
import com.bank.docgen.template.persistence.TemplateVersionRepository;
import com.bank.docgen.template.service.VersionFidelityWarningService;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class RuntimeGenerationServiceIdempotencyReleaseCacheTest {

    private static final UUID TEMPLATE_ID = UUID.randomUUID();

    @Mock
    private TemplateVersionRepository templateVersionRepository;
    @Mock
    private ApiPolicyRepository apiPolicyRepository;
    @Mock
    private IdempotencyService idempotencyService;
    @Mock
    private EncryptionParameterValidator encryptionParameterValidator;
    @Mock
    private VersionFidelityWarningService versionFidelityWarningService;

    private RuntimeGenerationService service;
    private TemplateEntity template;

    @BeforeEach
    void setUp() {
        service = new RuntimeGenerationService(
                templateVersionRepository,
                apiPolicyRepository,
                mock(com.bank.docgen.apimgmt.persistence.ApiCredentialRepository.class),
                null,
                idempotencyService,
                encryptionParameterValidator,
                mock(ContractAssemblyService.class),
                null,
                new ObjectMapper(),
                versionFidelityWarningService
        );
        template = new TemplateEntity(
                TEMPLATE_ID,
                "TPL-001",
                "RETAIL",
                "Sample",
                null,
                UUID.randomUUID(),
                "10000001"
        );
        template.setLifecycleStatus(TemplateLifecycleStatus.PUBLISHED);
        template.setReleaseVersion("2.0.0");
    }

    @Test
    void generateSync_usesStoredResolvedReleaseVersionWithoutScanningVersions() {
        ApiPolicyEntity policy = policy("2.0.0");
        GenerationIdempotencyEntity stored = idempotencyRecord("hash-old-route", "1.0.0");
        when(apiPolicyRepository.findByTemplateId(TEMPLATE_ID)).thenReturn(Optional.of(policy));
        when(templateVersionRepository.findByTemplateIdAndReleaseVersion(TEMPLATE_ID, "2.0.0"))
                .thenReturn(Optional.of(version("2.0.0")));
        when(idempotencyService.hashRequest(any())).thenReturn("hash-new-route");
        when(idempotencyService.findExisting(eq("idem-route"), eq(TEMPLATE_ID), eq("hash-new-route")))
                .thenThrow(new IdempotencyConflictException("idem-route"));
        when(idempotencyService.findLiveRecord("idem-route", TEMPLATE_ID)).thenReturn(Optional.of(stored));
        lenient().doNothing().when(encryptionParameterValidator).validate(any(), any(), any());

        assertThatThrownBy(() -> service.generateSync(
                template,
                session(),
                null,
                request("idem-route")
        ))
                .isInstanceOf(IdempotencyConflictException.class)
                .satisfies(ex -> {
                    IdempotencyConflictException conflict = (IdempotencyConflictException) ex;
                    assertThat(conflict.conflictType())
                            .isEqualTo(IdempotencyConflictException.DEFAULT_ROUTE_CHANGED);
                    assertThat(conflict.originalResolvedReleaseVersion()).isEqualTo("1.0.0");
                });

        verify(templateVersionRepository, never()).findByTemplateIdOrderByDevVersionNumberDesc(TEMPLATE_ID);
    }

    private GenerationIdempotencyEntity idempotencyRecord(String requestHash, String resolvedReleaseVersion) {
        GenerationIdempotencyEntity entity = new GenerationIdempotencyEntity(
                UUID.randomUUID(),
                "idem-route",
                TEMPLATE_ID,
                requestHash,
                "COMPLETED",
                Instant.now().plusSeconds(3600)
        );
        entity.setResolvedReleaseVersion(resolvedReleaseVersion);
        return entity;
    }

    private ApiPolicyEntity policy(String defaultRoute) {
        ApiPolicyEntity entity = new ApiPolicyEntity(UUID.randomUUID(), TEMPLATE_ID, "[\"grp\"]", "10000001");
        entity.replaceConfiguration(
                "[\"grp\"]",
                defaultRoute,
                "[\"DOCX\"]",
                "[\"SYNC_STREAM\"]",
                false,
                10,
                false,
                false,
                "10000001"
        );
        return entity;
    }

    private TemplateVersionEntity version(String releaseVersion) {
        TemplateVersionEntity version = new TemplateVersionEntity(UUID.randomUUID(), TEMPLATE_ID, "10000001");
        version.setReleaseVersion(releaseVersion);
        version.setLifecycleStatus(TemplateLifecycleStatus.PUBLISHED);
        return version;
    }

    private RuntimeSessionClaims session() {
        return new RuntimeSessionClaims(
                UUID.randomUUID(),
                "CRED-1",
                TEMPLATE_ID,
                "TPL-001",
                "svc-caller",
                List.of("grp")
        );
    }

    private GenerateRequestBody request(String idempotencyKey) {
        return new GenerateRequestBody(
                new OutputOptionsView("DOCX", "SYNC_STREAM"),
                Map.of("customerName", "Alice"),
                new com.bank.docgen.sharedkernel.api.EncryptionOptionsView(false, null, null, List.of()),
                "req-1",
                idempotencyKey
        );
    }
}
