package com.bank.docgen.template.api;

import com.bank.docgen.sharedkernel.api.DefensiveCopies;
import com.bank.docgen.sharedkernel.document.compute.ComputeDslLimits;

import com.bank.docgen.template.domain.ApprovalMatrixMode;
import com.bank.docgen.template.domain.ApprovalStage;
import com.bank.docgen.template.domain.ApprovalSubState;
import com.bank.docgen.template.domain.TemplateLifecycleStatus;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

public record TemplateDetailView(
        String id,
        String externalId,
        String groupCode,
        String name,
        String description,
        String masterId,
        TemplateLifecycleStatus lifecycleStatus,
        ApprovalSubState approvalSubState,
        String releaseVersion,
        String devVersionId,
        int devVersionNumber,
        List<VariableSchemaView> variables,
        List<AnchorBindingView> bindings,
        List<CompositionRuleView> rules,
        Instant createdAt,
        Instant updatedAt,
        String updatedBy,
        String updatedByDisplayName,
        boolean readOnly,
        TemplateExportMasterPinView masterPin,
        LocalDate nextReviewDue,
        String locale,
        String localeVariantFamilyId,
        ApprovalMatrixMode approvalMatrixMode,
        ApprovalStage approvalStage,
        List<String> allowedDocumentBrandCodes
) {
    public TemplateDetailView {
        variables = DefensiveCopies.copyList(variables);
        bindings = DefensiveCopies.copyList(bindings);
        rules = DefensiveCopies.copyList(rules);
        allowedDocumentBrandCodes = DefensiveCopies.copyList(allowedDocumentBrandCodes);
        if (approvalMatrixMode == null) {
            approvalMatrixMode = ApprovalMatrixMode.SINGLE_TRACK;
        }
    }

    /** Compatibility constructor for callers that omit locale + matrix fields. */
    public TemplateDetailView(
            String id,
            String externalId,
            String groupCode,
            String name,
            String description,
            String masterId,
            TemplateLifecycleStatus lifecycleStatus,
            ApprovalSubState approvalSubState,
            String releaseVersion,
            String devVersionId,
            int devVersionNumber,
            List<VariableSchemaView> variables,
            List<AnchorBindingView> bindings,
            List<CompositionRuleView> rules,
            Instant createdAt,
            Instant updatedAt,
            String updatedBy,
            String updatedByDisplayName,
            boolean readOnly,
            TemplateExportMasterPinView masterPin,
            LocalDate nextReviewDue
    ) {
        this(
                id,
                externalId,
                groupCode,
                name,
                description,
                masterId,
                lifecycleStatus,
                approvalSubState,
                releaseVersion,
                devVersionId,
                devVersionNumber,
                variables,
                bindings,
                rules,
                createdAt,
                updatedAt,
                updatedBy,
                updatedByDisplayName,
                readOnly,
                masterPin,
                nextReviewDue,
                ComputeDslLimits.DEFAULT_LOCALE,
                null,
                ApprovalMatrixMode.SINGLE_TRACK,
                null,
                List.of()
        );
    }

    /** Compatibility constructor for callers that omit matrix fields. */
    public TemplateDetailView(
            String id,
            String externalId,
            String groupCode,
            String name,
            String description,
            String masterId,
            TemplateLifecycleStatus lifecycleStatus,
            ApprovalSubState approvalSubState,
            String releaseVersion,
            String devVersionId,
            int devVersionNumber,
            List<VariableSchemaView> variables,
            List<AnchorBindingView> bindings,
            List<CompositionRuleView> rules,
            Instant createdAt,
            Instant updatedAt,
            String updatedBy,
            String updatedByDisplayName,
            boolean readOnly,
            TemplateExportMasterPinView masterPin,
            LocalDate nextReviewDue,
            String locale,
            String localeVariantFamilyId
    ) {
        this(
                id,
                externalId,
                groupCode,
                name,
                description,
                masterId,
                lifecycleStatus,
                approvalSubState,
                releaseVersion,
                devVersionId,
                devVersionNumber,
                variables,
                bindings,
                rules,
                createdAt,
                updatedAt,
                updatedBy,
                updatedByDisplayName,
                readOnly,
                masterPin,
                nextReviewDue,
                locale,
                localeVariantFamilyId,
                ApprovalMatrixMode.SINGLE_TRACK,
                null,
                List.of()
        );
    }

    /** Compatibility constructor for callers that omit allow-list. */
    public TemplateDetailView(
            String id,
            String externalId,
            String groupCode,
            String name,
            String description,
            String masterId,
            TemplateLifecycleStatus lifecycleStatus,
            ApprovalSubState approvalSubState,
            String releaseVersion,
            String devVersionId,
            int devVersionNumber,
            List<VariableSchemaView> variables,
            List<AnchorBindingView> bindings,
            List<CompositionRuleView> rules,
            Instant createdAt,
            Instant updatedAt,
            String updatedBy,
            String updatedByDisplayName,
            boolean readOnly,
            TemplateExportMasterPinView masterPin,
            LocalDate nextReviewDue,
            String locale,
            String localeVariantFamilyId,
            ApprovalMatrixMode approvalMatrixMode,
            ApprovalStage approvalStage
    ) {
        this(
                id,
                externalId,
                groupCode,
                name,
                description,
                masterId,
                lifecycleStatus,
                approvalSubState,
                releaseVersion,
                devVersionId,
                devVersionNumber,
                variables,
                bindings,
                rules,
                createdAt,
                updatedAt,
                updatedBy,
                updatedByDisplayName,
                readOnly,
                masterPin,
                nextReviewDue,
                locale,
                localeVariantFamilyId,
                approvalMatrixMode,
                approvalStage,
                List.of()
        );
    }
}
