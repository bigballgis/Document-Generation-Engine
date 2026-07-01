package com.bank.docgen.template.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import com.bank.docgen.apimgmt.persistence.ApiPolicyEntity;
import com.bank.docgen.apimgmt.persistence.ApiPolicyRepository;
import com.bank.docgen.authorization.management.domain.AuthSource;
import com.bank.docgen.sharedkernel.security.ManagementSessionClaims;
import com.bank.docgen.template.api.ChangeDiffDimensionView;
import com.bank.docgen.template.api.ChangeDiffView;
import com.bank.docgen.template.domain.AnchorContentType;
import com.bank.docgen.template.domain.BindingValidationStatus;
import com.bank.docgen.template.domain.ChangeDiffDimension;
import com.bank.docgen.template.domain.TemplateLifecycleStatus;
import com.bank.docgen.template.domain.VariableType;
import com.bank.docgen.template.persistence.AnchorBindingEntity;
import com.bank.docgen.template.persistence.AnchorBindingRepository;
import com.bank.docgen.template.persistence.TemplateEntity;
import com.bank.docgen.template.persistence.TemplateVersionEntity;
import com.bank.docgen.template.persistence.TemplateVersionRepository;
import com.bank.docgen.template.persistence.VariableSchemaEntity;
import com.bank.docgen.template.persistence.VariableSchemaRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ChangeDiffServiceTest {

    @Mock
    private TemplateService templateService;
    @Mock
    private TemplateVersionRepository templateVersionRepository;
    @Mock
    private VariableSchemaRepository variableSchemaRepository;
    @Mock
    private AnchorBindingRepository anchorBindingRepository;
    @Mock
    private ApiPolicyRepository apiPolicyRepository;

    private ChangeDiffService service;
    private UUID templateId;
    private UUID candidateVersionId;
    private UUID baselineVersionId;
    private ManagementSessionClaims author;
    private TemplateEntity template;

    @BeforeEach
    void setUp() {
        service = new ChangeDiffService(
                templateService,
                templateVersionRepository,
                variableSchemaRepository,
                anchorBindingRepository,
                apiPolicyRepository,
                new ObjectMapper()
        );
        templateId = UUID.randomUUID();
        candidateVersionId = UUID.randomUUID();
        baselineVersionId = UUID.randomUUID();
        template = new TemplateEntity(
                templateId,
                "TPL-1",
                "RETAIL",
                "Demo",
                null,
                UUID.randomUUID(),
                "10000003"
        );
        author = new ManagementSessionClaims(
                "10000003",
                "Author",
                "author@example.com",
                AuthSource.LOCAL,
                List.of("TEMPLATE_AUTHOR"),
                List.of("RETAIL"),
                "route.template-authoring-home",
                List.of("route.template-authoring-home"),
                Instant.now().plusSeconds(3600)
        );
        when(templateService.requireReadableTemplate(templateId, author)).thenReturn(template);
    }

    @Test
    void diff_detectsAddedVariable() {
        TemplateVersionEntity candidate = candidateVersion(candidateVersionId);
        TemplateVersionEntity baseline = publishedVersion(baselineVersionId, "1.0.0");
        when(templateVersionRepository.findByTemplateIdOrderByDevVersionNumberDesc(templateId))
                .thenReturn(List.of(candidate, baseline));
        when(variableSchemaRepository.findByTemplateVersionIdOrderByVariableKeyAsc(candidateVersionId))
                .thenReturn(List.of(variable("newField", true)));
        when(variableSchemaRepository.findByTemplateVersionIdOrderByVariableKeyAsc(baselineVersionId))
                .thenReturn(List.of(variable("existingField", true)));
        when(anchorBindingRepository.findByTemplateVersionIdOrderByAnchorIdAsc(candidateVersionId))
                .thenReturn(List.of());
        when(anchorBindingRepository.findByTemplateVersionIdOrderByAnchorIdAsc(baselineVersionId))
                .thenReturn(List.of());
        when(apiPolicyRepository.findByTemplateId(templateId))
                .thenReturn(Optional.of(new ApiPolicyEntity(UUID.randomUUID(), templateId, "[]", "10000003")));

        ChangeDiffView diff = service.compute(templateId, author);

        ChangeDiffDimensionView variables = diff.dimensions().stream()
                .filter(dimension -> dimension.dimension() == ChangeDiffDimension.VARIABLES)
                .findFirst()
                .orElseThrow();
        assertThat(variables.added()).containsExactly("newField");
        assertThat(diff.hasChanges()).isTrue();
    }

    @Test
    void diff_detectsAnchorBindingChange() {
        TemplateVersionEntity candidate = candidateVersion(candidateVersionId);
        TemplateVersionEntity baseline = publishedVersion(baselineVersionId, "1.0.0");
        when(templateVersionRepository.findByTemplateIdOrderByDevVersionNumberDesc(templateId))
                .thenReturn(List.of(candidate, baseline));
        when(variableSchemaRepository.findByTemplateVersionIdOrderByVariableKeyAsc(candidateVersionId))
                .thenReturn(List.of());
        when(variableSchemaRepository.findByTemplateVersionIdOrderByVariableKeyAsc(baselineVersionId))
                .thenReturn(List.of());
        when(anchorBindingRepository.findByTemplateVersionIdOrderByAnchorIdAsc(candidateVersionId))
                .thenReturn(List.of(binding(candidateVersionId, "HEADER", "{\"text\":\"new\"}")));
        when(anchorBindingRepository.findByTemplateVersionIdOrderByAnchorIdAsc(baselineVersionId))
                .thenReturn(List.of(binding(baselineVersionId, "HEADER", "{\"text\":\"old\"}")));
        when(apiPolicyRepository.findByTemplateId(templateId))
                .thenReturn(Optional.of(new ApiPolicyEntity(UUID.randomUUID(), templateId, "[]", "10000003")));

        ChangeDiffView diff = service.compute(templateId, author);

        ChangeDiffDimensionView anchors = diff.dimensions().stream()
                .filter(dimension -> dimension.dimension() == ChangeDiffDimension.ANCHORS)
                .findFirst()
                .orElseThrow();
        assertThat(anchors.modified()).hasSize(1);
        assertThat(anchors.modified().get(0).key()).isEqualTo("HEADER");
    }

    @Test
    void diff_noChange_returnsEmptyDiff() {
        TemplateVersionEntity candidate = candidateVersion(candidateVersionId);
        TemplateVersionEntity baseline = publishedVersion(baselineVersionId, "1.0.0");
        when(templateVersionRepository.findByTemplateIdOrderByDevVersionNumberDesc(templateId))
                .thenReturn(List.of(candidate, baseline));
        when(variableSchemaRepository.findByTemplateVersionIdOrderByVariableKeyAsc(candidateVersionId))
                .thenReturn(List.of(variable("field", true)));
        when(variableSchemaRepository.findByTemplateVersionIdOrderByVariableKeyAsc(baselineVersionId))
                .thenReturn(List.of(variable("field", true)));
        AnchorBindingEntity sharedBinding = binding(candidateVersionId, "HEADER", "{\"text\":\"same\"}");
        when(anchorBindingRepository.findByTemplateVersionIdOrderByAnchorIdAsc(candidateVersionId))
                .thenReturn(List.of(sharedBinding));
        when(anchorBindingRepository.findByTemplateVersionIdOrderByAnchorIdAsc(baselineVersionId))
                .thenReturn(List.of(binding(baselineVersionId, "HEADER", "{\"text\":\"same\"}")));
        when(apiPolicyRepository.findByTemplateId(templateId))
                .thenReturn(Optional.of(new ApiPolicyEntity(UUID.randomUUID(), templateId, "[]", "10000003")));

        ChangeDiffView diff = service.compute(templateId, author);

        assertThat(diff.hasChanges()).isFalse();
        assertThat(diff.totalChangeCount()).isZero();
    }

    private TemplateVersionEntity candidateVersion(UUID versionId) {
        TemplateVersionEntity version = new TemplateVersionEntity(versionId, templateId, "10000003");
        version.setRulesJson("[]");
        return version;
    }

    private TemplateVersionEntity publishedVersion(UUID versionId, String releaseVersion) {
        TemplateVersionEntity version = new TemplateVersionEntity(versionId, templateId, "10000003");
        version.setReleaseVersion(releaseVersion);
        version.setLifecycleStatus(TemplateLifecycleStatus.PUBLISHED);
        version.setRulesJson("[]");
        return version;
    }

    private VariableSchemaEntity variable(String key, boolean required) {
        return new VariableSchemaEntity(
                UUID.randomUUID(),
                candidateVersionId,
                key,
                VariableType.TEXT,
                required,
                null,
                null,
                "desc",
                null
        );
    }

    private AnchorBindingEntity binding(UUID versionId, String anchorId, String contentJson) {
        return new AnchorBindingEntity(
                UUID.randomUUID(),
                versionId,
                anchorId,
                AnchorContentType.TEXT,
                contentJson,
                BindingValidationStatus.VALID
        );
    }
}
