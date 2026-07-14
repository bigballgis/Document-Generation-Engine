package com.bank.docgen.template.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.when;

import com.bank.docgen.apimgmt.persistence.ApiPolicyEntity;
import com.bank.docgen.apimgmt.persistence.ApiPolicyRepository;
import com.bank.docgen.authorization.management.domain.AuthSource;
import com.bank.docgen.contentmodule.persistence.ContentModuleVersionEntity;
import com.bank.docgen.contentmodule.persistence.ContentModuleVersionRepository;
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
import com.bank.docgen.template.persistence.TemplateContentModuleReferenceEntity;
import com.bank.docgen.template.persistence.TemplateContentModuleReferenceRepository;
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
    @Mock
    private TemplateContentModuleReferenceRepository contentModuleReferenceRepository;
    @Mock
    private ContentModuleVersionRepository contentModuleVersionRepository;

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
                contentModuleReferenceRepository,
                contentModuleVersionRepository,
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
    }

    @Test
    void diff_detectsAddedVariable() {
        when(templateService.requireReadableTemplate(templateId, author)).thenReturn(template);
        TemplateVersionEntity candidate = candidateVersion(candidateVersionId);
        TemplateVersionEntity baseline = publishedVersion(baselineVersionId, "1.0.0");
        stubEmptyContentModuleRefs(candidateVersionId, baselineVersionId);
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
        when(templateService.requireReadableTemplate(templateId, author)).thenReturn(template);
        TemplateVersionEntity candidate = candidateVersion(candidateVersionId);
        TemplateVersionEntity baseline = publishedVersion(baselineVersionId, "1.0.0");
        stubEmptyContentModuleRefs(candidateVersionId, baselineVersionId);
        when(templateVersionRepository.findByTemplateIdOrderByDevVersionNumberDesc(templateId))
                .thenReturn(List.of(candidate, baseline));
        when(variableSchemaRepository.findByTemplateVersionIdOrderByVariableKeyAsc(candidateVersionId))
                .thenReturn(List.of());
        when(variableSchemaRepository.findByTemplateVersionIdOrderByVariableKeyAsc(baselineVersionId))
                .thenReturn(List.of());
        when(anchorBindingRepository.findByTemplateVersionIdOrderByAnchorIdAsc(candidateVersionId))
                .thenReturn(List.of(binding(candidateVersionId, "HEADER", paragraphJson("new"))));
        when(anchorBindingRepository.findByTemplateVersionIdOrderByAnchorIdAsc(baselineVersionId))
                .thenReturn(List.of(binding(baselineVersionId, "HEADER", paragraphJson("old"))));
        when(apiPolicyRepository.findByTemplateId(templateId))
                .thenReturn(Optional.of(new ApiPolicyEntity(UUID.randomUUID(), templateId, "[]", "10000003")));

        ChangeDiffView diff = service.compute(templateId, author);

        ChangeDiffDimensionView anchors = diff.dimensions().stream()
                .filter(dimension -> dimension.dimension() == ChangeDiffDimension.ANCHORS)
                .findFirst()
                .orElseThrow();
        assertThat(anchors.modified()).hasSize(1);
        assertThat(anchors.modified().get(0).key()).isEqualTo("HEADER");
        assertThat(diff.humanReadableEntries()).isNotEmpty();
        assertThat(diff.humanReadableEntries().stream().map(entry -> entry.summary()).toList().toString())
                .contains("old")
                .contains("new");
    }

    @Test
    void diff_noChange_returnsEmptyDiff() {
        when(templateService.requireReadableTemplate(templateId, author)).thenReturn(template);
        TemplateVersionEntity candidate = candidateVersion(candidateVersionId);
        TemplateVersionEntity baseline = publishedVersion(baselineVersionId, "1.0.0");
        stubEmptyContentModuleRefs(candidateVersionId, baselineVersionId);
        when(templateVersionRepository.findByTemplateIdOrderByDevVersionNumberDesc(templateId))
                .thenReturn(List.of(candidate, baseline));
        when(variableSchemaRepository.findByTemplateVersionIdOrderByVariableKeyAsc(candidateVersionId))
                .thenReturn(List.of(variable("field", true)));
        when(variableSchemaRepository.findByTemplateVersionIdOrderByVariableKeyAsc(baselineVersionId))
                .thenReturn(List.of(variable("field", true)));
        AnchorBindingEntity sharedBinding = binding(candidateVersionId, "HEADER", paragraphJson("same"));
        when(anchorBindingRepository.findByTemplateVersionIdOrderByAnchorIdAsc(candidateVersionId))
                .thenReturn(List.of(sharedBinding));
        when(anchorBindingRepository.findByTemplateVersionIdOrderByAnchorIdAsc(baselineVersionId))
                .thenReturn(List.of(binding(baselineVersionId, "HEADER", paragraphJson("same"))));
        when(apiPolicyRepository.findByTemplateId(templateId))
                .thenReturn(Optional.of(new ApiPolicyEntity(UUID.randomUUID(), templateId, "[]", "10000003")));

        ChangeDiffView diff = service.compute(templateId, author);

        assertThat(diff.hasChanges()).isFalse();
        assertThat(diff.totalChangeCount()).isZero();
        assertThat(diff.humanReadableEntries()).isEmpty();
    }

    @Test
    void computeBetween_detectsSentenceLevelContentChange() {
        TemplateVersionEntity baseline = publishedVersion(baselineVersionId, "1.0.0");
        TemplateVersionEntity candidate = candidateVersion(candidateVersionId);
        stubEmptyContentModuleRefs(candidateVersionId, baselineVersionId);
        when(variableSchemaRepository.findByTemplateVersionIdOrderByVariableKeyAsc(candidateVersionId))
                .thenReturn(List.of());
        when(variableSchemaRepository.findByTemplateVersionIdOrderByVariableKeyAsc(baselineVersionId))
                .thenReturn(List.of());
        when(anchorBindingRepository.findByTemplateVersionIdOrderByAnchorIdAsc(candidateVersionId))
                .thenReturn(List.of(binding(candidateVersionId, "BODY", paragraphJson("贷款利率 5.2%"))));
        when(anchorBindingRepository.findByTemplateVersionIdOrderByAnchorIdAsc(baselineVersionId))
                .thenReturn(List.of(binding(baselineVersionId, "BODY", paragraphJson("贷款利率 4.9%"))));
        when(apiPolicyRepository.findByTemplateId(templateId)).thenReturn(Optional.empty());

        ChangeDiffView diff = service.computeBetween(templateId, baseline, candidate);

        assertThat(diff.hasChanges()).isTrue();
        assertThat(diff.humanReadableEntries()).isNotEmpty();
        assertThat(diff.humanReadableEntries().getFirst().summary())
                .contains("贷款利率 4.9%")
                .contains("贷款利率 5.2%")
                .doesNotContain("masterCatalogVersion changed");
        ChangeDiffDimensionView content = diff.dimensions().stream()
                .filter(dimension -> dimension.dimension() == ChangeDiffDimension.CONTENT)
                .findFirst()
                .orElseThrow();
        assertThat(content.modified()).isNotEmpty();
        assertThat(content.modified().getFirst().summary()).contains("贷款利率");
    }

    @Test
    void computeBetween_detectsClauseReferenceVersionUpgrade() {
        TemplateVersionEntity baseline = publishedVersion(baselineVersionId, "1.0.0");
        TemplateVersionEntity candidate = candidateVersion(candidateVersionId);
        UUID moduleVersionV1 = UUID.randomUUID();
        UUID moduleVersionV2 = UUID.randomUUID();
        when(contentModuleReferenceRepository.findByTemplateVersionIdOrderByReferenceKeyAsc(baselineVersionId))
                .thenReturn(List.of(new TemplateContentModuleReferenceEntity(
                        UUID.randomUUID(), baselineVersionId, "CLAUSE-1", moduleVersionV1)));
        when(contentModuleReferenceRepository.findByTemplateVersionIdOrderByReferenceKeyAsc(candidateVersionId))
                .thenReturn(List.of(new TemplateContentModuleReferenceEntity(
                        UUID.randomUUID(), candidateVersionId, "CLAUSE-1", moduleVersionV2)));
        when(contentModuleVersionRepository.findById(moduleVersionV1))
                .thenReturn(Optional.of(moduleVersion(moduleVersionV1, "1.0.0")));
        when(contentModuleVersionRepository.findById(moduleVersionV2))
                .thenReturn(Optional.of(moduleVersion(moduleVersionV2, "1.1.0")));
        when(variableSchemaRepository.findByTemplateVersionIdOrderByVariableKeyAsc(candidateVersionId))
                .thenReturn(List.of());
        when(variableSchemaRepository.findByTemplateVersionIdOrderByVariableKeyAsc(baselineVersionId))
                .thenReturn(List.of());
        when(anchorBindingRepository.findByTemplateVersionIdOrderByAnchorIdAsc(candidateVersionId))
                .thenReturn(List.of());
        when(anchorBindingRepository.findByTemplateVersionIdOrderByAnchorIdAsc(baselineVersionId))
                .thenReturn(List.of());
        when(apiPolicyRepository.findByTemplateId(templateId)).thenReturn(Optional.empty());

        ChangeDiffView diff = service.computeBetween(templateId, baseline, candidate);

        assertThat(diff.humanReadableEntries()).anySatisfy(entry ->
                assertThat(entry.summary()).contains("CLAUSE-1").contains("1.0.0").contains("1.1.0"));
    }

    @Test
    void computeBetweenReleases_returnsSemanticDiffForPublishedPair() {
        when(templateService.requireReadableTemplate(templateId, author)).thenReturn(template);
        TemplateVersionEntity versionA = publishedVersion(baselineVersionId, "1.0.0");
        TemplateVersionEntity versionB = publishedVersion(candidateVersionId, "1.1.0");
        stubEmptyContentModuleRefs(candidateVersionId, baselineVersionId);
        when(templateVersionRepository.findByTemplateIdAndReleaseVersion(templateId, "1.0.0"))
                .thenReturn(Optional.of(versionA));
        when(templateVersionRepository.findByTemplateIdAndReleaseVersion(templateId, "1.1.0"))
                .thenReturn(Optional.of(versionB));
        when(variableSchemaRepository.findByTemplateVersionIdOrderByVariableKeyAsc(candidateVersionId))
                .thenReturn(List.of());
        when(variableSchemaRepository.findByTemplateVersionIdOrderByVariableKeyAsc(baselineVersionId))
                .thenReturn(List.of());
        when(anchorBindingRepository.findByTemplateVersionIdOrderByAnchorIdAsc(candidateVersionId))
                .thenReturn(List.of(binding(candidateVersionId, "BODY", paragraphJson("贷款利率 5.2%"))));
        when(anchorBindingRepository.findByTemplateVersionIdOrderByAnchorIdAsc(baselineVersionId))
                .thenReturn(List.of(binding(baselineVersionId, "BODY", paragraphJson("贷款利率 4.9%"))));
        when(apiPolicyRepository.findByTemplateId(templateId)).thenReturn(Optional.empty());

        ChangeDiffView diff = service.computeBetweenReleases(templateId, "1.0.0", "1.1.0", author);

        assertThat(diff.baselineReleaseVersion()).isEqualTo("1.0.0");
        assertThat(diff.candidateReleaseVersion()).isEqualTo("1.1.0");
        assertThat(diff.humanReadableEntries()).isNotEmpty();
    }

    @Test
    void computeBetweenReleases_sameVersion_returnsEmptyDiff() {
        when(templateService.requireReadableTemplate(templateId, author)).thenReturn(template);
        TemplateVersionEntity version = publishedVersion(baselineVersionId, "1.0.0");
        when(templateVersionRepository.findByTemplateIdAndReleaseVersion(templateId, "1.0.0"))
                .thenReturn(Optional.of(version));

        ChangeDiffView diff = service.computeBetweenReleases(templateId, "1.0.0", "1.0.0", author);

        assertThat(diff.hasChanges()).isFalse();
        assertThat(diff.totalChangeCount()).isZero();
    }

    @Test
    void computeBetweenReleases_missingRelease_throwsNotFound() {
        when(templateService.requireReadableTemplate(templateId, author)).thenReturn(template);
        when(templateVersionRepository.findByTemplateIdAndReleaseVersion(templateId, "9.9.9"))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.computeBetweenReleases(templateId, "9.9.9", "1.0.0", author))
                .isInstanceOf(TemplateNotFoundException.class);
    }

    @Test
    void computeBetweenReleases_denied_whenTemplateNotReadable() {
        doThrow(new TemplateAccessDeniedException())
                .when(templateService).requireReadableTemplate(templateId, author);

        assertThatThrownBy(() -> service.computeBetweenReleases(templateId, "1.0.0", "1.1.0", author))
                .isInstanceOf(TemplateAccessDeniedException.class);
    }

    private void stubEmptyContentModuleRefs(UUID... versionIds) {
        for (UUID versionId : versionIds) {
            when(contentModuleReferenceRepository.findByTemplateVersionIdOrderByReferenceKeyAsc(versionId))
                    .thenReturn(List.of());
        }
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

    private static String paragraphJson(String text) {
        return "{\"nodes\":[{\"type\":\"paragraph\",\"children\":[{\"type\":\"textRun\",\"value\":\""
                + text + "\"}]}]}";
    }

    private ContentModuleVersionEntity moduleVersion(UUID id, String semanticVersion) {
        return new ContentModuleVersionEntity(
                id,
                UUID.randomUUID(),
                semanticVersion,
                "{\"nodes\":[]}",
                "clause",
                "10000003"
        );
    }
}
