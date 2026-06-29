package com.bank.docgen.template.service;

import com.bank.docgen.template.domain.ApprovalSubState;
import com.bank.docgen.template.domain.LifecycleAction;
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
 * submit-for-approval" is modelled as {@code APPROVAL} + {@link ApprovalSubState#PENDING_SUBMIT},
 * and "awaiting approval decision" is {@code APPROVAL} + {@link ApprovalSubState#PENDING_DECISION}.</p>
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
}
