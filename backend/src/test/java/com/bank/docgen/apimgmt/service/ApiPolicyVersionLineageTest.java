package com.bank.docgen.apimgmt.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.when;

import com.bank.docgen.apimgmt.api.UpsertApiPolicyRequest;
import com.bank.docgen.apimgmt.mapping.ApiPolicyViewMapperFactory;
import com.bank.docgen.apimgmt.persistence.ApiCredentialRepository;
import com.bank.docgen.apimgmt.persistence.ApiPolicyEntity;
import com.bank.docgen.apimgmt.persistence.ApiPolicyRepository;
import com.bank.docgen.apimgmt.persistence.ApiPolicyVersionEntity;
import com.bank.docgen.apimgmt.persistence.ApiPolicyVersionRepository;
import com.bank.docgen.audit.service.ManagementAuditRecorder;
import com.bank.docgen.authorization.management.domain.AuthSource;
import com.bank.docgen.authorization.management.service.GroupAccessService;
import com.bank.docgen.runtime.service.ContractAssemblyService;
import com.bank.docgen.sharedkernel.security.ManagementSessionClaims;
import com.bank.docgen.sharedkernel.security.PasswordHashService;
import com.bank.docgen.template.domain.TemplateLifecycleStatus;
import com.bank.docgen.template.persistence.TemplateEntity;
import com.bank.docgen.template.persistence.TemplateVersionRepository;
import com.bank.docgen.template.service.TemplateService;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ApiPolicyVersionLineageTest {

    @Mock
    private TemplateService templateService;
    @Mock
    private ApiPolicyRepository apiPolicyRepository;
    @Mock
    private ApiCredentialRepository apiCredentialRepository;
    @Mock
    private PasswordHashService passwordHashService;
    @Mock
    private ManagementAuditRecorder managementAuditRecorder;
    @Mock
    private ContractAssemblyService contractAssemblyService;
    @Mock
    private ApiPolicyVersionRepository apiPolicyVersionRepository;
    @Mock
    private TemplateVersionRepository templateVersionRepository;
    @Mock
    private ApiPolicyImpactPreviewService apiPolicyImpactPreviewService;

    private final ObjectMapper objectMapper = new ObjectMapper();
    private final TemplateAdGroupAuthorizationCache authorizationCache = new TemplateAdGroupAuthorizationCache();
    private final List<ApiPolicyVersionEntity> versionHistory = new ArrayList<>();

    private ApiManagementService service;
    private ManagementSessionClaims groupAdmin;
    private UUID templateId;
    private TemplateEntity template;

    @BeforeEach
    void setUp() {
        ApiPolicyVersionSnapshotService snapshotService =
                new ApiPolicyVersionSnapshotService(apiPolicyVersionRepository, objectMapper);
        service = new ApiManagementService(
                templateService,
                apiPolicyRepository,
                apiCredentialRepository,
                new GroupAccessService(),
                passwordHashService,
                managementAuditRecorder,
                contractAssemblyService,
                objectMapper,
                snapshotService,
                templateVersionRepository,
                authorizationCache,
                apiPolicyImpactPreviewService,
                ApiPolicyViewMapperFactory.create(objectMapper)
        );
        groupAdmin = session(List.of("GROUP_ADMIN"));
        templateId = UUID.randomUUID();
        template = publishedTemplate(templateId);

        when(templateService.requireReadableTemplate(templateId, groupAdmin)).thenReturn(template);
        lenient().when(apiPolicyRepository.save(any(ApiPolicyEntity.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
        when(apiPolicyVersionRepository.save(any(ApiPolicyVersionEntity.class))).thenAnswer(invocation -> {
            ApiPolicyVersionEntity saved = invocation.getArgument(0);
            versionHistory.add(saved);
            return saved;
        });
    }

    @Test
    void save_createsHistoryRow_withIncrementedVersion() throws Exception {
        ApiPolicyEntity existing = existingPolicy(templateId, 2);
        when(apiPolicyRepository.findByTemplateId(templateId)).thenReturn(Optional.of(existing));

        service.upsertPolicy(
                templateId,
                new UpsertApiPolicyRequest(
                        List.of("RETAIL_API"),
                        "1.0.0",
                        List.of("DOCX", "PDF"),
                        List.of("SYNC_STREAM"),
                        false,
                        10,
                        false,
                        false
                ),
                groupAdmin
        );

        assertThat(existing.getPolicyVersion()).isEqualTo(3);
        assertThat(versionHistory).hasSize(1);
        ApiPolicyVersionEntity snapshot = versionHistory.getFirst();
        assertThat(snapshot.getPolicyVersion()).isEqualTo(3);
        assertThat(snapshot.getTemplateId()).isEqualTo(templateId);
        assertThat(readStringList(snapshot.getChangedAreasJson())).contains("OUTPUT_POLICY");
        JsonNode config = objectMapper.readTree(snapshot.getConfigSnapshotJson());
        assertThat(config.get("outputFormats").toString()).contains("PDF");
        assertThat(snapshot.getUpdatedBy()).isEqualTo("10000002");
        assertThat(snapshot.getUpdatedAt()).isNotNull();
    }

    @Test
    void multipleSaves_produceOrderedHistory() {
        AtomicReference<ApiPolicyEntity> head = new AtomicReference<>();
        when(apiPolicyRepository.findByTemplateId(templateId)).thenAnswer(invocation -> Optional.ofNullable(head.get()));
        when(apiPolicyRepository.save(any(ApiPolicyEntity.class))).thenAnswer(invocation -> {
            ApiPolicyEntity saved = invocation.getArgument(0);
            head.set(saved);
            return saved;
        });

        service.upsertPolicy(
                templateId,
                new UpsertApiPolicyRequest(
                        List.of("RETAIL_API"),
                        "1.0.0",
                        List.of("DOCX"),
                        List.of("SYNC_STREAM"),
                        false,
                        10,
                        false,
                        false
                ),
                groupAdmin
        );
        service.upsertPolicy(
                templateId,
                new UpsertApiPolicyRequest(
                        List.of("RETAIL_API"),
                        "1.0.0",
                        List.of("DOCX", "PDF"),
                        List.of("SYNC_STREAM"),
                        false,
                        10,
                        false,
                        false
                ),
                groupAdmin
        );
        service.upsertPolicy(
                templateId,
                new UpsertApiPolicyRequest(
                        List.of("RETAIL_API"),
                        "1.0.0",
                        List.of("DOCX", "PDF", "HTML"),
                        List.of("SYNC_STREAM"),
                        false,
                        10,
                        false,
                        false
                ),
                groupAdmin
        );

        assertThat(versionHistory).hasSize(3);
        assertThat(versionHistory.stream().map(ApiPolicyVersionEntity::getPolicyVersion).toList())
                .containsExactly(1, 2, 3);
    }

    @Test
    void historySnapshot_isImmutable_afterNextSave() throws Exception {
        ApiPolicyEntity existing = existingPolicy(templateId, 1);
        when(apiPolicyRepository.findByTemplateId(templateId)).thenReturn(Optional.of(existing));

        service.upsertPolicy(
                templateId,
                new UpsertApiPolicyRequest(
                        List.of("RETAIL_API"),
                        "1.0.0",
                        List.of("DOCX", "PDF"),
                        List.of("SYNC_STREAM"),
                        false,
                        10,
                        false,
                        false
                ),
                groupAdmin
        );
        String firstSnapshotJson = versionHistory.getFirst().getConfigSnapshotJson();
        JsonNode firstFormats = objectMapper.readTree(firstSnapshotJson).get("outputFormats");
        assertThat(firstFormats.toString()).contains("PDF");
        assertThat(firstFormats.toString()).doesNotContain("HTML");

        service.upsertPolicy(
                templateId,
                new UpsertApiPolicyRequest(
                        List.of("RETAIL_API"),
                        "1.0.0",
                        List.of("DOCX", "PDF", "HTML"),
                        List.of("SYNC_STREAM"),
                        false,
                        10,
                        false,
                        false
                ),
                groupAdmin
        );

        assertThat(versionHistory).hasSize(2);
        JsonNode stillFirstFormats = objectMapper.readTree(versionHistory.getFirst().getConfigSnapshotJson())
                .get("outputFormats");
        assertThat(stillFirstFormats.toString()).isEqualTo(firstFormats.toString());
        assertThat(stillFirstFormats.toString()).doesNotContain("HTML");
    }

    private List<String> readStringList(String json) throws Exception {
        return objectMapper.readValue(json, new com.fasterxml.jackson.core.type.TypeReference<List<String>>() {
        });
    }

    private ManagementSessionClaims session(List<String> roles) {
        return new ManagementSessionClaims(
                "10000002",
                "Admin",
                "admin@example.com",
                AuthSource.LOCAL,
                roles,
                List.of("RETAIL"),
                "route.template-authoring-home",
                List.of("route.template-authoring-home"),
                Instant.now().plusSeconds(3600)
        );
    }

    private TemplateEntity publishedTemplate(UUID id) {
        TemplateEntity entity = new TemplateEntity(
                id,
                "TPL-001",
                "RETAIL",
                "Sample",
                null,
                UUID.randomUUID(),
                "10000001"
        );
        entity.setLifecycleStatus(TemplateLifecycleStatus.PUBLISHED);
        entity.setReleaseVersion("1.0.0");
        return entity;
    }

    private ApiPolicyEntity existingPolicy(UUID templateId, int version) {
        ApiPolicyEntity policy = new ApiPolicyEntity(UUID.randomUUID(), templateId, "[\"RETAIL_API\"]", "10000001");
        policy.replaceConfiguration(
                "[\"RETAIL_API\"]",
                "1.0.0",
                "[\"DOCX\"]",
                "[\"SYNC_STREAM\"]",
                false,
                10,
                false,
                false,
                "10000001"
        );
        while (policy.getPolicyVersion() < version) {
            policy.update(
                    "[\"RETAIL_API\"]",
                    "1.0.0",
                    "[\"DOCX\"]",
                    "[\"SYNC_STREAM\"]",
                    false,
                    10,
                    false,
                    false,
                    "10000001"
            );
        }
        return policy;
    }
}
