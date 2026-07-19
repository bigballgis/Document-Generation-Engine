package com.bank.docgen.template.service;

import com.bank.docgen.template.domain.ApprovalMatrixMode;
import com.bank.docgen.template.domain.ApprovalStage;
import com.bank.docgen.template.domain.ApprovalSubState;
import com.bank.docgen.template.domain.LifecycleAction;
import com.bank.docgen.template.domain.LifecycleDecision;
import com.bank.docgen.template.domain.TemplateLifecycleStatus;
import com.bank.docgen.template.persistence.TemplateEntity;
import com.bank.docgen.template.persistence.TemplateLifecycleRecordEntity;
import com.bank.docgen.template.persistence.TemplateLifecycleRecordRepository;
import java.util.List;
import org.springframework.stereotype.Component;

/**
 * Single source of truth for deriving the APPROVAL sub-state of a template.
 *
 * <p>There is no distinct {@code TEST_PASSED} lifecycle status: "test passed / awaiting
 * submit-for-approval" is modelled as {@code APPROVAL} + {@link ApprovalSubState#PENDING_SUBMIT}.
 * Under {@link ApprovalMatrixMode#SINGLE_TRACK}, "awaiting approval decision" is
 * {@link ApprovalSubState#PENDING_DECISION}. Under {@link ApprovalMatrixMode#LEGAL_THEN_COMPLIANCE},
 * submit enters {@link ApprovalSubState#PENDING_LEGAL_DECISION}; a LEGAL approve that remains in
 * {@code APPROVAL} advances to {@link ApprovalSubState#PENDING_COMPLIANCE_DECISION}.</p>
 */
@Component
public class ApprovalSubStateResolver {

    private final TemplateLifecycleRecordRepository lifecycleRecordRepository;

    public ApprovalSubStateResolver(TemplateLifecycleRecordRepository lifecycleRecordRepository) {
        this.lifecycleRecordRepository = lifecycleRecordRepository;
    }

    public ApprovalSubState resolve(TemplateEntity template) {
        if (template.getLifecycleStatus() != TemplateLifecycleStatus.APPROVAL) {
            return null;
        }
        List<TemplateLifecycleRecordEntity> records =
                lifecycleRecordRepository.findByTemplateIdOrderByCreatedAtDesc(template.getId());
        if (template.getApprovalMatrixMode() == ApprovalMatrixMode.LEGAL_THEN_COMPLIANCE) {
            return resolveMultiStage(records);
        }
        return resolveSingleTrack(records);
    }

    /**
     * Current multi-stage approval stage, or {@code null} when not in a LEGAL/COMPLIANCE window.
     */
    public ApprovalStage resolveStage(TemplateEntity template) {
        return ApprovalStage.fromSubState(resolve(template));
    }

    private static ApprovalSubState resolveSingleTrack(List<TemplateLifecycleRecordEntity> records) {
        for (TemplateLifecycleRecordEntity record : records) {
            if (record.getAction() == LifecycleAction.SUBMIT_FOR_APPROVAL) {
                return ApprovalSubState.PENDING_DECISION;
            }
            if (record.getAction() == LifecycleAction.RECORD_TEST_DECISION
                    && record.getToStatus() == TemplateLifecycleStatus.APPROVAL) {
                return ApprovalSubState.PENDING_SUBMIT;
            }
        }
        return ApprovalSubState.PENDING_SUBMIT;
    }

    private static ApprovalSubState resolveMultiStage(List<TemplateLifecycleRecordEntity> records) {
        for (TemplateLifecycleRecordEntity record : records) {
            if (record.getAction() == LifecycleAction.RECORD_APPROVAL_DECISION
                    && record.getDecision() == LifecycleDecision.APPROVED
                    && record.getToStatus() == TemplateLifecycleStatus.APPROVAL) {
                return ApprovalSubState.PENDING_COMPLIANCE_DECISION;
            }
            if (record.getAction() == LifecycleAction.SUBMIT_FOR_APPROVAL) {
                return ApprovalSubState.PENDING_LEGAL_DECISION;
            }
            if (record.getAction() == LifecycleAction.RECORD_TEST_DECISION
                    && record.getToStatus() == TemplateLifecycleStatus.APPROVAL) {
                return ApprovalSubState.PENDING_SUBMIT;
            }
        }
        return ApprovalSubState.PENDING_SUBMIT;
    }
}
