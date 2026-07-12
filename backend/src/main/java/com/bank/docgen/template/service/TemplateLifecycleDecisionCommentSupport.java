package com.bank.docgen.template.service;

import com.bank.docgen.sharedkernel.security.ManagementSessionClaims;
import com.bank.docgen.template.api.LifecycleDecisionRequest;
import com.bank.docgen.template.domain.LifecycleDecision;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Package-private formatting for persisted lifecycle decision comments / structured opinions.
 */
final class TemplateLifecycleDecisionCommentSupport {

    private static final String STRUCTURED_OPINION_PREFIX = DecisionFormService.STRUCTURED_OPINION_PREFIX;

    private final DecisionFormService decisionFormService;
    private final ObjectMapper objectMapper;

    TemplateLifecycleDecisionCommentSupport(DecisionFormService decisionFormService, ObjectMapper objectMapper) {
        this.decisionFormService = decisionFormService;
        this.objectMapper = objectMapper;
    }

    String formatDecisionComment(LifecycleDecisionRequest request, ManagementSessionClaims session) {
        String comment = request.commentSummary();
        if (requiresStructuredNegativeOpinion(request.decision())) {
            comment = appendBlock(comment, formatStructuredDecisionComment(request));
        }
        if (request.decision() == LifecycleDecision.REJECTED) {
            comment = appendBlock(comment, formatRemediationLinks(request));
        }
        String exceptionMarker = formatExceptionMarker(request, session);
        if (exceptionMarker != null) {
            comment = appendBlock(comment, exceptionMarker);
        }
        return comment;
    }

    String normalizeComment(String commentSummary) {
        return commentSummary == null ? "" : commentSummary.trim();
    }

    private boolean requiresStructuredNegativeOpinion(LifecycleDecision decision) {
        return decision == LifecycleDecision.FAILED || decision == LifecycleDecision.REJECTED;
    }

    private String formatStructuredDecisionComment(LifecycleDecisionRequest request) {
        Map<String, String> structured = new LinkedHashMap<>();
        structured.put("reasonCategory", request.reasonCategory().trim());
        structured.put("impactSummary", request.impactSummary().trim());
        try {
            return STRUCTURED_OPINION_PREFIX + objectMapper.writeValueAsString(structured);
        } catch (JsonProcessingException ex) {
            throw new TemplateValidationException("api.error.validation.requestBodyInvalid");
        }
    }

    private String formatRemediationLinks(LifecycleDecisionRequest request) {
        Map<String, String> remediation = new LinkedHashMap<>();
        if (!isBlank(request.remediationTestRecordId())) {
            remediation.put("testRecordId", request.remediationTestRecordId().trim());
        }
        if (!isBlank(request.remediationChangeDiffRef())) {
            remediation.put("changeDiffRef", request.remediationChangeDiffRef().trim());
        }
        if (!isBlank(request.remediationChecklistCode())) {
            remediation.put("checklistCode", request.remediationChecklistCode().trim());
        }
        if (remediation.isEmpty()) {
            return null;
        }
        try {
            return STRUCTURED_OPINION_PREFIX + objectMapper.writeValueAsString(remediation);
        } catch (JsonProcessingException ex) {
            throw new TemplateValidationException("api.error.validation.requestBodyInvalid");
        }
    }

    private String formatExceptionMarker(LifecycleDecisionRequest request, ManagementSessionClaims session) {
        if (!decisionFormService.isGroupAdminException(request, session)) {
            return null;
        }
        Map<String, String> marker = new LinkedHashMap<>();
        marker.put("exceptionReason", request.exceptionReason().trim());
        try {
            return DecisionFormService.EXCEPTION_INTERVENTION_PREFIX
                    + objectMapper.writeValueAsString(marker);
        } catch (JsonProcessingException ex) {
            throw new TemplateValidationException("api.error.validation.requestBodyInvalid");
        }
    }

    private String appendBlock(String comment, String block) {
        if (block == null || block.isBlank()) {
            return comment;
        }
        if (comment != null && !comment.isBlank()) {
            return comment.trim() + "\n" + block;
        }
        return block;
    }

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }
}
