package com.bank.docgen.template.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import com.bank.docgen.apimgmt.persistence.ApiPolicyEntity;
import com.bank.docgen.template.api.PublishGateChecklistView;
import com.bank.docgen.template.domain.PublishGateCheckCode;
import com.bank.docgen.template.domain.PublishGatePhase;
import com.bank.docgen.template.persistence.TemplateVersionEntity;
import com.bank.docgen.template.persistence.VariableSchemaEntity;
import com.bank.docgen.template.domain.VariableType;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;

/**
 * Peeled from PublishGateServiceTest (AI-SCALE #169).
 */
class PublishGateServiceCoreTest extends PublishGateServiceTestFixtures {

    @Test
    void publish_withUnresolvedBlocker_isRejected_withChecklist() {
        when(templateService.evaluateBindings(templateId, admin)).thenReturn(blockingBindings());
        when(coverageComputationService.compute(templateId, admin)).thenReturn(greenCoverage());

        PublishGateChecklistView checklist = service.evaluate(templateId, admin);

        assertThat(checklist.ready()).isFalse();
        assertThat(checklist.items().stream()
                .anyMatch(item -> item.checkCode() == PublishGateCheckCode.ANCHOR_INTEGRITY && item.blocker()))
                .isTrue();
        assertThatThrownBy(() -> service.assertReady(templateId, admin))
                .isInstanceOf(TemplateValidationException.class)
                .satisfies(ex -> {
                    TemplateValidationException validation = (TemplateValidationException) ex;
                    assertThat(validation.messageKey()).isEqualTo("api.error.template.publishGateBlocked");
                    assertThat(validation.messageArgs()).contains("ANCHOR_INTEGRITY");
                });
    }
    @Test
    void publish_belowCoverageThreshold_isRejected() {
        when(templateService.evaluateBindings(templateId, admin)).thenReturn(nonBlockingBindings());
        when(coverageComputationService.compute(templateId, admin)).thenReturn(blockedCoverage());

        PublishGateChecklistView checklist = service.evaluate(templateId, admin);

        assertThat(checklist.ready()).isFalse();
        assertThat(checklist.items().stream()
                .anyMatch(item -> item.checkCode() == PublishGateCheckCode.COVERAGE_THRESHOLDS && item.blocker()))
                .isTrue();
        assertThatThrownBy(() -> service.assertReady(templateId, admin))
                .isInstanceOf(TemplateValidationException.class);
    }
    @Test
    void publish_allGreen_succeeds_andRecordsChecklist() {
        when(templateService.evaluateBindings(templateId, admin)).thenReturn(nonBlockingBindings());
        when(coverageComputationService.compute(templateId, admin)).thenReturn(greenCoverage());

        PublishGateChecklistView checklist = service.evaluate(templateId, admin);

        assertThat(checklist.ready()).isTrue();
        assertThat(checklist.blockerCount()).isZero();
        service.assertReady(templateId, admin);
    }
    @Test
    void bdd003_paginationDeltaOver2xBudget_blocksPublish() {
        inFlightVersion.setAuthorWordPageCount(6);
        when(previewEvidencePort.latestSuccessfulPdfPageCount(templateId, versionId))
                .thenReturn(Optional.of(9));
        when(templateService.evaluateBindings(templateId, admin)).thenReturn(nonBlockingBindings());
        when(coverageComputationService.compute(templateId, admin)).thenReturn(greenCoverage());

        PublishGateChecklistView checklist = service.evaluate(templateId, admin);

        assertThat(checklist.ready()).isFalse();
        assertThat(checklist.items().stream()
                .filter(item -> item.checkCode() == PublishGateCheckCode.PAGINATION_DELTA_BUDGET)
                .findFirst())
                .get()
                .satisfies(item -> {
                    assertThat(item.blocker()).isTrue();
                    assertThat(item.ready()).isFalse();
                });
        assertThatThrownBy(() -> service.assertReady(templateId, admin))
                .isInstanceOf(TemplateValidationException.class);
    }
    @Test
    void bdd002_paginationDeltaWarningBand_doesNotBlockPublishGate() {
        inFlightVersion.setAuthorWordPageCount(6);
        when(previewEvidencePort.latestSuccessfulPdfPageCount(templateId, versionId))
                .thenReturn(Optional.of(8));
        when(templateService.evaluateBindings(templateId, admin)).thenReturn(nonBlockingBindings());
        when(coverageComputationService.compute(templateId, admin)).thenReturn(greenCoverage());

        PublishGateChecklistView checklist = service.evaluate(templateId, admin);

        assertThat(checklist.items().stream()
                .filter(item -> item.checkCode() == PublishGateCheckCode.PAGINATION_DELTA_BUDGET)
                .findFirst())
                .get()
                .satisfies(item -> assertThat(item.blocker()).isFalse());
        assertThat(checklist.ready()).isTrue();
    }
    @Test
    void bdd004_missingAuthorWordPageCount_skipsPaginationBudgetBlocker() {
        inFlightVersion.setAuthorWordPageCount(null);
        when(previewEvidencePort.latestSuccessfulPdfPageCount(templateId, versionId))
                .thenReturn(Optional.of(6));
        when(templateService.evaluateBindings(templateId, admin)).thenReturn(nonBlockingBindings());
        when(coverageComputationService.compute(templateId, admin)).thenReturn(greenCoverage());

        PublishGateChecklistView checklist = service.evaluate(templateId, admin);

        assertThat(checklist.items().stream()
                .filter(item -> item.checkCode() == PublishGateCheckCode.PAGINATION_DELTA_BUDGET)
                .findFirst())
                .get()
                .satisfies(item -> assertThat(item.blocker()).isFalse());
        assertThat(checklist.ready()).isTrue();
    }
    @Test
    void publishGate_isEvaluatedLive_notStaticText() {
        when(templateService.evaluateBindings(templateId, admin)).thenReturn(nonBlockingBindings());
        when(coverageComputationService.compute(templateId, admin)).thenReturn(greenCoverage());
        when(previewEvidencePort.latestBatchTestRun(templateId)).thenReturn(Optional.empty());

        PublishGateChecklistView checklist = service.evaluate(templateId, admin);

        assertThat(checklist.items().stream()
                .anyMatch(item -> item.checkCode() == PublishGateCheckCode.TEST_RESULTS
                        && item.summary().contains("noBatchRun")
                        && item.blocker()))
                .isTrue();
    }
    @Test
    void submitForApproval_excludesApprovalSummaryAndApiPolicy() {
        when(templateService.evaluateBindings(templateId, admin)).thenReturn(nonBlockingBindings());
        when(coverageComputationService.compute(templateId, admin)).thenReturn(greenCoverage());
        when(lifecycleRecordRepository.findByTemplateIdOrderByCreatedAtDesc(templateId))
                .thenReturn(List.of());
        when(apiPolicyRepository.findByTemplateId(templateId))
                .thenReturn(Optional.empty());

        PublishGateChecklistView publishChecklist = service.evaluate(templateId, admin);
        PublishGateChecklistView submitChecklist = service.evaluate(
                templateId, admin, PublishGatePhase.SUBMIT_FOR_APPROVAL);

        assertThat(publishChecklist.ready()).isFalse();
        assertThat(submitChecklist.ready()).isTrue();
        assertThat(submitChecklist.items()).noneMatch(item ->
                item.checkCode() == PublishGateCheckCode.APPROVAL_SUMMARY
                        || item.checkCode() == PublishGateCheckCode.API_POLICY);
    }
    @Test
    void submitForApproval_withHardBlocker_rejectedWithSubmitGateMessageKey() {
        when(templateService.evaluateBindings(templateId, admin)).thenReturn(blockingBindings());
        when(coverageComputationService.compute(templateId, admin)).thenReturn(greenCoverage());

        assertThatThrownBy(() -> service.assertReady(templateId, admin, PublishGatePhase.SUBMIT_FOR_APPROVAL))
                .isInstanceOf(TemplateValidationException.class)
                .hasFieldOrPropertyWithValue("messageKey", "api.error.template.submitForApprovalGateBlocked");
    }
    @Test
    void submitForApproval_allGreen_succeeds() {
        when(templateService.evaluateBindings(templateId, admin)).thenReturn(nonBlockingBindings());
        when(coverageComputationService.compute(templateId, admin)).thenReturn(greenCoverage());

        PublishGateChecklistView checklist = service.evaluate(templateId, admin, PublishGatePhase.SUBMIT_FOR_APPROVAL);

        assertThat(checklist.ready()).isTrue();
        service.assertReadyForSubmitForApproval(templateId, admin);
    }
    @Test
    void publish_withSkeletonPolicyAndEmptyAdGroups_isBlocked_fosW7_2() {
        when(templateService.evaluateBindings(templateId, admin)).thenReturn(nonBlockingBindings());
        when(coverageComputationService.compute(templateId, admin)).thenReturn(greenCoverage());
        ApiPolicyEntity skeleton = ApiPolicyEntity.createSkeleton(templateId, "10000002");
        when(apiPolicyRepository.findByTemplateId(templateId)).thenReturn(Optional.of(skeleton));

        PublishGateChecklistView checklist = service.evaluate(templateId, admin);

        assertThat(checklist.items().stream()
                .filter(item -> item.checkCode() == PublishGateCheckCode.API_POLICY)
                .findFirst()
                .orElseThrow())
                .satisfies(item -> {
                    assertThat(item.blocker()).isTrue();
                    assertThat(item.ready()).isFalse();
                    assertThat(item.messageKey()).isEqualTo("api.publishGate.apiPolicy.blocked");
                    assertThat(item.summary()).contains("adGroupsConfigured=false");
                    assertThat(item.summary()).contains("defaultRouteConfigured=false");
                });
    }
    @Test
    void publish_withSkeletonPolicyAndDefaultRoute_isReady_fosW7_2() {
        when(templateService.evaluateBindings(templateId, admin)).thenReturn(nonBlockingBindings());
        when(coverageComputationService.compute(templateId, admin)).thenReturn(greenCoverage());
        ApiPolicyEntity skeleton = ApiPolicyEntity.createSkeleton(templateId, "10000002");
        skeleton.updateDefaultRouteDomain("1.0.0", "10000002");
        when(apiPolicyRepository.findByTemplateId(templateId)).thenReturn(Optional.of(skeleton));

        PublishGateChecklistView checklist = service.evaluate(templateId, admin);

        assertThat(checklist.items().stream()
                .filter(item -> item.checkCode() == PublishGateCheckCode.API_POLICY)
                .findFirst()
                .orElseThrow())
                .satisfies(item -> {
                    assertThat(item.blocker()).isFalse();
                    assertThat(item.ready()).isTrue();
                    assertThat(item.summary()).contains("defaultRouteConfigured=true");
                });
    }
    @Test
    void publish_withMalformedRuleExpression_isBlocked_ruleBounds() {
        when(templateService.evaluateBindings(templateId, admin)).thenReturn(nonBlockingBindings());
        when(coverageComputationService.compute(templateId, admin)).thenReturn(greenCoverage());
        when(templateRuleValidationService.validateRules(
                org.mockito.ArgumentMatchers.eq(templateId),
                org.mockito.ArgumentMatchers.any(),
                org.mockito.ArgumentMatchers.eq(admin)))
                .thenReturn(malformedRules());

        PublishGateChecklistView checklist = service.evaluate(templateId, admin);

        assertThat(checklist.ready()).isFalse();
        assertThat(checklist.items().stream()
                .anyMatch(item -> item.checkCode() == PublishGateCheckCode.RULE_BOUNDS && item.blocker()))
                .isTrue();
    }
    @Test
    void publish_withoutApiPolicySkeleton_isBlocked() {
        when(templateService.evaluateBindings(templateId, admin)).thenReturn(nonBlockingBindings());
        when(coverageComputationService.compute(templateId, admin)).thenReturn(greenCoverage());
        when(apiPolicyRepository.findByTemplateId(templateId)).thenReturn(Optional.empty());

        PublishGateChecklistView checklist = service.evaluate(templateId, admin);

        assertThat(checklist.items().stream()
                .anyMatch(item -> item.checkCode() == PublishGateCheckCode.API_POLICY && item.blocker()))
                .isTrue();
    }
    @Test
    void evaluateForRelease_usesPublishedVersion_withoutInFlightDev() {
        String releaseVersion = "1.0.0";
        UUID publishedVersionId = UUID.randomUUID();
        TemplateVersionEntity published = new TemplateVersionEntity(publishedVersionId, templateId, "10000002");
        published.setReleaseVersion(releaseVersion);
        when(templateVersionRepository.findByTemplateIdAndReleaseVersion(templateId, releaseVersion))
                .thenReturn(Optional.of(published));
        when(templateService.validateBindingsForVersion(templateId, published, admin))
                .thenReturn(nonBlockingBindings());
        when(coverageComputationService.computeForVersion(templateId, published, admin))
                .thenReturn(greenCoverage());
        when(changeDiffService.computeForVersion(templateId, published, admin))
                .thenReturn(new com.bank.docgen.template.api.ChangeDiffView(
                        templateId.toString(), null, publishedVersionId.toString(), null, false, 0, List.of(), List.of()));
        when(variableSchemaRepository.findByTemplateVersionIdOrderByVariableKeyAsc(publishedVersionId))
                .thenReturn(List.of(new VariableSchemaEntity(
                        UUID.randomUUID(), publishedVersionId, "field", VariableType.TEXT, true, null, null, "desc", null)));
        when(previewEvidencePort.countSuccessfulPreviews(templateId, publishedVersionId)).thenReturn(1);
        when(previewEvidencePort.countFailedPreviews(templateId, publishedVersionId)).thenReturn(0);
        when(contentModuleReferenceService.validateReferences(publishedVersionId))
                .thenReturn(new com.bank.docgen.template.api.ContentModuleReferenceValidationSummaryView(false, 0, 0));
        when(contentModuleReferenceService.evaluateEffectiveExpiry(publishedVersionId))
                .thenReturn(new com.bank.docgen.template.api.ContentModuleEffectiveExpirySummaryView(
                        false, 0, 0, List.of()));
        when(contentModuleReferenceService.evaluateEffectiveNotStarted(publishedVersionId))
                .thenReturn(new com.bank.docgen.template.api.ContentModuleEffectiveNotStartedSummaryView(
                        false, 0, 0, List.of()));
        when(anchorBindingRepository.findByTemplateVersionIdOrderByAnchorIdAsc(publishedVersionId))
                .thenReturn(List.of());
        when(templateRuleValidationService.validateRulesForVersion(
                org.mockito.ArgumentMatchers.eq(templateId),
                org.mockito.ArgumentMatchers.eq(published),
                org.mockito.ArgumentMatchers.any(),
                org.mockito.ArgumentMatchers.eq(admin)))
                .thenReturn(validRules());

        PublishGateChecklistView checklist = service.evaluateForRelease(templateId, releaseVersion, admin);

        assertThat(checklist.templateId()).isEqualTo(templateId.toString());
        assertThat(checklist.ready()).isTrue();
        assertThat(checklist.items()).isNotEmpty();
        assertThat(checklist.items().stream().map(item -> item.checkCode()).toList())
                .contains(
                        PublishGateCheckCode.ANCHOR_INTEGRITY,
                        PublishGateCheckCode.VARIABLE_SCHEMA,
                        PublishGateCheckCode.APPROVAL_SUMMARY,
                        PublishGateCheckCode.API_POLICY,
                        PublishGateCheckCode.PAGINATION_DELTA_BUDGET
                );
    }
    @Test
    void evaluateForRelease_unknownRelease_throwsTemplateNotFound() {
        when(templateVersionRepository.findByTemplateIdAndReleaseVersion(templateId, "9.9.9"))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.evaluateForRelease(templateId, "9.9.9", admin))
                .isInstanceOf(TemplateNotFoundException.class);
    }
}
