package com.bank.docgen.template.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import com.bank.docgen.template.domain.ApprovalSubState;
import com.bank.docgen.template.domain.LifecycleAction;
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
                        TemplateLifecycleStatus.APPROVAL
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
                                TemplateLifecycleStatus.APPROVAL
                        ),
                        record(
                                LifecycleAction.RECORD_TEST_DECISION,
                                TemplateLifecycleStatus.TESTING,
                                TemplateLifecycleStatus.APPROVAL
                        )
                ));

        assertThat(resolver.resolve(template)).isEqualTo(ApprovalSubState.PENDING_DECISION);
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
            TemplateLifecycleStatus to
    ) {
        return new TemplateLifecycleRecordEntity(
                UUID.randomUUID(),
                templateId,
                action,
                from,
                to,
                null,
                null,
                null,
                "10000003"
        );
    }
}
