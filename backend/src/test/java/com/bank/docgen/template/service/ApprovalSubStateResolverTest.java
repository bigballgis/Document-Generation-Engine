package com.bank.docgen.template.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import com.bank.docgen.template.domain.ApprovalMatrixMode;
import com.bank.docgen.template.domain.ApprovalSubState;
import com.bank.docgen.template.domain.LifecycleAction;
import com.bank.docgen.template.domain.LifecycleDecision;
import com.bank.docgen.template.domain.TemplateLifecycleStatus;
import com.bank.docgen.template.persistence.TemplateEntity;
import com.bank.docgen.template.persistence.TemplateLifecycleRecordEntity;
import com.bank.docgen.template.persistence.TemplateLifecycleRecordRepository;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ApprovalSubStateResolverTest {

    @Mock
    private TemplateLifecycleRecordRepository lifecycleRecordRepository;

    private ApprovalSubStateResolver resolver;
    private UUID templateId;

    @BeforeEach
    void setUp() {
        resolver = new ApprovalSubStateResolver(lifecycleRecordRepository);
        templateId = UUID.randomUUID();
    }

    @Test
    void resolve_returnsNullWhenNotInApproval() {
        TemplateEntity template = template(TemplateLifecycleStatus.DRAFT);

        assertThat(resolver.resolve(template)).isNull();
    }

    @Test
    void resolve_returnsPendingSubmitAfterTestPassed() {
        TemplateEntity template = template(TemplateLifecycleStatus.APPROVAL);
        when(lifecycleRecordRepository.findByTemplateIdOrderByCreatedAtDesc(templateId))
                .thenReturn(List.of(record(
                        LifecycleAction.RECORD_TEST_DECISION,
                        TemplateLifecycleStatus.TESTING,
                        TemplateLifecycleStatus.APPROVAL,
                        null
                )));

        assertThat(resolver.resolve(template)).isEqualTo(ApprovalSubState.PENDING_SUBMIT);
    }

    @Test
    void resolve_returnsPendingDecisionAfterSubmitForApproval() {
        TemplateEntity template = template(TemplateLifecycleStatus.APPROVAL);
        when(lifecycleRecordRepository.findByTemplateIdOrderByCreatedAtDesc(templateId))
                .thenReturn(List.of(
                        record(
                                LifecycleAction.SUBMIT_FOR_APPROVAL,
                                TemplateLifecycleStatus.APPROVAL,
                                TemplateLifecycleStatus.APPROVAL,
                                null
                        ),
                        record(
                                LifecycleAction.RECORD_TEST_DECISION,
                                TemplateLifecycleStatus.TESTING,
                                TemplateLifecycleStatus.APPROVAL,
                                null
                        )
                ));

        assertThat(resolver.resolve(template)).isEqualTo(ApprovalSubState.PENDING_DECISION);
    }

    @Test
    void resolve_multiStage_returnsPendingLegalAfterSubmit() {
        TemplateEntity template = template(TemplateLifecycleStatus.APPROVAL);
        template.setApprovalMatrixMode(ApprovalMatrixMode.LEGAL_THEN_COMPLIANCE);
        when(lifecycleRecordRepository.findByTemplateIdOrderByCreatedAtDesc(templateId))
                .thenReturn(List.of(record(
                        LifecycleAction.SUBMIT_FOR_APPROVAL,
                        TemplateLifecycleStatus.APPROVAL,
                        TemplateLifecycleStatus.APPROVAL,
                        null
                )));

        assertThat(resolver.resolve(template)).isEqualTo(ApprovalSubState.PENDING_LEGAL_DECISION);
    }

    @Test
    void resolve_multiStage_returnsPendingComplianceAfterLegalApprove() {
        TemplateEntity template = template(TemplateLifecycleStatus.APPROVAL);
        template.setApprovalMatrixMode(ApprovalMatrixMode.LEGAL_THEN_COMPLIANCE);
        when(lifecycleRecordRepository.findByTemplateIdOrderByCreatedAtDesc(templateId))
                .thenReturn(List.of(
                        record(
                                LifecycleAction.RECORD_APPROVAL_DECISION,
                                TemplateLifecycleStatus.APPROVAL,
                                TemplateLifecycleStatus.APPROVAL,
                                LifecycleDecision.APPROVED
                        ),
                        record(
                                LifecycleAction.SUBMIT_FOR_APPROVAL,
                                TemplateLifecycleStatus.APPROVAL,
                                TemplateLifecycleStatus.APPROVAL,
                                null
                        )
                ));

        assertThat(resolver.resolve(template)).isEqualTo(ApprovalSubState.PENDING_COMPLIANCE_DECISION);
    }

    private TemplateEntity template(TemplateLifecycleStatus status) {
        TemplateEntity template = new TemplateEntity(
                templateId,
                "TPL-001",
                "RETAIL",
                "Sample",
                null,
                UUID.randomUUID(),
                "10000003"
        );
        template.setLifecycleStatus(status);
        return template;
    }

    private TemplateLifecycleRecordEntity record(
            LifecycleAction action,
            TemplateLifecycleStatus from,
            TemplateLifecycleStatus to,
            LifecycleDecision decision
    ) {
        return new TemplateLifecycleRecordEntity(
                UUID.randomUUID(),
                templateId,
                action,
                from,
                to,
                decision,
                null,
                null,
                "10000003"
        );
    }
}
