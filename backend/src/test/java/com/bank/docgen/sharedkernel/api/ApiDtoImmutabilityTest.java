package com.bank.docgen.sharedkernel.api;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.bank.docgen.apimgmt.api.ApiPolicyView;
import com.bank.docgen.audit.api.ManagementAuditEventView;
import com.bank.docgen.contentmodule.api.ContentModuleDetailView;
import com.bank.docgen.contentmodule.api.ContentModuleReviewRecordView;
import com.bank.docgen.contentmodule.domain.ContentModuleLifecycleState;
import com.bank.docgen.contentmodule.domain.ContentModuleReviewState;
import com.bank.docgen.master.api.MasterAnchorView;
import com.bank.docgen.master.api.MasterDocumentDetailView;
import com.bank.docgen.rendering.api.PreviewComparisonView;
import com.bank.docgen.runtime.api.BatchResultItemView;
import com.bank.docgen.runtime.api.BatchResultView;
import com.bank.docgen.runtime.api.BatchSummaryView;
import com.bank.docgen.runtime.api.ContractResultView;
import com.bank.docgen.runtime.api.EncryptionSummaryView;
import com.bank.docgen.runtime.api.OutputOptionsView;
import com.bank.docgen.contentmodule.api.ContentModuleVersionView;
import com.bank.docgen.sharedkernel.api.FieldError;
import com.bank.docgen.template.api.TemplateDetailView;
import com.bank.docgen.template.domain.ApprovalSubState;
import com.bank.docgen.template.domain.TemplateLifecycleStatus;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

class ApiDtoImmutabilityTest {

    @Test
    void contractResultViewDefensivelyCopiesCollectionFields() {
        List<String> paths = new ArrayList<>(List.of("/generate"));
        List<String> examples = new ArrayList<>(List.of("sample"));

        ContractResultView view = new ContractResultView(
                "tpl-1",
                paths,
                null,
                null,
                List.of(),
                List.of(),
                List.of(),
                examples
        );

        paths.add("/batch");
        examples.add("other");

        assertThat(view.paths()).containsExactly("/generate");
        assertThat(view.examples()).containsExactly("sample");
        assertThatThrownBy(() -> view.paths().add("x")).isInstanceOf(UnsupportedOperationException.class);
    }

    @Test
    void templateDetailViewDefensivelyCopiesSchemaLists() {
        List<String> variableIds = new ArrayList<>(List.of("var-1"));

        TemplateDetailView view = new TemplateDetailView(
                "id",
                "ext",
                "RETAIL",
                "Name",
                "Desc",
                "master-1",
                TemplateLifecycleStatus.DRAFT,
                ApprovalSubState.PENDING_SUBMIT,
                "1.0",
                "dev-1",
                1,
                List.of(),
                List.of(),
                List.of(),
                Instant.parse("2026-01-01T00:00:00Z"),
                Instant.parse("2026-01-02T00:00:00Z"),
                null,
                null,
                false
        );

        variableIds.add("var-2");
        assertThat(view.variables()).isEmpty();
        assertThatThrownBy(() -> view.bindings().add(null)).isInstanceOf(UnsupportedOperationException.class);
    }

    @Test
    void masterDocumentDetailViewDefensivelyCopiesAnchorsAndReviewHistory() {
        List<MasterAnchorView> anchors = new ArrayList<>(List.of(new MasterAnchorView("a1", "Anchor 1", 0)));
        List<String> reviewActorIds = new ArrayList<>(List.of("10000001"));

        MasterDocumentDetailView view = new MasterDocumentDetailView(
                "doc-1",
                "RETAIL",
                "Master",
                "Desc",
                "ACTIVE",
                "master.docx",
                "Initial",
                anchors,
                List.of(),
                "10000001",
                "10000001",
                Instant.parse("2026-01-01T00:00:00Z"),
                Instant.parse("2026-01-02T00:00:00Z")
        );

        anchors.add(new MasterAnchorView("a2", "Anchor 2", 1));
        reviewActorIds.add("10000002");

        assertThat(view.anchors()).hasSize(1);
        assertThatThrownBy(() -> view.anchors().clear()).isInstanceOf(UnsupportedOperationException.class);
    }

    @Test
    void batchResultViewDefensivelyCopiesItems() {
        List<BatchResultItemView> items = new ArrayList<>(List.of(batchItem("item-1")));

        BatchResultView view = new BatchResultView(
                "batch-1",
                new BatchSummaryView(1, 1, 1, 0, 0),
                items
        );

        items.add(batchItem("item-2"));

        assertThat(view.items()).hasSize(1);
        assertThat(view.items().getFirst().itemId()).isEqualTo("item-1");
        assertThatThrownBy(() -> view.items().add(batchItem("item-3")))
                .isInstanceOf(UnsupportedOperationException.class);
    }

    @Test
    void apiPolicyViewDefensivelyCopiesPolicyLists() {
        List<String> adGroups = new ArrayList<>(List.of("DOCGEN-API-RETAIL"));
        List<String> formats = new ArrayList<>(List.of("PDF"));
        List<String> modes = new ArrayList<>(List.of("ATTACHMENT"));

        ApiPolicyView view = new ApiPolicyView(
                "tpl-1",
                1,
                adGroups,
                "1.0",
                formats,
                modes,
                true,
                10,
                5,
                20,
                false,
                false,
                true,
                30,
                90,
                Instant.parse("2026-01-01T00:00:00Z")
        );

        adGroups.add("DOCGEN-API-CORP");
        formats.add("DOCX");
        modes.add("INLINE");

        assertThat(view.allowedAdGroups()).containsExactly("DOCGEN-API-RETAIL");
        assertThat(view.outputFormats()).containsExactly("PDF");
        assertThat(view.outputModes()).containsExactly("ATTACHMENT");
    }

    @Test
    void managementAuditEventViewDefensivelyCopiesChangedAreasAndWarnings() {
        List<String> changedAreas = new ArrayList<>(List.of("OUTPUT_POLICY"));
        List<String> warningCodes = new ArrayList<>(List.of("WARN-1"));

        ManagementAuditEventView view = new ManagementAuditEventView(
                Instant.parse("2026-01-01T00:00:00Z"),
                "POLICY_UPDATED",
                "tpl-1",
                null,
                null,
                "cred-1",
                1,
                2,
                changedAreas,
                false,
                null,
                "actor",
                "fp",
                "ok",
                warningCodes,
                null
        );

        changedAreas.add("BATCH_LIMITS");
        warningCodes.add("WARN-2");

        assertThat(view.changedAreas()).containsExactly("OUTPUT_POLICY");
        assertThat(view.warningCodes()).containsExactly("WARN-1");
    }

    @Test
    void contentModuleDetailViewDefensivelyCopiesSharedGroupsAndVersions() {
        List<String> sharedGroups = new ArrayList<>(List.of("RETAIL"));
        List<ContentModuleVersionView> versions = new ArrayList<>(List.of(
                new ContentModuleVersionView(
                        "ver-1",
                        "1.0.0",
                        ContentModuleReviewState.DRAFT,
                        ContentModuleLifecycleState.ACTIVE,
                        "Initial",
                        "{}",
                        null,
                        Instant.parse("2026-01-01T00:00:00Z"),
                        Instant.parse("2026-01-02T00:00:00Z")
                )
        ));
        List<ContentModuleReviewRecordView> history = new ArrayList<>();

        ContentModuleDetailView view = new ContentModuleDetailView(
                "module-1",
                "MOD-001",
                "RETAIL",
                "Module",
                "Desc",
                sharedGroups,
                versions,
                history
        );

        sharedGroups.add("CORP");
        versions.add(new ContentModuleVersionView(
                "ver-2",
                "1.1.0",
                ContentModuleReviewState.DRAFT,
                ContentModuleLifecycleState.ACTIVE,
                "Update",
                "{}",
                null,
                Instant.parse("2026-01-03T00:00:00Z"),
                Instant.parse("2026-01-04T00:00:00Z")
        ));
        history.add(new ContentModuleReviewRecordView(
                "SUBMITTED",
                null,
                "Initial",
                null,
                "10000003",
                Instant.parse("2026-01-01T00:00:00Z"),
                "1.0.0",
                null,
                null
        ));

        assertThat(view.sharedGroupCodes()).containsExactly("RETAIL");
        assertThat(view.versions()).hasSize(1);
        assertThat(view.reviewHistory()).isEmpty();
    }

    @Test
    void previewComparisonViewDefensivelyCopiesItems() {
        List<String> labels = new ArrayList<>(List.of("blocker"));

        PreviewComparisonView view = new PreviewComparisonView(1, 1, 0, List.of());

        labels.add("warning");
        assertThat(view.items()).isEmpty();
        assertThatThrownBy(() -> view.items().add(null)).isInstanceOf(UnsupportedOperationException.class);
    }

    @Test
    void errorDetailDefensivelyCopiesFieldErrorsAndConflictMap() {
        List<FieldError> fieldErrors = new ArrayList<>(List.of(
                new FieldError("name", "required", "Required")
        ));
        Map<String, Object> conflict = new HashMap<>(Map.of("requestId", "req-1"));

        ErrorDetail detail = new ErrorDetail(
                "VALIDATION_FAILED",
                "VALIDATION",
                "Validation failed",
                "api.error.validation.validationFailed",
                false,
                fieldErrors,
                conflict
        );

        fieldErrors.add(new FieldError("code", "invalid", "Invalid"));
        conflict.put("status", "PENDING");

        assertThat(detail.fieldErrors()).hasSize(1);
        assertThat(detail.idempotencyConflict()).containsEntry("requestId", "req-1");
        assertThat(detail.idempotencyConflict()).doesNotContainKey("status");
    }

    private static BatchResultItemView batchItem(String itemId) {
        return new BatchResultItemView(
                itemId,
                "SUCCESS",
                new OutputOptionsView("PDF", "ATTACHMENT"),
                EncryptionSummaryView.disabled("PDF"),
                "doc-" + itemId,
                List.of()
        );
    }
}
