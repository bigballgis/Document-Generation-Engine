package com.bank.docgen.template.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;
import com.bank.docgen.template.api.PublishGateChecklistView;
import com.bank.docgen.template.domain.PublishGateCheckCode;
import com.bank.docgen.template.persistence.AnchorBindingEntity;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;

/**
 * Peeled from PublishGateServiceTest (AI-SCALE #169).
 */
class PublishGateServiceContentModuleTest extends PublishGateServiceTestFixtures {

    @Test
    void publishGate_blocksContentModuleEffectiveExpired_lm008() {
        when(templateService.evaluateBindings(templateId, admin)).thenReturn(nonBlockingBindings());
        when(coverageComputationService.compute(templateId, admin)).thenReturn(greenCoverage());
        when(contentModuleReferenceService.evaluateEffectiveExpiry(versionId))
                .thenReturn(new com.bank.docgen.template.api.ContentModuleEffectiveExpirySummaryView(
                        true,
                        1,
                        1,
                        List.of("MOD-A@1.0.0 effectiveTo=2026-07-01T00:00:00Z")
                ));

        PublishGateChecklistView checklist = service.evaluate(templateId, admin);

        assertThat(checklist.ready()).isFalse();
        assertThat(checklist.items().stream()
                .filter(item -> item.checkCode() == PublishGateCheckCode.CONTENT_MODULE_EFFECTIVE_EXPIRED)
                .findFirst()
                .orElseThrow())
                .satisfies(item -> {
                    assertThat(item.blocker()).isTrue();
                    assertThat(item.ready()).isFalse();
                    assertThat(item.messageKey()).isEqualTo("api.publishGate.contentModuleEffectiveExpired.blocked");
                    assertThat(item.summary()).contains("MOD-A@1.0.0");
                });
        assertThatThrownBy(() -> service.assertReady(templateId, admin))
                .isInstanceOf(TemplateValidationException.class);
    }
    @Test
    void publishGate_blocksContentModuleEffectiveNotStarted_e5001() {
        when(templateService.evaluateBindings(templateId, admin)).thenReturn(nonBlockingBindings());
        when(coverageComputationService.compute(templateId, admin)).thenReturn(greenCoverage());
        when(contentModuleReferenceService.evaluateEffectiveNotStarted(versionId))
                .thenReturn(new com.bank.docgen.template.api.ContentModuleEffectiveNotStartedSummaryView(
                        true,
                        1,
                        1,
                        List.of("CLAUSE-A MOD-A@1.0.0 effectiveFrom=2027-01-01T00:00:00Z")
                ));

        PublishGateChecklistView checklist = service.evaluate(templateId, admin);

        assertThat(checklist.ready()).isFalse();
        assertThat(checklist.items().stream()
                .filter(item -> item.checkCode() == PublishGateCheckCode.CONTENT_MODULE_EFFECTIVE_NOT_STARTED)
                .findFirst()
                .orElseThrow())
                .satisfies(item -> {
                    assertThat(item.blocker()).isTrue();
                    assertThat(item.ready()).isFalse();
                    assertThat(item.messageKey())
                            .isEqualTo("api.publishGate.contentModuleEffectiveNotStarted.blocked");
                    assertThat(item.summary()).contains("CLAUSE-A");
                    assertThat(item.summary()).contains("effectiveFrom=");
                    assertThat(item.checkCode())
                            .isNotEqualTo(PublishGateCheckCode.CONTENT_MODULE_EFFECTIVE_EXPIRED);
                });
        assertThatThrownBy(() -> service.assertReady(templateId, admin))
                .isInstanceOf(TemplateValidationException.class);
    }
    @Test
    void publishGate_contentModuleReferencesPass_whenOnlyEffectiveExpired_lm013() {
        when(templateService.evaluateBindings(templateId, admin)).thenReturn(nonBlockingBindings());
        when(coverageComputationService.compute(templateId, admin)).thenReturn(greenCoverage());
        when(contentModuleReferenceService.validateReferences(versionId))
                .thenReturn(new com.bank.docgen.template.api.ContentModuleReferenceValidationSummaryView(false, 1, 0));
        when(contentModuleReferenceService.evaluateEffectiveExpiry(versionId))
                .thenReturn(new com.bank.docgen.template.api.ContentModuleEffectiveExpirySummaryView(
                        true, 1, 1, List.of("MOD-A@1.0.0 effectiveTo=2026-07-01T00:00:00Z")));

        PublishGateChecklistView checklist = service.evaluate(templateId, admin);

        assertThat(checklist.items().stream()
                .filter(item -> item.checkCode() == PublishGateCheckCode.CONTENT_MODULE_REFERENCES)
                .findFirst()
                .orElseThrow()
                .ready()).isTrue();
        assertThat(checklist.items().stream()
                .filter(item -> item.checkCode() == PublishGateCheckCode.CONTENT_MODULE_EFFECTIVE_EXPIRED)
                .findFirst()
                .orElseThrow()
                .blocker()).isTrue();
    }
    @Test
    void publishGate_blocksContentModuleReferencesWithEmptyPinnedStructure() {
        when(templateService.evaluateBindings(templateId, admin)).thenReturn(nonBlockingBindings());
        when(coverageComputationService.compute(templateId, admin)).thenReturn(greenCoverage());
        when(contentModuleReferenceService.validateReferences(versionId))
                .thenReturn(new com.bank.docgen.template.api.ContentModuleReferenceValidationSummaryView(true, 1, 1));

        PublishGateChecklistView checklist = service.evaluate(templateId, admin);

        assertThat(checklist.ready()).isFalse();
        assertThat(checklist.items().stream()
                .filter(item -> item.checkCode() == PublishGateCheckCode.CONTENT_MODULE_REFERENCES)
                .findFirst()
                .orElseThrow())
                .satisfies(item -> {
                    assertThat(item.blocker()).isTrue();
                    assertThat(item.ready()).isFalse();
                    assertThat(item.messageKey()).isEqualTo("api.publishGate.contentModuleReferences.blocked");
                });
        assertThatThrownBy(() -> service.assertReady(templateId, admin))
                .isInstanceOf(TemplateValidationException.class);
    }
    @Test
    void publishGate_blocksInvalidContentModuleReferences() {
        when(templateService.evaluateBindings(templateId, admin)).thenReturn(nonBlockingBindings());
        when(coverageComputationService.compute(templateId, admin)).thenReturn(greenCoverage());
        when(contentModuleReferenceService.validateReferences(versionId))
                .thenReturn(new com.bank.docgen.template.api.ContentModuleReferenceValidationSummaryView(true, 1, 1));

        PublishGateChecklistView checklist = service.evaluate(templateId, admin);

        assertThat(checklist.ready()).isFalse();
        assertThat(checklist.items().stream()
                .anyMatch(item -> item.checkCode() == PublishGateCheckCode.CONTENT_MODULE_REFERENCES
                        && item.blocker()))
                .isTrue();
    }
    @Test
    void publishGate_allowsQrBarcodeRef_alone() {
        // BDD-CE-K06b-006 — qrBarcodeRef no longer hard-blocks publish gate
        when(templateService.evaluateBindings(templateId, admin)).thenReturn(nonBlockingBindings());
        when(coverageComputationService.compute(templateId, admin)).thenReturn(greenCoverage());
        String qrJson = "{\"nodes\":[{\"type\":\"qrBarcodeRef\",\"referenceKey\":\"PAYMENT-QR\"}]}";
        AnchorBindingEntity binding = new AnchorBindingEntity(
                UUID.randomUUID(),
                versionId,
                "BODY",
                com.bank.docgen.template.domain.AnchorContentType.RICH_TEXT,
                qrJson,
                com.bank.docgen.template.domain.BindingValidationStatus.VALID
        );
        when(anchorBindingRepository.findByTemplateVersionIdOrderByAnchorIdAsc(versionId))
                .thenReturn(List.of(binding));
        when(nodeMatrixValidationService.countUnsupportedNodeBlockers(qrJson)).thenReturn(0);

        PublishGateChecklistView checklist = service.evaluate(templateId, admin);

        assertThat(checklist.items().stream()
                .filter(item -> item.checkCode() == PublishGateCheckCode.UNSUPPORTED_STRUCTURED_NODES)
                .findFirst()
                .orElseThrow())
                .satisfies(item -> {
                    assertThat(item.blocker()).isFalse();
                    assertThat(item.ready()).isTrue();
                });
    }
    @Test
    void publishGate_allowsAttachmentListRef_alone() {
        // BDD-CE-K06c-003 — attachmentListRef no longer hard-blocks publish gate
        when(templateService.evaluateBindings(templateId, admin)).thenReturn(nonBlockingBindings());
        when(coverageComputationService.compute(templateId, admin)).thenReturn(greenCoverage());
        String attachmentJson = "{\"nodes\":[{\"type\":\"attachmentListRef\",\"referenceKey\":\"ATTACHMENTS\"}]}";
        AnchorBindingEntity binding = new AnchorBindingEntity(
                UUID.randomUUID(),
                versionId,
                "BODY",
                com.bank.docgen.template.domain.AnchorContentType.RICH_TEXT,
                attachmentJson,
                com.bank.docgen.template.domain.BindingValidationStatus.VALID
        );
        when(anchorBindingRepository.findByTemplateVersionIdOrderByAnchorIdAsc(versionId))
                .thenReturn(List.of(binding));
        when(nodeMatrixValidationService.countUnsupportedNodeBlockers(attachmentJson)).thenReturn(0);

        PublishGateChecklistView checklist = service.evaluate(templateId, admin);

        assertThat(checklist.items().stream()
                .filter(item -> item.checkCode() == PublishGateCheckCode.UNSUPPORTED_STRUCTURED_NODES)
                .findFirst()
                .orElseThrow())
                .satisfies(item -> {
                    assertThat(item.blocker()).isFalse();
                    assertThat(item.ready()).isTrue();
                });
    }
    @Test
    void publishGate_blocksUnresolvedPasteCleaningResidue() {
        // BDD-OPS-PASTE-BINDING-001 / S4
        when(templateService.evaluateBindings(templateId, admin)).thenReturn(nonBlockingBindings());
        when(coverageComputationService.compute(templateId, admin)).thenReturn(greenCoverage());
        AnchorBindingEntity binding = new AnchorBindingEntity(
                UUID.randomUUID(),
                versionId,
                "BODY",
                com.bank.docgen.template.domain.AnchorContentType.RICH_TEXT,
                "{\"schemaVersion\":\"1.0\",\"nodes\":[]}",
                com.bank.docgen.template.domain.BindingValidationStatus.INCOMPATIBLE_CONTENT_TYPE
        );
        binding.setPasteCleaningEvidenceJson("""
                {"transformedCount":0,"removedCount":0,"warningCount":0,"blockedCount":1,"unresolvedPasteBlockers":true,"items":[{"category":"BLOCKED","messageKey":"paste.summary.blocked","detectionSummary":"Blocked embedded object in pasted HTML."}]}
                """);
        when(anchorBindingRepository.findByTemplateVersionIdOrderByAnchorIdAsc(versionId))
                .thenReturn(List.of(binding));

        PublishGateChecklistView checklist = service.evaluate(templateId, admin);

        assertThat(checklist.ready()).isFalse();
        assertThat(checklist.items().stream()
                .filter(item -> item.checkCode() == PublishGateCheckCode.PASTE_CLEANING_BLOCKERS)
                .findFirst()
                .orElseThrow())
                .satisfies(item -> {
                    assertThat(item.blocker()).isTrue();
                    assertThat(item.ready()).isFalse();
                    assertThat(item.messageKey()).isEqualTo("api.publishGate.pasteCleaningBlockers.blocked");
                    assertThat(item.summary()).contains("unresolvedPasteBindings=1");
                });
        assertThatThrownBy(() -> service.assertReady(templateId, admin))
                .isInstanceOf(TemplateValidationException.class);
    }
    @Test
    void publishGate_pasteCleaningReadyWhenResidueCleared() {
        // BDD-OPS-PASTE-BINDING-001 / S5 (publish dimension)
        when(templateService.evaluateBindings(templateId, admin)).thenReturn(nonBlockingBindings());
        when(coverageComputationService.compute(templateId, admin)).thenReturn(greenCoverage());
        AnchorBindingEntity binding = new AnchorBindingEntity(
                UUID.randomUUID(),
                versionId,
                "BODY",
                com.bank.docgen.template.domain.AnchorContentType.RICH_TEXT,
                "{\"schemaVersion\":\"1.0\",\"nodes\":[]}",
                com.bank.docgen.template.domain.BindingValidationStatus.VALID
        );
        binding.setPasteCleaningEvidenceJson("""
                {"transformedCount":1,"removedCount":0,"warningCount":0,"blockedCount":0,"unresolvedPasteBlockers":false,"items":[]}
                """);
        when(anchorBindingRepository.findByTemplateVersionIdOrderByAnchorIdAsc(versionId))
                .thenReturn(List.of(binding));

        PublishGateChecklistView checklist = service.evaluate(templateId, admin);

        assertThat(checklist.items().stream()
                .filter(item -> item.checkCode() == PublishGateCheckCode.PASTE_CLEANING_BLOCKERS)
                .findFirst()
                .orElseThrow())
                .satisfies(item -> {
                    assertThat(item.blocker()).isFalse();
                    assertThat(item.ready()).isTrue();
                    assertThat(item.messageKey()).isEqualTo("api.publishGate.pasteCleaningBlockers.ready");
                });
    }
    @Test
    void publishGate_unsupportedStructuredNodes_readyWhenAbsent() {
        // A8 — happy path: no writer-unsupported nodes → dedicated check ready
        when(templateService.evaluateBindings(templateId, admin)).thenReturn(nonBlockingBindings());
        when(coverageComputationService.compute(templateId, admin)).thenReturn(greenCoverage());

        PublishGateChecklistView checklist = service.evaluate(templateId, admin);

        assertThat(checklist.items().stream()
                .filter(item -> item.checkCode() == PublishGateCheckCode.UNSUPPORTED_STRUCTURED_NODES)
                .findFirst()
                .orElseThrow())
                .satisfies(item -> {
                    assertThat(item.blocker()).isFalse();
                    assertThat(item.ready()).isTrue();
                    assertThat(item.messageKey()).isEqualTo("api.publishGate.unsupportedStructuredNodes.ready");
                });
    }
    @Test
    void publishGate_blocksWhenLatestPreviewHasUnviewedFidelityWarnings() {
        when(templateService.evaluateBindings(templateId, admin)).thenReturn(nonBlockingBindings());
        when(coverageComputationService.compute(templateId, admin)).thenReturn(greenCoverage());
        when(previewEvidencePort.countUnviewedFidelityWarnings(templateId, versionId)).thenReturn(2);

        PublishGateChecklistView checklist = service.evaluate(templateId, admin);

        assertThat(checklist.ready()).isFalse();
        assertThat(checklist.items().stream()
                .anyMatch(item -> item.checkCode() == PublishGateCheckCode.FIDELITY_WARNINGS_VIEWED
                        && item.blocker()))
                .isTrue();
    }
    @Test
    void publishGate_readyWhenAllFidelityWarningsViewed() {
        when(templateService.evaluateBindings(templateId, admin)).thenReturn(nonBlockingBindings());
        when(coverageComputationService.compute(templateId, admin)).thenReturn(greenCoverage());
        when(previewEvidencePort.countUnviewedFidelityWarnings(templateId, versionId)).thenReturn(0);

        PublishGateChecklistView checklist = service.evaluate(templateId, admin);

        assertThat(checklist.items().stream()
                .filter(item -> item.checkCode() == PublishGateCheckCode.FIDELITY_WARNINGS_VIEWED)
                .findFirst()
                .orElseThrow()
                .ready())
                .isTrue();
    }
}
