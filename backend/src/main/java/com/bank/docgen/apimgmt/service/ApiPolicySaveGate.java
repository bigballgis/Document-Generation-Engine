package com.bank.docgen.apimgmt.service;

import com.bank.docgen.apimgmt.api.ApiPolicyImpactPreviewView;
import com.bank.docgen.audit.api.PolicyUpdateAuditDetail;
import com.bank.docgen.template.service.TemplateValidationException;
import java.util.ArrayList;
import java.util.List;

final class ApiPolicySaveGate {

    private ApiPolicySaveGate() {
    }

    static void requireSaveAllowed(ApiPolicyImpactPreviewView preview, boolean confirmed) {
        if (preview.blocking()) {
            throw new TemplateValidationException("api.error.apimgmt.policyImpactBlocked");
        }
        if (!preview.warnings().isEmpty() && !confirmed) {
            throw new TemplateValidationException("api.error.apimgmt.policyImpactConfirmationRequired");
        }
    }

    static PolicyUpdateAuditDetail auditDetailFromPreview(
            ApiPolicyImpactPreviewView preview,
            boolean confirmed,
            List<String> configDiffSummary
    ) {
        List<String> hardBlockSummary = preview.blocking()
                ? List.of(preview.summaryMessageKey())
                : List.of();
        return new PolicyUpdateAuditDetail(
                configDiffSummary == null ? List.of() : List.copyOf(configDiffSummary),
                buildImpactPreviewSummary(preview),
                hardBlockSummary,
                preview.warnings(),
                confirmed || preview.warnings().isEmpty(),
                false,
                null
        );
    }

    private static List<String> buildImpactPreviewSummary(ApiPolicyImpactPreviewView preview) {
        List<String> summary = new ArrayList<>();
        summary.add(preview.summaryMessageKey());
        if (preview.contractDiffSummary() != null && !preview.contractDiffSummary().isBlank()) {
            summary.add(preview.contractDiffSummary());
        }
        if (preview.idempotencyImpactSummary() != null && !preview.idempotencyImpactSummary().isBlank()) {
            summary.add(preview.idempotencyImpactSummary());
        }
        return summary;
    }
}
